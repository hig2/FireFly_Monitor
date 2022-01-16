
import com.fazecast.jSerialComm.SerialPort;
import java.io.*;
import java.util.*;


/*
* Класс описывает базовую работу и удобный протокол переачи массивов по последовательному порту
* Массив является типом short (особенность совместимости с системами где int 2 байта и хранит числа от -32 768 до 32 767)
* Имеется контроль crc, содержится в последней ячейке предоваймого массива (max value 32767)
* Максимальное передоваймое значение массива является динамическим и рассчитывается по формуле maxValue = 32767 / lenght array (getMaxValue)
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

public class Tested {
    private static final int[] BAUD_RATE_LIST = {9600, 19200, 38400, 57600, 74880, 115200};
    private final int maxValueArray;
    private final short[] resultArray;
    private final SerialPort serialPort;
    private String openPortName;
    private int openPortBaudRate;

    private Tested(String openPortName, int baudRate, short[] resultArray) {
        this.openPortName = openPortName;
        serialPort = SerialPort.getCommPort(openPortName);
        serialPort.openPort();
        serialPort.setBaudRate(baudRate);
        openPortBaudRate = baudRate;
        this.resultArray = resultArray;
        maxValueArray = 0;
        System.out.println("Open serial port: " +  openPortName);
    }

    public static Tested getConnectSerialPort(String portName, int baudRate, short[] resultArray){
        if(portName == null){
            return null;
        }

        portName = portName.toUpperCase();

        String[] serialPortList = getArrayAllSerialPortsName();
        if(Arrays.binarySearch(serialPortList, portName) >= 0){
            return new Tested(portName, baudRate, resultArray);
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

    public final short[] getResultArray(){return resultArray;}

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

    public final void test() {
        try {
            while (true) {
                Thread.sleep(20);
                while (serialPort.bytesAvailable()  > 0) {
                    byte[] readBuffer = new byte[serialPort.bytesAvailable()];
                    int numRead = serialPort.readBytes(readBuffer, readBuffer.length);
                    parseFrame(readBuffer, numRead);

                    /*
                    char[] resultCharArray = new char[numRead];
                    for(int i = 0; i < resultCharArray.length; i++){
                        resultCharArray[i] = (char) readBuffer[i];
                    }

                    System.out.print(String.valueOf(resultCharArray));
                    //System.out.println("Read " + numRead + " bytes.");
                    //System.out.println(Arrays.toString(readBuffer));
                    */

                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        close();
    }


    private int[] parseFrame(byte[] frame, int quantityOfRealByte){
        byte startSymbol = (byte) '$';
        byte separatorSymbol = (byte) ' ';
        byte finishSymbol = (byte) ';';


        return new int[0];
    }





    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        String[] serialPortList = getArrayAllSerialPortsName();

        System.out.println(Arrays.toString(serialPortList));


       String scannerResult = scanner.nextLine().toUpperCase();

         Tested tested = new Tested(scannerResult, getBaudRateList()[0]);

       tested.test();


      InputStream in = tested.getInStream();

      // String result = IOUtils.toString(in, StandardCharsets.UTF_8);


        /*

        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            System.out.println(result.toString("UTF-8"));
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            tested.close();
        }

         */







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