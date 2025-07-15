package com.gra.paradise.botattendance.exception;

public class ScheduleAlreadyClosedException extends RuntimeException {
    public ScheduleAlreadyClosedException() {
        super("A escala já está encerrada.");
    }
}
