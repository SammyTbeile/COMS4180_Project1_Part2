package edu.columbia.group6;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.ServerSocket;

public class Server {

    private int port;

    private boolean run = true;

    private String logName;

    /**
     * Main method
     *
     * @param args String[]
     */
    public static void main(String[] args) {
        // Set up the command line options.
        CommandLineParser cliParser = new DefaultParser();
        Options options = new Options();
        options.addOption(Option.builder().argName("p").required(true).hasArg(true).longOpt("port").build());
        options.addOption(Option.builder().argName("l").required(true).hasArg(true).longOpt("log").build());

        try {
            CommandLine cli = cliParser.parse(options, args);

            final Server server = new Server();
            server.setOptions(cli);

            // Add a shutdown hook for the server so that the server terminates on ctrl-c interrupt.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                // Terminate the socket server.
                server.run = false;
                System.out.println("Received shutdown signal. Shutting down server.");
            }));

            // Start the server.
            try {
                server.runServer();
            } catch (RuntimeException e) { // Catch any runtime errors and specify that this was a server error.
                server.run = false; // Terminate the server loop.
                System.err.println("Server error. " + e.getMessage());

                System.exit(0);
            }

        } catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getMessage());

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("server", options);

            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unknown error.");

            System.exit(1);
        }
    }

    /**
     * Set up options from the command line.
     *
     * @param cli                   CommandLine
     * @throws RuntimeException
     */
    private void setOptions(CommandLine cli) throws RuntimeException {
        // Check the port. Port must be a valid IANA port number.
        if (cli.hasOption("port")) {
            try {
                int checkPort = Integer.parseInt(cli.getOptionValue("port"));

                if (checkPort >= 1 && checkPort <= 65535) {
                    this.port = checkPort;
                } else {
                    throw new RuntimeException("Port must be a value between 1 and 65535, inclusive.");
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException("Port must be a number");
            }
        }
        if (cli.hasOption("log")){
          this.logName = cli.getOptionValue("log");
        }
    }

    /**
     * Adapted from https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html.
     *
     * @throws RuntimeException
     */
    private void runServer() throws RuntimeException {
        try {
            ServerSocket socket = new ServerSocket(this.port);

            while (this.run) {
                // Create threads so that multiple clients can connect at the same time.
                new FtpMultiServerThread(socket.accept(), this.logName).start();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error opening socket: " + e.getMessage());
        }
    }
}
