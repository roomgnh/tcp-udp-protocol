import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ChatServerTCP {
    private static Map<String, Socket> clients = new HashMap<>();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(12345);
            System.out.println("TCP Chat Server is running. Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String clientName;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writer = new PrintWriter(clientSocket.getOutputStream(), true);

                // Request client name
                writer.println("Enter your name:");
                clientName = reader.readLine();
                clients.put(clientName, clientSocket);

                System.out.println(clientName + " joined the chat. IP: " + clientSocket.getInetAddress());

                // Inform other clients about the new connection
                broadcastOnlineUsers();

                // Handle incoming messages
                String message;
                while ((message = reader.readLine()) != null) {
                    if (message.startsWith("/private")) {
                        sendPrivateMessage(message);
                    } else if (message.equalsIgnoreCase("exit")) {
                        break;
                    } else if (message.startsWith("SEND_FILE")) {
                        broadcastMessage(clientName, message);
                        sendFile(message);
                    } else {
                        broadcastMessage(clientName, message);
                    }
                }

                // Inform other clients about the disconnection
                broadcastMessage("SERVER", clientName + " has left the chat.");

                // Remove client from the list
                clients.remove(clientName);
                broadcastOnlineUsers();

                reader.close();
                writer.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void broadcastMessage(String sender, String message) {
            for (Map.Entry<String, Socket> entry : clients.entrySet()) {
                if (!entry.getKey().equals(sender)) {
                    try {
                        PrintWriter clientWriter = new PrintWriter(entry.getValue().getOutputStream(), true);
                        clientWriter.println(sender + ": " + message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void sendPrivateMessage(String message) {
            String[] parts = message.split(" ", 3);
            if (parts.length == 3) {
                String receiver = parts[1];
                String privateMessage = parts[2];

                Socket receiverSocket = clients.get(receiver);
                if (receiverSocket != null) {
                    try {
                        PrintWriter privateWriter = new PrintWriter(receiverSocket.getOutputStream(), true);
                        privateWriter.println(clientName + " (private): " + privateMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Error: Receiver " + receiver + " not found.");
                }
            }
        }

        private void sendFile(String message) {
            String[] parts = message.split(" ", 3);
            if (parts.length == 3) {
                String receiver = parts[1];
                String fileName = parts[2];
        
                Socket receiverSocket = clients.get(receiver);
                if (receiverSocket != null) {
                    try {
                        File fileToSend = new File(fileName);
        
                        // Ubah menjadi path absolut jika diperlukan
                        if (!fileToSend.isAbsolute()) {
                            fileToSend = new File(System.getProperty("user.dir"), fileName);
                        }
        
                        FileInputStream fileInputStream = new FileInputStream(fileToSend);
                        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                        OutputStream outputStream = receiverSocket.getOutputStream();
        
                        // Kirim informasi file ke klien penerima
                        PrintWriter receiverWriter = new PrintWriter(receiverSocket.getOutputStream(), true);
                        receiverWriter.println("SEND_FILE_INFO " + clientName + " " + fileToSend.getName());
        
                        byte[] buffer = new byte[4096];
                        int bytesRead;
        
                        while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
        
                        bufferedInputStream.close();
                        fileInputStream.close();
                        outputStream.close();
        
                        System.out.println("File sent to " + receiver + ": " + fileToSend.getName());
                    } catch (FileNotFoundException e) {
                        System.out.println("Error: File not found - " + fileName);
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Error: Receiver " + receiver + " not found.");
                }
            }
        }       

        private void broadcastOnlineUsers() {
            StringBuilder onlineUsers = new StringBuilder("ONLINE_USERS");
            for (String user : clients.keySet()) {
                onlineUsers.append(" ").append(user);
            }

            for (Map.Entry<String, Socket> entry : clients.entrySet()) {
                try {
                    PrintWriter clientWriter = new PrintWriter(entry.getValue().getOutputStream(), true);
                    clientWriter.println(onlineUsers.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
