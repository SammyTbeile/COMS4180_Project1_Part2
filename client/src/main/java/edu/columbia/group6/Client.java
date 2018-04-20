package edu.columbia.group6;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Client {
    // Create Socket
    private Socket clientSock;
    private Scanner scanner;

    /**
     * Constructor to set up socket.
     *
     * @param hostname
     * @param port
     */
    public Client(String hostname, int port) {
        try {
            clientSock = new Socket(hostname, port);
        } catch (IOException e) {
            System.err.println("Unable to open socket to server.");
            System.exit(1);
        }
    }

    /**
     * Main function to start client.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        //Create Scanner
        Scanner scan = new Scanner(System.in);

        //Check for valid args and format
        if (args.length != 2) {
            System.out.println("Usage: client ip-address port");
            System.exit(1);
        }

        //Assign args to variables
        String hostname = args[0];
        String sPort = args[1];
        int port = Integer.parseInt(sPort);


        //Convert hostname to IP and check if Valid
        InetAddress ad = InetAddress.getByName(hostname);
        String ip;
        ip = ad.getHostAddress();
        checkIP(ip);
        checkPort(port);

        Client client = new Client(ip, port);
        client.dispatch();
    }

    /**
     * Dispatch based on command line input.
     */
    private void dispatch() {
        this.scanner = new Scanner(System.in);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            if (line.equals("exit")) {
                System.out.println("Exiting");

                try {
                    this.clientSock.close();
                    System.exit(0);
                } catch (IOException e) {
                    System.err.println("Unable to close socket.");
                }
            } else {
                if (line.startsWith("get ")) {
                    this.get(line);
                } else if (line.startsWith("put ")) {
                    this.put(line);
                } else if (line.equals("ls")) {
                    this.LS();
                }
            }
        }
    }

    // Check to make sure Port is valid
    public static void checkPort(int port) {
        int ports = port;
        try {
            if (ports < 1 || ports > 65535) {
                throw new RuntimeException("Port must be a value between 1 and 65535");
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Port needs to be a number");
        }
    }

    // Check to make sure IP is valid
    public static boolean checkIP(String ips) {
        try {
            if (ips == null || ips.isEmpty()) {
                return false;
            }

            String[] parts = ips.split("\\.");
            if (parts.length != 4) {
                return false;
            }

            for (String s : parts) {
                int i = Integer.parseInt(s);
                if ((i < 0) || (i > 255)) {
                    return false;
                }
            }
            if (ips.endsWith(".")) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    /**
     * Directory listing from server.
     */
    private void LS() {
        try {
            PrintWriter out = new PrintWriter(this.clientSock.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.clientSock.getInputStream()));
            out.println("LS");
            System.out.println("Waiting for response...\n");

            Scanner scanner = new Scanner(reader);

            int newlinePos = 0;

            while (scanner.hasNextLine()) {
                String response = scanner.nextLine();

                // Break when receiving the NUL character.
                if (response.equals(Character.toString(Character.MIN_VALUE))) {
                    break;
                }

                // 250 is SUCCESS. Continue.
                if (response.equals("250")) {
                    newlinePos++;
                } else if (response.equals("550")) {
                    System.out.println("File not found.");
                } else if (response.equals("501")) {
                    System.err.println("An unknown error occurred.");
                } else if (response.startsWith("Request: ")) {
                    // Ignore the request line.
                    newlinePos++;
                } else if (response.length() == 0 && newlinePos == 2) {
                    // Second newline after request and before content.
                    newlinePos++;
                } else if (newlinePos == 3) {
                    // The content.
                    System.out.println(response);
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Send the file to the server.
     */
    private void put(String command) {
        // Get the filename from the command.
        String[] pieces = command.substring(4).split("\\s+");

        if (pieces.length != 1) {
            throw new RuntimeException("Invalid number of put parameters. You must enter a filename");
        }

        String filename = pieces[0];
        File f = new File(filename);

        if (!f.exists()) {
            System.err.println("File Not Found");
            return;
        }

        try {
            // Base 64 encode the file.
            byte[] b64 = Base64.encodeBase64(FileUtils.readFileToByteArray(f));

            PrintWriter out = new PrintWriter(this.clientSock.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.clientSock.getInputStream()));

            // Get the SHA 256 digest of the file.
            String digest = new DigestUtils(SHA_256).digestAsHex(f);

            // Send the command.
            out.println("PUT " + f.getName() + " " + digest);
            out.println();
            out.println(new String(b64));

            Scanner scanner = new Scanner(reader);

            // Parse response from the server.
            while (scanner.hasNextLine()) {
                String response = scanner.nextLine();

                // Break when receiving the NUL character.
                if (response.equals(Character.toString(Character.MIN_VALUE))) {
                    break;
                }

                // Parse the response code.
                switch (response) {
                    case "250":
                        System.out.println("File successfully uploaded and saved.");
                        break;
                    case "501":
                        System.err.println("The server encountered an error when uploading the data.");
                        break;
                    default:
                        break;
                }
            }

        } catch (IOException e) {
            System.err.println("Error sending data to server.");
        }
    }

    /**
     * Retrieve a file (GET) from the FTP server.
     * @param command
     */
    private void get(String command) {
        // Get the filename from the command.
        String[] pieces = command.substring(4).split("\\s+");

        if (pieces.length != 1) {
            throw new RuntimeException("Invalid number of get parameters. You must enter a filename");
        }

        String filename = pieces[0];


        try {
            PrintWriter out = new PrintWriter(this.clientSock.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.clientSock.getInputStream()));

            out.println("GET " + filename);

            Scanner scanner = new Scanner(reader);
            int newlinePos = 0;
            String hash = "";

            while (scanner.hasNextLine()) {
                String response = scanner.nextLine();

                // Break when receiving the NUL character.
                if (response.equals(Character.toString(Character.MIN_VALUE))) {
                    break;
                }

                // 250 is SUCCESS. Continue.
                if (response.equals("250")) {
                    newlinePos++;
                } else if (response.equals("550")) {
                    System.out.println("File not found.");
                } else if (response.equals("501")) {
                    System.err.println("An unknown error occurred.");
                } else if (response.startsWith("Request: ")) {
                    // Ignore the request line.
                    newlinePos++;
                } else if (response.startsWith("Hash: ")) {
                    hash = response.substring(6);
                    newlinePos++;
                } else if (response.length() == 0 && newlinePos == 3) {
                    // Second newline after request and before content.
                    newlinePos++;
                } else if (newlinePos == 4) {
                    String digest = new DigestUtils(SHA_256).digestAsHex(Base64.decodeBase64(response));
                    if (hash.equals(digest)) {
                        Path file = Paths.get(filename);
                        Files.write(file, Base64.decodeBase64(response));
                        System.out.println("The file was successfully saved.");
                    } else {
                        System.err.println("The received data does not match the sent hash!");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error sending data to server.");
        }

    }
}