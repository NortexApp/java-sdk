package com.sending.sdk.exceptions;

import java.io.IOException;

public class SDNException extends IOException {
    private String code;

    public SDNException() {
        super();
    }
    public SDNException(String message) {
        super(message);
    }

    public SDNException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
