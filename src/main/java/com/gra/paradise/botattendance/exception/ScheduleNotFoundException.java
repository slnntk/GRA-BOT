package com.gra.paradise.botattendance.exception;

public class ScheduleNotFoundException extends RuntimeException {
    public ScheduleNotFoundException(Long scheduleId) {
        super("Escala com ID " + scheduleId + " não foi encontrada.");
    }
}
