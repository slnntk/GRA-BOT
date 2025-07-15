package com.gra.paradise.botattendance.exception;

public class UserNotBoardedException extends RuntimeException {
    public UserNotBoardedException() {
        super("Usuário não está embarcado.");
    }

    public UserNotBoardedException(String message) {
        super(message);
    }
}
