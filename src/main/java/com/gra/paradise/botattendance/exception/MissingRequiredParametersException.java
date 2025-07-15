package com.gra.paradise.botattendance.exception;

public class MissingRequiredParametersException extends RuntimeException {

    public MissingRequiredParametersException() {
        super("Parâmetros obrigatórios ausentes.");
    }

    public MissingRequiredParametersException(String message) {
        super(message);
    }
}
