package com.gra.paradise.botattendance.exception;

public class InvalidCustomIdException extends RuntimeException {
    public InvalidCustomIdException(String action) {
        super("Custom ID inválido para ação: " + action);
    }
}
