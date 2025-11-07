package com.darpan.databaseAiAgent.api;

public class ValidationResult {

    private final boolean ok;
    private final String message;

    public ValidationResult(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    public boolean isOk() {
        return ok;
    }

    public String getMessage() {
        return message;
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, "OK");
    }

    public static ValidationResult error(String msg) {
        return new ValidationResult(false, msg);
    }
}