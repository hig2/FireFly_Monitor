
import com.fazecast.jSerialComm.SerialPort;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;


/*
* getArrayAllSerialPortsName() - возвращает массив String с именами всех доступных портов
* getBaudRateList() - возвращает массив Int всех доступных бит рейтов
*
*
*
*
*
 */

public class Tested {
    private static final int[] BAUD_RATE_LIST = {9600, 19200, 38400, 57600, 74880, 115200};
    private final SerialPort serialPort;
    private String openPortName;
    private int openPortBaudRate;

    private Tested(String openPortName, int baudRate) {
        this.openPortName = openPortName;
        serialPort = SerialPort.getCommPort(openPortName);
        serialPort.openPort();
        serialPort.setBaudRate(baudRate);
        openPortBaudRate = baudRate;
    }

    public static Tested getConnectSerialPort(String portName, int baudRate){
        if(portName == null){
            return null;
        }

        portName = portName.toUpperCase();

        String[] serialPortList = getArrayAllSerialPortsName();
        if(Arrays.binarySearch(serialPortList, portName) >= 0){
            return new Tested(portName, baudRate);
        }

        return null;
    }

    public static int[] getBaudRateList() {return BAUD_RATE_LIST;}


    public static String[] getArrayAllSerialPortsName(){
        String[] resultList = Arrays.stream(SerialPort.getCommPorts()).map(SerialPort::getSystemPortName).toArray(String[]::new);
        Arrays.sort(resultList);
        return resultList;
    }


    final public void close(){
        serialPort.closePort();
        System.out.println("Close serial port: " +  openPortName);
        openPortName = null;
        openPortBaudRate = 0;
    }

    public final boolean isOpened(){return serialPort.isOpen();}

    public final String getOpenPortName() {return openPortName;}

    public final int getOpenPortBaudRate() {return openPortBaudRate;}

    public String toString(){
        return "Port is " + (serialPort.isOpen() ? getOpenPortName() + "open, baud rate: " + getOpenPortBaudRate() : "clos.") ;
    }

    public final InputStream getInStream(){
        if(serialPort.isOpen()){
            return serialPort.getInputStream();
        }
        return null;
    }




    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String[] serialPortList = getArrayAllSerialPortsName();

        System.out.println(Arrays.toString(serialPortList));


       String scannerResult = scanner.nextLine().toUpperCase();

       Tested tested = new Tested(scannerResult, getBaudRateList()[0]);

       // reader = new BufferedReader(new InputStreamReader(tested), StandardCharsets.UTF_8);



    }
}

/*
* выводим имена всех доступных портов
* ждем ввода в консоль для получения необходимого порта
* открываем порт и настраиваем бит рейд на 19200 бод
* слушаем порт все что пришло передаем в консоль
* если что то пришло в консоль то отправляем по последовательному порту
 */