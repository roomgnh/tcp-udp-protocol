import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPClient implements Runnable {
    public final static int SERVER_PORT = 7778;
    static DatagramSocket udpClientSocket = null;
    static InetAddress serverIPAddress;
    String name;

    public void setName(String nm) {
        this.name = nm;
    }

    public String getName() {
        return name;
    }

    UDPClient() {
        try {
            udpClientSocket = new DatagramSocket();
        } catch (IOException er) {
            System.out.println(er);
        }

        Thread t = new Thread(this);
        t.start();
    }

    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        try {
            System.out.print("Enter your name: ");
            String name = reader.readLine();
            setName(name);

            serverIPAddress = InetAddress.getByName("localhost");

            // Log on to the server
            byte[] sendData = ("HAI " + name).getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverIPAddress, SERVER_PORT);
            udpClientSocket.send(sendPacket);

            // Thread untuk menerima pesan dari server dan menampilkannya di konsol
            new Thread(() -> {
                while (true) {
                    byte[] receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                    try {
                        udpClientSocket.receive(receivePacket);
                        String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        System.out.println(receivedMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            while (true) {
                // Send a message to the server
                System.out.print("Enter your message (or type 'exit' to quit): ");
                String message = reader.readLine();

                if ("exit".equalsIgnoreCase(message)) {
                    break;
                }

                sendData = (name + ": " + message).getBytes();
                sendPacket = new DatagramPacket(sendData, sendData.length, serverIPAddress, SERVER_PORT);
                udpClientSocket.send(sendPacket);
            }

            udpClientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new UDPClient();
    }
}