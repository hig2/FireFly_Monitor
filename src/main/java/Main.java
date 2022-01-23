import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {

        short[] inArray = new short[3];
        short[] outArray = {1, 14, 0, 0, 0, 1, 0, 0, 90, 101, 1, 0};
        Scanner scanner = new Scanner(System.in);
        SerialBoomerang connect;
        String scannerResult;

        do {
            String[] serialPortList = SerialBoomerang.getArrayAllSerialPortsName();
            System.out.println(Arrays.toString(serialPortList));
            while (!scanner.hasNextLine())
                Thread.sleep(100);
            scannerResult = scanner.nextLine().toUpperCase();
        } while ((connect = SerialBoomerang.getConnectSerialPort(scannerResult, SerialBoomerang.getBaudRateList()[0], inArray, outArray, true, 3500)) == null);

        //Thread.sleep(10000);

       // connect.close();





    }
}
