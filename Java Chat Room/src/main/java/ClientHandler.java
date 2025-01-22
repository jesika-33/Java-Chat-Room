
/* The Client Handler class is in charge of
handling the connection between each user and the server
*/
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ClientHandler implements Runnable {
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>(); // To store all connections that had been established
    private ArrayList<String> messagesRecord; // Message history for each user (Temporary/ Only available when the server is on)
    private Socket chatSocket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;

    public ClientHandler(Socket chatSocket) throws IOException{
        this.chatSocket = chatSocket;
        this.messagesRecord= new ArrayList<>();
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(chatSocket.getOutputStream()));
        this.bufferedReader = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));
        this.clientUsername = bufferedReader.readLine();
        clientHandlers.add(this);
        checkUsername();
        broadcastMessage("Server : " + this.clientUsername + " has entered the chat! ");
    }

    // The following function works to sent message sent by a user
    // and be displayed on the terminal excluding the specific user
    public void broadcastMessage(String messageSend){

        // The format of message :
        // Username : +             }   these three have been concatenated into messageSend
        // message typed by user +  }   before broadcast message was called
        // Number of message +      }
        // Time ( when the message was received by the client handler, with the certain format)
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);
        String formattedMessage = messageSend + " " + formattedDateTime;

        // The formattedMessage are added into temporary history (only kept when server is on)
        // and a txt file (permanent / does not get deleted when server is off and can be accessed before  server is on)
        messagesRecord.add(formattedMessage);
        saveChat(formattedMessage);

        // The formattedMessage are then broadcast into every client/ user's command line interface
        // except for the user who sent it
        for(ClientHandler clientHandler : clientHandlers){
            try{
                if(!clientHandler.clientUsername.equals(clientUsername)){
                    clientHandler.bufferedWriter.write(formattedMessage);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            }catch(IOException exception){
                closeEverything(chatSocket, bufferedReader, bufferedWriter);
            }
        }
    }

    // The following function works to check whether there is already a client with the same username in the current server
    public void checkUsername(){
        int count = 0;
        try{
            for(ClientHandler clientHandler : clientHandlers){
                if(clientHandler.clientUsername.equals(clientUsername)){
                    count++;
                }
                // If there is indeed another user with the same username, the new user is given the following reminder
                if(count >= 2){
                    clientHandler.bufferedWriter.write("Welcome Back, " + clientUsername + "! If this is your first time logging in, please re-enter the chat with a new username ");
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.write("Note : you cannot chat with another user under the same username");
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                    break;
                }
            }
            // Otherwise, there is a welcoming message indicating they have successfully entered the group chat
            if(count == 1){
                bufferedWriter.write("Welcome, " + clientUsername + "!");
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        }catch(IOException exception){
            closeEverything(chatSocket, bufferedReader, bufferedWriter);
        }
    }

    // The following function is to save the chat log by appending the message being sent to ChatHistory.txt
    public void saveChat(String message){
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("ChatHistory.txt",true));
            writer.write(message);
            writer.newLine();
            writer.flush();
        }catch(IOException exception){
            closeEverything(chatSocket, bufferedReader, bufferedWriter);
        }
    }

    // The following function is to search for keywords or username from the chat log throughout the current running server
    public void searchMessage(String messageSearch){
        int count = 0;
        ArrayList<String> messageMatch = new ArrayList<>();
        int endIndex = 0;

        // Searching message that contains the keyword by searching from each user's chat history
        // and combining them into an array (unordered)
        for(ClientHandler clientHandler : clientHandlers){
            for(String message : clientHandler.messagesRecord){

                // ** taken into consideration the format of the message
                if (message.contains("[")){
                    endIndex= message.indexOf("[");
                } else if (message.contains("!")) {
                    endIndex = message.indexOf("!");
                }
                if(message.substring(0,endIndex).contains(messageSearch)){
                    messageMatch.add(message);
                    count++;
                }
            }
        }

        // Sending back the message that contains the keyword back to the client
        // If there is no match found, it will display "Message(s) not found!"
        for(ClientHandler clientHandler : clientHandlers){
            if(clientHandler.clientUsername.equals(clientUsername)){
                try{
                    if(count == 0){
                        clientHandler.bufferedWriter.write("Message(s) not found!");
                        clientHandler.bufferedWriter.newLine();
                        clientHandler.bufferedWriter.flush();
                    }
                    else{
                        for(String match : messageMatch){
                            clientHandler.bufferedWriter.write(match);
                            clientHandler.bufferedWriter.newLine();
                            clientHandler.bufferedWriter.flush();
                        }
                    }
                }catch (IOException exception){
                        closeEverything(chatSocket, bufferedReader, bufferedWriter);
                }
            }
        }
    }

    // Removing the client from the array of connections (client handlers)
    // and by making an announcement to other users that the certain user has left
    public void removeClient(){
        clientHandlers.remove(this);
        broadcastMessage("Server : "+clientUsername+" has left the chat");
    }

    // Closing  every component of the ClientHandler
    public  void  closeEverything(Socket chatSocket, BufferedReader bufferedReader, BufferedWriter bufferedWriter){
        removeClient();
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
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void run(){
        String messageFromClient;
        try {
            // While the connection exists, listen for message typed by client
            // and separate the actions taken by message type
            while(chatSocket.isConnected()){
                messageFromClient = bufferedReader.readLine();
                if(messageFromClient.startsWith("#search")){
                    searchMessage(messageFromClient.substring(messageFromClient.indexOf("(")+1,messageFromClient.indexOf(")")));
                }
                else{
                    broadcastMessage(messageFromClient);
                }
            }
        }catch(IOException exception){
            closeEverything(chatSocket, bufferedReader, bufferedWriter);
        }
    }
}