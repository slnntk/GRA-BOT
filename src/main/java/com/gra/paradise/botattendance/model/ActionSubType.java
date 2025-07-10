package com.gra.paradise.botattendance.model;

import lombok.Getter;

@Getter
public enum ActionSubType {
    FUGA("Fuga"),
    TIRO("Tiro");

    private final String displayName;

    ActionSubType(String displayName) {
        this.displayName = displayName;
    }

}