package com.gra.paradise.botattendance.model;

import lombok.Getter;

@Getter
public enum MissionType {
    PATROL("Patrulhamento"),
    ACTION("Ação"),
    OTHER("Outros");

    private final String displayName;

    MissionType(String displayName) {
        this.displayName = displayName;
    }
}