import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketPostman {
    static private Socket client;
    private final short[] inArray;
    private short[] outArray;

    private boolean connectStatus = false;

    private final char startSymbol = '$';
    private final char finishSymbol = ';';
    private final char separatorSymbol = ' ';

    private final byte[] globalBuffer;
    private int indexGlobalBuffer = 0;
    private boolean startReadFlag = false;
    private int realByte = 0;

    public SocketPostman(String ipAddress, int port, short[] inArray, short[] outArray) throws IOException {
        this.inArray = inArray;
        this.outArray = outArray;
        globalBuffer = new byte[inArray.length * 10]; // динамический рост буффера относительно длины входного пакета
        System.out.println("Подключение к " + ipAddress + " на порт " + port);
        client = new Socket(ipAddress, port);
        connectStatus = true;
        clientTask();

    }

    public final boolean isConnected() {
        return connectStatus;
    }


    public void clientTask() throws IOException {
        byte[] buffer = new byte[globalBuffer.length];
        DataInputStream in = new DataInputStream(client.getInputStream());

        Thread thread = new Thread(() -> {
            while (connectStatus) {
                try {
                    Thread.sleep(20);
                    if (in.available() > 0) {
                        int numByte = in.read(buffer);
                        parseBuffer(buffer, numByte);
                    }
                    client.getOutputStream().write(0); // проверка на подключение к серверу
                } catch (Exception e) {
                    e.printStackTrace();
                    connectStatus = false;
                }
            }
        });

        thread.start();
    }

    public Socket getClient() {
        return client;
    }

    private void parseBuffer(byte[] buffer, int numByte) throws IOException {

        for (int i = 0; i < numByte; i++) {
            if (buffer[i] == startSymbol) {
                indexGlobalBuffer = 0;
                startReadFlag = true;
                realByte = 0;
                continue;
            } else if (buffer[i] == finishSymbol && startReadFlag) {
                //обновляем глобальное состояние
                inArrayUpload(globalBuffer, realByte);
                realByte = 0;
                startReadFlag = false;
                indexGlobalBuffer = 0;
                return;
            }
            if (startReadFlag) {
                if (indexGlobalBuffer == globalBuffer.length) {
                    realByte = 0;
                    startReadFlag = false;
                    indexGlobalBuffer = 0;
                    return;
                }
                globalBuffer[indexGlobalBuffer++] = buffer[i];
                realByte++;
            }
        }

    }


    private void inArrayUpload(byte[] buffer, int realByte) {
        short[] bufferArray = new short[inArray.length];

        for (int i = 0, acc = 0, factor = 0, indexOfBufferArray = 0; i < realByte + 1; i++) {
            if (i == realByte) {
                bufferArray[indexOfBufferArray] = (short) acc;
                break;
            }

            if (buffer[i] == separatorSymbol) {
                bufferArray[indexOfBufferArray] = (short) acc;
                indexOfBufferArray++;
                if (indexOfBufferArray == (bufferArray.length)) {
                    // пришедший пакет больше ожидаемого
                    return;
                }
                acc = 0;
                factor = 0;
            } else if ((buffer[i] - 48) >= 0 && (buffer[i] - 48) <= 9) {
                acc = ((acc * factor) + (buffer[i] - 48));
                factor = 10;
            } else {
                // была ошибка валидности пакета
                return;
            }

        }

        // начало проверки контрольной суммы
        short crc = 0;

        for (int n = 0; n < bufferArray.length - 1; n++) {
            crc += bufferArray[n];
        }

        if (bufferArray[bufferArray.length - 1] == crc) {
            //все ок

            for (int i = 0; i < inArray.length; i++) {
                inArray[i] = bufferArray[i];
            }
            return;
        } else {
            // была ошибка crc
            return;
        }
    }


    public static void main(String[] args) throws IOException, InterruptedException {

        short[] inArray = new short[15];
        short[] outArray = new short[3];
        SocketPostman socketPostman = new SocketPostman(args[0], Integer.parseInt(args[1]), inArray, outArray);
        while (socketPostman.isConnected()) {
            Thread.sleep(400);
            System.out.println(Arrays.toString(inArray));
            //System.out.println(socketPostman.getClient());
        }
        System.out.println("произошел разрыв соединения!");
    }
}
