package com.gra.paradise.botattendance.exception;

public class UserAlreadyBoardedException extends RuntimeException {
    public UserAlreadyBoardedException() {
        super("Usuário já está embarcado.");
    }
}
