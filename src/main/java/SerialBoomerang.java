
import com.fazecast.jSerialComm.SerialPort;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private final static Set<SerialPort> openPortList = new HashSet<>();
    private short[] inArray;
    private short[] outArray;
    private final SerialPort serialPort;
    private String openPortName;
    private int openPortBaudRate;

    private SerialBoomerang(String openPortName, int baudRate, short[] inArray, short[] outArray) {
        this.openPortName = openPortName;
        serialPort = SerialPort.getCommPort(openPortName);
        serialPort.openPort();
        serialPort.setBaudRate(baudRate);
        openPortBaudRate = baudRate;
        this.inArray = inArray;
        this.outArray = outArray;
        openPortList.add(serialPort);
        System.out.println("Open serial port: " + openPortName);
    }

    public static SerialBoomerang getConnectSerialPort(String portName, int baudRate, short[] inArray, short[] outArray) {
        if (portName == null) {
            return null;
        }
        portName = portName.toUpperCase();

        if (Arrays.binarySearch(getArrayAllSerialPortsName(), portName) >= 0) {
            return new SerialBoomerang(portName, baudRate, inArray, outArray);
        }
        return null;
    }

    public static int[] getBaudRateList() {
        return BAUD_RATE_LIST;
    }


    public static String[] getArrayAllSerialPortsName() {
        String[] resultList = Arrays.stream(SerialPort.getCommPorts()).map(SerialPort::getSystemPortName).toArray(String[]::new);
        Arrays.sort(resultList);
        return resultList;
    }


    public final void close() {
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
        return "Port is " + (serialPort.isOpen() ? getOpenPortName() + "open, baud rate: " + getOpenPortBaudRate() : "clos.");
    }

    private void slaveTask() throws IOException {

        byte startSymbol = (byte) '$';
        byte finishSymbol = (byte) ';';

        byte[] globalBuffer = new byte[inArray.length * 5];
        int indexGlobalBuffer = 0;
        boolean startReadFlag = false;
        int realByte = 0;


        try {
            while (true) {
                Thread.sleep(20);
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
                            indexGlobalBuffer = 0;
                            startReadFlag = false;
                            //обновляем глобальное состояние
                            inArray = inArrayUpload(globalBuffer, realByte);
                            System.out.println(Arrays.toString(inArray));
                            realByte = 0;
                        }

                        if (startReadFlag) {
                            globalBuffer[indexGlobalBuffer++] = readBuffer[i];
                            realByte++;

                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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



    public static void main(String[] args) throws IOException, InterruptedException {

        short[] inArray = new short[5];
        short[] outArray = new short[3];
        Scanner scanner = new Scanner(System.in);
        SerialBoomerang connect = null;

        while (connect == null) {

            String[] serialPortList = getArrayAllSerialPortsName();

            System.out.println(Arrays.toString(serialPortList));

            while (!scanner.hasNextLine())
                Thread.sleep(100);

            String scannerResult = scanner.nextLine().toUpperCase();

            connect = getConnectSerialPort(scannerResult, getBaudRateList()[0], inArray, outArray);

        }

        connect.slaveTask();
        connect.close();
    }
}


