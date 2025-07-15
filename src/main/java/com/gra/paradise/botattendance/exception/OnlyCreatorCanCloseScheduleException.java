package com.gra.paradise.botattendance.exception;

public class OnlyCreatorCanCloseScheduleException extends IllegalStateException {
    public OnlyCreatorCanCloseScheduleException() {
        super("Somente o criador pode encerrar a escala.");
    }
}
