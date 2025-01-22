
/* This is a Java Chat Room Project Assignment
for class course CSC1004 Computational Laboratory Using Java

This program allows user to open multiple command line interfaces
and chat between interfaces. Some features in this program includes
the local time when a message was sent, number of message(s)
sent by each user (interface), chat history and search chat history by keywords.
*/

import java.io.*;
import java.net.*;

public class ChatServer {
    private ServerSocket chatServerSocket;

    // Constructor for the server, which consists of :
    // - the server socket (the point of server, to user to connect to the same server)
    public ChatServer(ServerSocket serverSocket){
        this.chatServerSocket = serverSocket;
    }
    public void startServer() {
        try {

            // While the server is open, new user can connect
            // and chat between users (command line interface)
            while (!chatServerSocket.isClosed()) {
                Socket chatSocket = chatServerSocket.accept();
                System.out.println("A new user has joined!");

                ClientHandler chatClientHandler = new ClientHandler(chatSocket);
                Thread chatThread = new Thread(chatClientHandler);
                chatThread.start();
            }
        } catch(IOException exception){
            closeServer();
        }
    }

    // Closing the server's component which is the ServerSocket
    public void closeServer(){
        try{
            if (chatServerSocket!= null){
                chatServerSocket.close();
            }
        }catch (IOException exception){
            exception.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException{
            ServerSocket chatServerSocket = new ServerSocket(21);
            ChatServer server = new ChatServer(chatServerSocket);
            server.startServer();
        }
}
