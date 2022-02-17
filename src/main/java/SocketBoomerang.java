import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class SocketBoomerang {
    static private Socket client;
    private short[] inArray = new short[15];

    private final char startSymbol = '$';
    private final char finishSymbol = ';';
    private final char separatorSymbol = ' ';

    private final byte[] globalBuffer = new byte[1024];
    private int indexGlobalBuffer = 0;
    private boolean startReadFlag = false;
    private int realByte = 0;


    private boolean parseBuffer(byte[] buffer, int numByte) throws IOException {

            for (int i = 0; i < numByte; i++) {
                if (buffer[i] == startSymbol) {
                    indexGlobalBuffer = 0;
                    startReadFlag = true;
                    realByte = 0;
                    continue;
                } else if (buffer[i] == finishSymbol) {
                    //обновляем глобальное состояние
                    inArray = inArrayUpload(globalBuffer, realByte);
                    System.out.println(Arrays.toString(inArray));
                    realByte = 0;
                    startReadFlag = false;
                    indexGlobalBuffer = 0;
                    return true;

                }

                if (startReadFlag) {
                    if(indexGlobalBuffer == globalBuffer.length){
                        realByte = 0;
                        startReadFlag = false;
                        indexGlobalBuffer = 0;
                        return false;
                    }
                    globalBuffer[indexGlobalBuffer++] = buffer[i];
                    realByte++;
                }
            }

        return false;
    }

    private short[] inArrayUpload(byte[] buffer, int realByte) {
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
                    return inArray;
                }
                acc = 0;
                factor = 0;
            } else if ((buffer[i] - 48) >= 0 && (buffer[i] - 48) <= 9) {
                acc = ((acc * factor) + (buffer[i] - 48));
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


    public static void main(String[] args) throws IOException {
        String serverName = "192.168.2.100";
        int port = 1804;
        byte[] buffer = new byte[1024];
        int iter = 0;
        SocketBoomerang socketBoomerang = new SocketBoomerang();
        try {
            System.out.println("Подключение к " + serverName + " на порт " + port);
            Socket client = new Socket(serverName, port);

            //OutputStream outToServer = client.getOutputStream();
            //DataOutputStream out = new DataOutputStream(outToServer);
            //out.writeUTF("Привет из " + client.getLocalSocketAddress());
            DataInputStream in = new DataInputStream(client.getInputStream());

            while (true) {
                Thread.sleep(100);
                if (in.available() > 0) {
                    iter = in.read(buffer);

                    byte[] bufferSecond = new byte[iter - 4];

                    for (int i = 0; i < iter - 4; i++) {
                        bufferSecond[i] = buffer[i + 1];
                        //System.out.print((char) bufferSecond[i]);
                    }
                    socketBoomerang.parseBuffer(buffer, iter);


                   // System.out.println(Arrays.toString(socketBoomerang.inArrayUpload(bufferSecond, bufferSecond.length)));
                    /*
                    for(int i = 0; i < iter; i++){
                        System.out.print((char) buffer[i]);
                    }
                    */

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
