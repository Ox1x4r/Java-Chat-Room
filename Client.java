// READ THE README.md FILE FOR INSTRUCTIONS //
// READ THE README.md FILE FOR INSTRUCTIONS //
// READ THE README.md FILE FOR INSTRUCTIONS //

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client implements Runnable {

    // Instance variables
    private Socket client; // A socket for connection to the server
    private BufferedReader in; // A reader for receiving data from the server
    private PrintWriter out; // A writer for sending data to the server
    private boolean done; // A flag that inidicates is the client should be terminated or not

    @Override
    public void run() {
        try {
            // Connects client to the server, also creates a write and a reader to send and receive data
            client = new Socket("127.0.0.1", 9999);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            // Creates a seperate thread for handling user input
            InputHandler inHandler = new InputHandler();
            Thread t = new Thread(inHandler);
            t.start();

            // Reads data from server and prints it to the console
            String inMessage;
            while ((inMessage = in.readLine()) != null) {
                System.out.println(inMessage);
            }

        } catch (IOException e) {
            // If an exception occurs, shut client down
            shutdown();
        }
    }

    public void shutdown() {
        done = true;
        try {
            in.close();
            out.close();
            if (!client.isClosed())
                ;
            {
                client.close();
            }
        } catch (IOException e) {
            // Ignore any exceptions that occur whilst attempting to close the socket
        }
    }

    class InputHandler implements Runnable {
        @Override
        public void run() {
            try {
                // Creates a reader for reading user input
                BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                // Reads user input input and sends it to the server
                while (!done) {
                    String message = inReader.readLine();
                    // If the user enters /quit, quits the user from the server and outputs a message to the server
                    if (message.equals("/quit")) {
                        out.println(message);
                        inReader.close();
                        shutdown();
                    } else {
                        out.println(message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }
    }

    public static void main(String[] args) {
        // Creates a new instance of a client and starts the client
        Client client = new Client();
        client.run();
    }
}
