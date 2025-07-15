package com.gra.paradise.botattendance.exception;

public class CreatorCannotLeaveException extends RuntimeException {
    public CreatorCannotLeaveException() {
        super("O criador da escala não pode desembarcar.");
    }
}
