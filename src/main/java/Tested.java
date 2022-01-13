
import com.fazecast.jSerialComm.SerialPort;

public class Tested {
    public static void main(String[] args) {
        for(SerialPort element : SerialPort.getCommPorts()){
            System.out.println(element.getSystemPortName());
        }

        SerialPort comPort = SerialPort.getCommPorts()[0];
        comPort.openPort();

    }
}