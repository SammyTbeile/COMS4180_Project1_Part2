package edu.columbia.group6;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that defines FTP status codes.
 * For more information, see https://stackoverflow.com/a/1080912.
 */
public enum FtpStatusCode {
    SUCCESS(250),
    GENERAL_ERROR(500),
    SYNTAX_ERROR(501),
    COMMAND_NOT_RECOGNIZED(502),
    BAD_SEQUENCE(503),
    FILE_NOT_FOUND(550),
    FILE_TOO_BIG(552),
    CONTINUE(-1),
    ;

    private final Integer status;

    // Reverse lookup
    private static final Map<Integer, FtpStatusCode> lookup = new HashMap<Integer, FtpStatusCode>();

    static {
        for (FtpStatusCode d : FtpStatusCode.values()) {
            lookup.put(d.getStatus(), d);
        }
    }

    private FtpStatusCode(Integer status) {
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }

    public static FtpStatusCode get(Integer status) {
        return lookup.get(status);
    }
}
