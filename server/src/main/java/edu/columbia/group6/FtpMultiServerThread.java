package edu.columbia.group6;

import edu.columbia.group6.exception.FtpException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * Thread to manage FTP client connections.
 */
public class FtpMultiServerThread extends Thread {
    private final Socket socket;

    /**
     * Constructor for the thread.
     *
     * @param socket The client socket.
     */
    public FtpMultiServerThread(Socket socket) {
        super("FtpMultiServerThread");
        this.socket = socket;
    }

    /**
     * Method to manage a client socket connection.
     */
    public void run() {
        try (
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            Scanner scanner = new Scanner(in);
            FtpProtocol proto = new FtpProtocol();

            while (true) {
                try {
                    FtpResponse response = proto.process(scanner);

                    if (!response.getStatusCode().equals(FtpStatusCode.CONTINUE)) {
                        System.out.println(response);
                        out.println(response);
                        out.println(Character.MIN_VALUE);
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatResponse(FtpResponse response) {
        return response.toString();
    }
}
