package com.gra.paradise.botattendance.exception;

public class PilotCannotBeCrewException extends RuntimeException {

    public PilotCannotBeCrewException() {
        super("O piloto não pode embarcar como tripulante.");
    }

    public PilotCannotBeCrewException(String message) {
        super(message);
    }
}
