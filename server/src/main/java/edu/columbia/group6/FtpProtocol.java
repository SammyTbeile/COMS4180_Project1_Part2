package edu.columbia.group6;

import edu.columbia.group6.exception.FtpException;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class FtpProtocol {
    private int newLineCount = 0;
    private String command;
    private String hash;
    private String filename;
    private String response;
    private int responseStatus;


    public FtpResponse process(Scanner scanner, String logName) {
        if (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            if (line.startsWith("PUT ")) {
                String[] pieces = line.substring(4).split("\\s+");

                if (pieces.length != 2) {
                    throw new RuntimeException("Invalid number of PUT parameters.");
                }

                String filename = pieces[0];
                String hash = pieces[1];

                this.command = "PUT";
                this.hash = hash;
                this.filename = filename;
                this.newLineCount++;

                return new FtpResponse(FtpStatusCode.CONTINUE);
            }

            if (line.startsWith("GET ")) {
                this.filename = line.substring(4);
                FileWriter log = null;
                try {
                  log = new FileWriter(logName, true);
                  log.write(this.filename);
                } catch (IOException e) {
                  System.out.println("Failed to open log file");
                }

                try {
                    FtpCommand cmd = new FtpCommand();
                    byte[] file = cmd.getFileContents(this.filename);
                    String hash = cmd.getFileHash(this.filename);
                    if (log != null){
                      try {
                        log.write(": sent\n");
                        log.close();
                      } catch (IOException e){
                        System.out.println("Failed to write to log file");
                      }
                    }

                    return new FtpResponse(FtpStatusCode.SUCCESS, "GET " + this.filename, new String(file, StandardCharsets.US_ASCII), hash);
                } catch (IOException io) {
                    if (log != null){
                      try {
                        log.write(": failed to send\n");
                        log.close();
                      } catch (IOException e){
                        System.out.println("Failed to write to log file");
                      }
                    }
                    return new FtpResponse(FtpStatusCode.FILE_NOT_FOUND, "GET " + this.filename);
                } catch (Exception io) {
                  if (log != null){
                    try {
                      log.write(": failed to send\n");
                      log.close();
                    } catch (IOException e){
                      System.out.println("Failed to write to log file");
                    }
                  }
                    return new FtpResponse(FtpStatusCode.GENERAL_ERROR, "GET " + this.filename);
                }
            }

            if (line.startsWith("LS")) {
                FtpCommand cmd = new FtpCommand();

                try {
                    List<Path> paths = cmd.ls();

                    StringBuilder output = new StringBuilder();

                    for (Path path : paths) {
                        output.append(path.getFileName().toString()).append("\n");
                    }

                    return new FtpResponse(FtpStatusCode.SUCCESS, "LS", StringUtils.strip(output.toString(), "\n"));
                } catch (FtpException e) {
                    return new FtpResponse(FtpStatusCode.GENERAL_ERROR, "LS", "Unable to LS.");
                }
            }

            if (line.length() == 0 && this.command.equals("PUT") && this.newLineCount == 1) {
                this.newLineCount++;
                return new FtpResponse(FtpStatusCode.CONTINUE);
            }

            if (this.newLineCount == 2 && this.command.equals("PUT")) {
                FtpCommand cmd = new FtpCommand();

                try {
                    cmd.put(filename, hash, line.getBytes());
                    this.reset();

                    return new FtpResponse(FtpStatusCode.SUCCESS, "PUT");
                } catch (FtpException ftp) {
                    System.err.println(ftp.getMessage());
                    return new FtpResponse(FtpStatusCode.GENERAL_ERROR, "PUT", "Unable to save file.");
                }
            }
        }

        return new FtpResponse(FtpStatusCode.CONTINUE);
    }

    private void reset() {
        this.newLineCount = 0;
        this.command = "";
        this.hash = "";
        this.filename = "";
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }
}
