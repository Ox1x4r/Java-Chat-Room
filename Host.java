// READ THE README.md FILE FOR INSTRUCTIONS //
// READ THE README.md FILE FOR INSTRUCTIONS //
// READ THE README.md FILE FOR INSTRUCTIONS //

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Defines Host class
public class Host implements Runnable {

    // Define instance variables
    private ArrayList<ConnectionHandler> connections; // ArrayList that holds the connection handlers for each client.
    private ServerSocket server; // ServerSocket to listen for incoming connections
    private boolean done; // Boolean flag to indicate if the server is required to shutdown
    private ExecutorService pool; // Thread pool to manage multiple client connections
    private ConnectionHandler currentAdmin = null; // Initialises null variable to currentAdmin

    // Host constructor
    public Host() {
        connections = new ArrayList<>();
        done = false;
        currentAdmin = null;
    }

    // Host run method - looks for incoming connections and starts new
    // ConnectionHandler threads for each client
    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool(); // Initialize thread pool
            while (!done) {
                Socket client = server.accept(); // Wait for incoming connections
                ConnectionHandler handler = new ConnectionHandler(client, connections); // Creates a new ConnectionHandler for the client

                if (connections.isEmpty()) { // Checks if the list of connections is empty
                    handler.isFirstUser = true; // If its the first connection to the server, sets the users role to 'isFirstUser'
                }

                connections.add(handler); // Adds the ConnectionHandler to the ArrayList
                pool.execute(handler); // Starts new thread for the ConnectionHandler
            }
        } catch (Exception e) {
            shutdown(); // If an exception occurs, shutdown the server
        }
    }

    // Method to broadcast a message to all connected clients
    public void broadcast(String message) {
        String timestamp = "[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")) + "] "; // Gets current time as a string
        for (ConnectionHandler ch : connections) { // Loop through all ConnectionHandlers in ArrayList
            if (ch != null) {
                ch.sendMessage(timestamp + message); // If ConnecionHandler != null , send message to client with timestamp
            }
        }
    }

    // Method to shutdown the server
    public void shutdown() {
        try {
            done = true; // Sets done flag to true
            pool.shutdown(); // Shuts down the thread pool
            if (!server.isClosed()) {
                server.close(); // If server is not already closed, close the server socket
            }
            for (ConnectionHandler ch : connections) { // Loop through all ConnectionHandlers in ArrayList
                ch.shutdown(); // Shutdown each ConnectionHandler
            }
        } catch (IOException e) {
            // Ignore any exceptions that occur
        }
    }

    // Define ConnectionHandler class
    class ConnectionHandler implements Runnable {

        // Define instance variables
        private Socket client; // Socket for client connection
        private BufferedReader in; // BufferedReader to read input from client
        private PrintWriter out; // PrintWriter to send output to client
        private String ID; // String to store clients ID
        private ArrayList<ConnectionHandler> connections; // ArrayList of ConnectionHolders, holds all active users
        private boolean isFirstUser = false; // Flag to indicate if it is the first user to connect
        private boolean isAdmin = false; // Flag to indicate if the user is an admin

        // ConnectionHandler constructor
        public ConnectionHandler(Socket client, ArrayList<ConnectionHandler> connections) {
            this.client = client;
            this.connections = connections;
        }

        // ConnectionHandler run method - reads input from client and sends message to all the clients that are connected
        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true); // Initializes PrintWriter to send output to the client
                                                                       
                in = new BufferedReader(new InputStreamReader(client.getInputStream())); // Initializes BufferedReader to read input from the client

                out.println("Please Enter Your ID: "); // Asks the client to enter their ID
                ID = in.readLine(); // Reads the clients ID

                // Check to see if this is the first user connecting
                if (isFirstUser) {
                    out.println("Welcome, you are the first user. You have been given administrator privileges.");
                    ID = "Admin " + ID; // Adds the "Admin" prefix to the users ID
                    isAdmin = true; // Sets the user as admin
                    currentAdmin = this; // Sets the user as current admin
                }

                System.out.println(ID + " Connected!"); // Prints confirmation of the clients ID and that they are now connceted
                broadcast(ID + " Joined the chat!"); // Sends a message to all clients that are connected that a new user has joined

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/changeID")) { // Command for the user to change ID
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) { // Check to see if new ID is provided in correct format
                            broadcast(ID + " renamed themselves to " + messageSplit[1]); // Sends a message to all connected clients that a client has changed their ID

                            System.out.println(ID + " renamed themselves to " + messageSplit[1]); // Prints the clients changed ID to the console

                            ID = messageSplit[1];
                            out.println("Successfully changed ID to " + ID); // Sends a message to the client that their ID change was successful

                        } else {
                            out.println("No ID Provided!"); // If ID is invalid / No ID was provided sends an error message to the client

                        }
                    
                    // Admin Kick command

                    } else if (message.startsWith("/kick")) {
                        if (!isAdmin) { // Checks if user is admin
                            out.println("You do not have admin permission to use this command.");
                        } else {
                            String[] messageSplit = message.split(" ", 2); // Splits the command in two
                            if (messageSplit.length != 2) { // Check to ensure format is correct
                                out.println("Usage: /kick <user ID>");
                            } else {
                                String userID = messageSplit[1]; // Takes the user ID from command
                                boolean userFound = false;
                                for (ConnectionHandler ch : connections) { // Iterates over all connected users
                                    if (ch.ID.equals(userID)) { // Check to see if the current client is the one to be kicked
                                        userFound = true;
                                        ch.sendMessage("You have been kicked from the chat!"); // Sends a message to the user
                                        ch.shutdown(); // Disconnects the user from server
                                        connections.remove(ch); // Removes kicked user from list of connected users
                                        broadcast(userID + " has been kicked from the chat by " + ID); // Broadcasts to everyone that user has been kicked
                                        System.out.println(userID + " has been kicked from the chat by " + ID); // Logs the user being kicked
                                    }
                                }
                            }
                        }
                    
                        // Private message command
                    
                    } else if (message.startsWith("/pm")) {
                        String[] messageSplit = message.split(" ", 2); // Splits the message into two parts, the recipient and the message

                        if (messageSplit.length == 2) { // Check to see if == 2
                            String[] privateMessageSplit = messageSplit[1].split(" ", 2); // Splits the private message into two parts, recipient and message

                            if (privateMessageSplit.length == 2) { // Check to see if == 2
                                // Gets the recipient and message from the split
                                String recipient = privateMessageSplit[0];
                                String pmMessage = privateMessageSplit[1];
                                boolean recipientFound = false;
                                // Iterates through connected clients to find the recipient
                                for (Host.ConnectionHandler ch : connections) {
                                    if (ch.ID.equals(recipient)) { // If recipient is found, the private message will send and then end the loop

                                        ch.sendMessage(ID + " (private message): " + pmMessage);
                                        out.println("Sent private message to " + recipient + ": " + pmMessage);
                                        recipientFound = true;
                                        break;
                                    }
                                }
                                if (!recipientFound) { // If recipient is not found, an error message will be printed
                                    out.println(recipient + " not found");
                                }
                            } else {
                                out.println("Invalid private message format"); // Error message for invalid format
                            }
                        }
                    }
                    // Show user info using "/info"
                    else if (message.startsWith("/info")) {
                        out.println(ID + client);
                    }

                    else if (message.startsWith("/quit")) { // Commands to quit the chat
                        broadcast(ID + " left the chat!"); // Outputs a message to all connected clients that a user has quit the chat

                        shutdown(); // Closes the connection for the client that quit
                    } else {
                        broadcast(ID + ": " + message); // Outputs a message to all the clients
                    }

                }
            } catch (IOException e) {
                // Client disconnected, remove connection handler from list and broadcast message
                connections.remove(this); // Removes connection handler from list of connections
                broadcast(ID + " Left the chat!"); // Broadcasts to other users that user has left
                if (isAdmin && currentAdmin == this) { // If the current user has admin and left
                    currentAdmin = null; // Sets current admin to null
                    assignNewAdmin(); // Assigns a new admin
                }
                shutdown();
            }
        }

        public void sendMessage(String message) { // Method to send messages to clients
            out.println(message); // Outputs message to the client
        }

        public void shutdown() { // Method to close connection
            try {
                in.close(); // Closes the BufferedReader used to read input from the client
                out.close(); // Closes the PrintWriter used to send output from the client
                if (!client.isClosed()) { // Check to see if clients socket is still open
                    client.close(); // Closes the clients connection
                }
            } catch (IOException e) {
            }
        }
    }

    // Method to assign administrator privileges to a new user if current admin disconnects
    private void assignNewAdmin() {
        for (ConnectionHandler ch : connections) { // Loops through all connection handlers to find new admin
            if (ch != null && !ch.isAdmin) { // Checks to see if connection handler exists and is not already admin
                // Assign admin privileges to the connection handler
                ch.isAdmin = true;
                ch.ID = "Admin " + ch.ID;
                currentAdmin = ch;
                ch.out.println("You have been assigned administrator privileges!");
                broadcast(ch.ID + " has been assigned administrator privileges!");
                break; // Stops searching for new admin once admin is found
            }
        }
    }


    public static void main(String[] args) {
        Host server = new Host(); // Creates a new instance of the Host Class
        server.run(); // Starts the server
    }
}