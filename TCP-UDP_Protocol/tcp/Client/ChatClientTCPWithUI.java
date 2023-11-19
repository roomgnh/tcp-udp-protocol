import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatClientTCPWithUI {

    private JFrame frame;
    private JTextField nameField;
    private JTextArea chatArea;
    private JTextField messageField;
    private JComboBox<String> clientList;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public ChatClientTCPWithUI() {
        frame = new JFrame("TCP Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BorderLayout());

        nameField = new JTextField();
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectToServer();
            }
        });

        namePanel.add(nameField, BorderLayout.CENTER);
        namePanel.add(connectButton, BorderLayout.EAST);

        frame.add(namePanel, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());

        messageField = new JTextField();
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        JButton fileButton = new JButton("Browse");
        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseFile();
            }
        });

        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);
        messagePanel.add(fileButton, BorderLayout.WEST);

        frame.add(messagePanel, BorderLayout.SOUTH);

        clientList = new JComboBox<>();
        clientList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateReceiverField();
            }
        });

        frame.add(clientList, BorderLayout.WEST);

        frame.setVisible(true);
    }

    private void connectToServer() {
        try {
            String name = nameField.getText();
            String serverIP = JOptionPane.showInputDialog("Enter the server IP:");
            socket = new Socket(serverIP, 12345);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            // Send client name to the server
            writer.println(name);

            // Receive messages from the server and display them in the chat area
            Thread receiveThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = reader.readLine()) != null) {
                        if (message.startsWith("SEND_FILE")) {
                            receiveFile(message);
                        } else if (message.startsWith("ONLINE_USERS")) {
                            updateClientList(message);
                        } else {
                            chatArea.append(message + "\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            receiveThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        try {
            String message = messageField.getText();
            writer.println(message);
            messageField.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void chooseFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            sendFile(selectedFile);
        }
    }

    private void sendFile(File file) {
        try {
            String receiver = (String) clientList.getSelectedItem();
            if (receiver != null) {
                FileInputStream fileInputStream = new FileInputStream(file);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                OutputStream outputStream = socket.getOutputStream();

                writer.println("SEND_FILE " + receiver + " " + file.getName());

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                bufferedInputStream.close();
                fileInputStream.close();
                outputStream.close();

                chatArea.append("File sent to " + receiver + ": " + file.getName() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveFile(String message) {
        String[] parts = message.split(" ", 3);
        if (parts.length == 3) {
            String sender = parts[1];
            String fileName = parts[2];

            try {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new File(fileName));
                int result = fileChooser.showSaveDialog(frame);

                if (result == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fileChooser.getSelectedFile();
                    FileOutputStream fileOutputStream = new FileOutputStream(fileToSave);
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                    InputStream inputStream = socket.getInputStream();

                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        bufferedOutputStream.write(buffer, 0, bytesRead);
                    }

                    bufferedOutputStream.close();
                    fileOutputStream.close();
                    inputStream.close();

                    chatArea.append("File received from " + sender + ": " + fileName + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateClientList(String message) {
        String[] parts = message.split(" ");
        if (parts.length > 1) {
            List<String> onlineUsers = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                onlineUsers.add(parts[i]);
            }
            SwingUtilities.invokeLater(() -> {
                clientList.removeAllItems();
                for (String user : onlineUsers) {
                    clientList.addItem(user);
                }
            });
        }
    }

    private void updateReceiverField() {
        String selectedUser = (String) clientList.getSelectedItem();
        if (selectedUser != null) {
            messageField.setText("/private " + selectedUser + " ");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ChatClientTCPWithUI();
            }
        });
    }
}
