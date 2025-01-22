
/* This is a Java Chat Room Project Assignment
for class course CSC1004 Computational Laboratory Using Java

This program allows user to open multiple command line interfaces
and chat between interfaces. Some features in this program includes
the local time when a message was sent, number of message(s)
sent by each user (interface), chat history and search chat history by keywords.
*/

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient{
    private Socket chatSocket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    private int numMessage;

    // Constructor for each user (command-line interface)
    // Each user will have :
    // - chat socket (the point of a user, to allow connection with the server)
    // - buffered reader and writer for input and output by sending the message to/ from the server
    // - a username (initialized before connecting to the chat server)
    // ** Exceptions/ Conditions (for username):
    // ** A username cannot be empty.
    // ** If two user has the same username, the new user/ last one to log in
    // will be reminded that they is a user using the same name
    // ** In the case of a same username, the two users will have the same data / memory,
    // and will not be able to see the message sent by other user (command line interface)
    // - number of message sent (starts at 0 and increase when the user sent a message)
    public ChatClient(Socket chatSocket, String username){
        try {
            this.chatSocket = chatSocket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(chatSocket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));
            this.username = username;
            this.numMessage = 0;
        }catch(IOException exception){
            closeEverything(chatSocket,bufferedReader,bufferedWriter);
        }
    }

    // The following function send message that was typed by the user to the client handler
    // There are 2 types of messages :
    // 1) a typical message (a message sent by one user to other user within the same server)
    // ** the message will be concatenated with the username and number of message has been sent
    // by the user and sent to the client handler to be broadcasted to other users' command line interface
    // 2) a search message to search of previous message(s) with the certain keyword
    // ** the format of search message : #search (keyword)
    public  void  sendMessage(){
        try{
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            Scanner scanner = new Scanner(System.in);

            while(chatSocket.isConnected()){
                String messageSend = scanner.nextLine();

                // ** Exception: A message cannot be empty
                if ((!messageSend.isEmpty())) {
                    if (messageSend.startsWith("#search")) {
                        bufferedWriter.write(messageSend);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                    } else {
                        numMessage++;
                        bufferedWriter.write(username + " : " + messageSend + " [" + numMessage + "] message(s)");
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                    }
                }
            }
        }catch(IOException exception){
            closeEverything (chatSocket,bufferedReader,bufferedWriter);
        }
    }

    // The following function receives the message from the client handler
    // and prints them to the user's command line interface
    public void listenForMessage(){
        new Thread(() -> {
            String groupChatMessage;
            while (chatSocket.isConnected()){
                try{
                    groupChatMessage = bufferedReader.readLine();
                    System.out.println(groupChatMessage);

                }catch(IOException exception){
                    closeEverything(chatSocket,bufferedReader,bufferedWriter);
                }
            }
        }).start();
    }

    // The following function make sure that all components in a user
    // e.g. buffered reader, buffered writer and chat socket are close
    // so that when a user log out or exit of the command line interface,
    // all of the memory of the user gets deleted (except of the chat history)
    public void closeEverything(Socket chatSocket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        try{
            if(bufferedReader != null){
                bufferedReader.close();
            }
            if(bufferedWriter != null){
                bufferedWriter.close();
            }
            if(chatSocket != null){
                chatSocket.close();
            }
        }catch(IOException exception){
            exception.printStackTrace();
        }
    }

    public  static void main(String[] args)throws IOException{
        // When the user first open the command line interface,
        // they are required to input a username
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your username:");
        String username = scanner.nextLine();

        //** Exception handle when username is empty
        //** The user would be asked to input their username until it is not empty
        while (username.isEmpty()){
            System.out.println("Username cannot be empty! Enter your username:");
            username = scanner.nextLine();
        }

        //The connection to the server is established, and the user can chat with other users in the group chat
        Socket chatSocket = new Socket("localhost", 21);
        ChatClient client = new ChatClient(chatSocket, username);
        client.listenForMessage();
        System.out.println("This chat will be recorded in ChatHistory.txt");
        System.out.println("To search for message, type #search(keywords)");
        System.out.println();
        client.sendMessage();
    }
}