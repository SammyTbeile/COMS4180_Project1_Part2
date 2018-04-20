package edu.columbia.group6;

import edu.columbia.group6.exception.FtpException;
import edu.columbia.group6.hash.FileHashes;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class FtpCommand {
    private String docRoot = "./docroot";

    public List<Path> ls() throws FtpException {
        List<Path> files = new ArrayList<>();
        Path docRoot = Paths.get(this.docRoot);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(docRoot)) {
            for (Path file: stream) {
                files.add(file);
            }

            return files;
        } catch (IOException | DirectoryIteratorException e) {
            throw new FtpException("Unable to iterate through ftp docroot.");
        }
    }

    /**
     * Return a base64 string of the file contents.
     *
     * @param filename
     * @return
     * @throws IOException
     */
    public byte[] getFileContents(String filename) throws IOException {
        File file = new File(this.docRoot + "/" + filename);

        return Base64.encodeBase64(FileUtils.readFileToByteArray(file));
    }

    public String getFileHash(String filename) throws FtpException {
        FileHashes fh = new FileHashes();
        return fh.getHash(filename);
    }

    /**
     *
     * @param filename
     * @param hash
     * @param content
     * @throws IOException
     */
    public void put(String filename, String hash, byte[] content) throws FtpException {
        try {
            System.err.println("putting data");
            Path file = Paths.get(this.docRoot + "/" + filename);
            Files.write(file, Base64.decodeBase64(content));

            System.err.println("wrote data");

            FileHashes fh = new FileHashes();
            fh.addFile(filename, hash);

            System.err.println("file hash written");
        } catch (IOException io) {
            System.err.println(io.getMessage());
        }
    }
}
