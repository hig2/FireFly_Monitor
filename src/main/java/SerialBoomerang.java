
import com.fazecast.jSerialComm.SerialPort;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;


//qweqweqweeeeeeeee$1 2 3 4 10;qweqwerqwerqwerqwerqwerqwerqwerqwerqwertqtwetetwtewqqqqqqqqqteqwtwe

/*
 * Класс описывает базовую работу и удобный протокол переачи массивов по последовательному порту
 * Массив является типом short (особенность совместимости с системами где int 2 байта и хранит числа от -32 768 до 32 767)
 * Имеется контроль crc, содержится в последней ячейке предоваймого массива (max value 32767)
 * Максимальное передоваймое значение массива является динамическим и рассчитывается по формуле maxValue = 32767 / len array (getMaxValue)
 *
 *
 * Доступные методы:
 * getArrayAllSerialPortsName() - возвращает массив String с именами всех доступных портов
 * getBaudRateList() - возвращает массив Int всех доступных бит рейтов
 *
 *
 *
 *
 * ! необходимо реализовать проверку на наличие битрейта в битрейд листе
 */

public class SerialBoomerang {
    private final static int[] BAUD_RATE_LIST = {9600, 19200, 38400, 57600, 74880, 115200};
    private short[] inArray;
    private short[] outArray;
    private final SerialPort serialPort;
    private String openPortName;
    private int openPortBaudRate;
    private boolean threadStop = false;

    private SerialBoomerang(String openPortName, int baudRate, short[] inArray, short[] outArray) {
        this.openPortName = openPortName;
        serialPort = SerialPort.getCommPort(openPortName);
        serialPort.openPort();
        serialPort.setBaudRate(baudRate);
        openPortBaudRate = baudRate;
        this.inArray = inArray;
        this.outArray = outArray;
        System.out.println("Open serial port: " + openPortName);
    }

    public static SerialBoomerang getConnectSerialPort(String portName, int baudRate, short[] inArray, short[] outArray, boolean isMaster, int delay) throws IOException, InterruptedException {
        if (portName == null) {
            return null;
        }
        portName = portName.toUpperCase();

        if (Arrays.binarySearch(getArrayAllConnectedPortsList(), portName) >= 0) {
            System.out.println("Error: " + portName + " is busy.");
            return null;
        }



        if (Arrays.binarySearch(getArrayAllSerialPortsName(), portName) >= 0) {
            SerialBoomerang serialBoomerang = new SerialBoomerang(portName, baudRate, inArray, outArray);

            if(isMaster){
                serialBoomerang.masterRun(delay);
            }else{
                serialBoomerang.slaveRun();
            }
            return serialBoomerang;
        }
        return null;
    }


    public static int[] getBaudRateList() {
        return BAUD_RATE_LIST;
    }

    public static String[] getArrayAllConnectedPortsList() {
        return Arrays.stream(SerialPort.getCommPorts()).filter(e -> e.getDescriptivePortName().equals("User-Specified Port")).map(SerialPort::getSystemPortName).distinct().sorted().toArray(String[]::new);
    }


    public static String[] getArrayAllSerialPortsName() {
        return Arrays.stream(SerialPort.getCommPorts()).map(SerialPort::getSystemPortName).distinct().sorted().toArray(String[]::new);
    }


    public final void close() {
        threadStop = true;
        serialPort.closePort();
        System.out.println("Close serial port: " + openPortName);
        openPortName = null;
        openPortBaudRate = 0;
    }

    public final short[] getResultArray() {
        return inArray;
    }

    public final boolean isOpened() {
        return serialPort.isOpen();
    }

    public final String getOpenPortName() {
        return openPortName;
    }

    public final int getOpenPortBaudRate() {
        return openPortBaudRate;
    }

    public String toString() {
        return "Port is " + (serialPort.isOpen() ? getOpenPortName() + " open, baud rate: " + getOpenPortBaudRate() : "clos.");
    }


    private void slaveRun() throws IOException, InterruptedException {
       Thread thread = new Thread(() -> {
            while (!threadStop) {
                try {
                    Thread.sleep(20);
                    if (readSerial()) {
                        System.out.println(Arrays.toString(inArray));
                        // отправка сообщения
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private void masterRun(long delay) throws IOException, InterruptedException {
        AtomicLong t = new AtomicLong(0);
        Thread thread = new Thread(() -> {
            while (!threadStop) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if ((System.currentTimeMillis() - t.get()) > delay) {
                    t.set(System.currentTimeMillis());
                    writeSerial();
                    try {
                        readSerial();
                        System.out.println(Arrays.toString(inArray));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }


    private void writeSerial(){
        char startSymbol =  '$';
        char finishSymbol = ';';
        char separatorSymbol = ' ';
        short crc = 0;
        String result = String.valueOf(startSymbol);

        for(int i = 0; i < (outArray.length - 1); i++){
            crc += outArray[i];
        }

        for(int i = 0; i < outArray.length; i++){
            result = i == (outArray.length - 1) ? result + crc + finishSymbol : result + outArray[i] + separatorSymbol;
        }

            //System.out.println(serialPort.setComPortParameters(9600, 8, 1, 1));

            byte[] bytesMassage = result.getBytes();
            serialPort.writeBytes(bytesMassage, bytesMassage.length);



    }

    private boolean readSerial() throws IOException {

        byte startSymbol = (byte) '$';
        byte finishSymbol = (byte) ';';

        byte[] globalBuffer = new byte[inArray.length * 5];
        int indexGlobalBuffer = 0;
        boolean startReadFlag = false;
        int realByte = 0;


        while (serialPort.bytesAvailable() > 0) {
            byte[] readBuffer = new byte[serialPort.bytesAvailable()];
            int numRead = serialPort.readBytes(readBuffer, readBuffer.length);
            for (int i = 0; i < numRead; i++) {
                if (readBuffer[i] == startSymbol) {
                    indexGlobalBuffer = 0;
                    startReadFlag = true;
                    realByte = 0;
                    continue;
                } else if (readBuffer[i] == finishSymbol) {
                    //обновляем глобальное состояние
                    inArray = inArrayUpload(globalBuffer, realByte);
                    return true;

                }

                if (startReadFlag) {
                    globalBuffer[indexGlobalBuffer++] = readBuffer[i];
                    realByte++;

                }
            }

        }

        return false;
    }


    private short[] inArrayUpload(byte[] newInArray, int realByte) {
        byte separatorSymbol = (byte) ' ';
        short[] bufferArray = new short[inArray.length];

        //realByte + 1 обрабатываем тем самым последнюю итерацию acc
        for (int i = 0, acc = 0, factor = 0, indexOfBufferArray = 0; i < realByte + 1; i++) {
            if (i == realByte) {
                bufferArray[indexOfBufferArray] = (short) acc;
                break;
            }

            if (newInArray[i] == separatorSymbol) {
                bufferArray[indexOfBufferArray] = (short) acc;
                indexOfBufferArray++;
                if (indexOfBufferArray == (bufferArray.length)) {
                    // пришедший пакет больше ожидаемого
                    return inArray;
                }
                acc = 0;
                factor = 0;
            } else if ((newInArray[i] - 48) >= 0 && (newInArray[i] - 48) <= 9) {
                acc = ((acc * factor) + (newInArray[i] - 48));
                factor = 10;
            } else {
                // была ошибка валидности пакета
                return inArray;
            }

        }

        // начало проверки контрольной суммы
        short crc = 0;

        for (int n = 0; n < bufferArray.length - 1; n++) {
            crc += bufferArray[n];
        }

        if (bufferArray[bufferArray.length - 1] == crc) {
            //все ок
            return bufferArray;
        } else {
            // была ошибка crc
            return inArray;
        }
    }

}


