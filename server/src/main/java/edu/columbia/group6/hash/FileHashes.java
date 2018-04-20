package edu.columbia.group6.hash;

import edu.columbia.group6.exception.FtpException;

import java.io.*;
import java.sql.*;

public class FileHashes implements Serializable {
    private Connection connection;

    /**
     * FileHashes constructor.
     */
    public FileHashes() {
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:./db/hashes.sqlite");
        } catch (SQLException e) {
            System.err.println(e);
        }
    }

    public void addFile(String filename, String hash) throws FtpException {
        try {
            PreparedStatement exists = this.connection.prepareStatement(
                    "SELECT hash FROM hashes WHERE filename = ?");
            exists.setString(1, filename);
            ResultSet rs = exists.executeQuery();

            if (!rs.next()) {
                PreparedStatement statement = this.connection.prepareStatement(
                        "INSERT INTO hashes (hash, filename) \n" +
                                "  VALUES (?,?)");
                statement.setString(1, hash);
                statement.setString(2, filename);
                statement.executeUpdate();
            } else {
                PreparedStatement statement = this.connection.prepareStatement(
                        "UPDATE hashes SET hash = ? WHERE filename = ?");
                statement.setString(1, hash);
                statement.setString(2, filename);
                statement.executeUpdate();
            }

            this.connection.close();
        } catch (SQLException e) {
            System.err.println(e);
            throw new FtpException("Failed to insert hash into database.");
        }
    }

    public String getHash(String filename) {
        String hash = "";

        try {
            PreparedStatement statement = this.connection.prepareStatement(
                    "SELECT hash FROM hashes WHERE filename = ?");
            statement.setString(1, filename);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                hash = rs.getString(1);
            }

            this.connection.close();
        } catch (SQLException e) {
            System.err.println(e);
        }

        return hash;
    }
}
