import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketBoomerang {
    public static void main(String[] args) {
        String serverName = "192.168.1.1";
        int port = 80;
        try {
            System.out.println("Подключение к " + serverName + " на порт " + port);
            Socket client = new Socket(serverName, port);
            System.out.println("Просто подключается к " + client.getRemoteSocketAddress());

            OutputStream outToServer = client.getOutputStream();
            DataOutputStream out = new DataOutputStream(outToServer);
            out.writeUTF("Привет из " + client.getLocalSocketAddress());

            InputStream inFromServer = client.getInputStream();
            DataInputStream in = new DataInputStream(inFromServer);
            //System.out.println("Сервер ответил " + in.readUTF());
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
