package edu.columbia.group6;

public class FtpResponse {
    private FtpStatusCode statusCode;
    private String message = "";
    private String command = "";
    private String hash = "";

    public FtpResponse(FtpStatusCode code) {
        this.statusCode = code;
    }

    public FtpResponse(FtpStatusCode statusCode, String command, String message) {
        this.statusCode = statusCode;
        this.command = command;
        this.message = message;
    }

    public FtpResponse(FtpStatusCode statusCode, String command, String message, String hash) {
        this.statusCode = statusCode;
        this.command = command;
        this.message = message;
        this.hash = hash;
    }

    public FtpResponse(FtpStatusCode statusCode, String command) {
        this.statusCode = statusCode;
        this.command = command;
    }

    @Override
    public String toString() {
        if (command.startsWith("PUT")) {
            return statusCode.getStatus() + "\n" +
                    "Request: " + command + "\n" +
                    message;
        } else if (command.startsWith("GET") && !message.equals("")) {
            return statusCode.getStatus() + "\n" +
                    "Request: " + command + "\n" +
                    "Hash: " + hash + "\n\n" +
                    message;
        } else if (command.equals("LS")) {
            return statusCode.getStatus() + "\n" +
                    "Request: " + command + "\n\n" +
                    message;
        } else {
            return statusCode.getStatus() + "\n" +
                    "Request: " + command;
        }
    }

    public FtpStatusCode getStatusCode() {
        return statusCode;
    }
}
