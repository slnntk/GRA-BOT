package com.gra.paradise.botattendance.exception;

public class ScheduleModificationNotAllowedException extends RuntimeException {
    public ScheduleModificationNotAllowedException() {
        super("A escala não pode ser modificada neste momento.");
    }
}
