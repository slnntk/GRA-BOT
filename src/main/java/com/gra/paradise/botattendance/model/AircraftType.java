package com.gra.paradise.botattendance.model;

import lombok.Getter;

@Getter
public enum AircraftType {
    EC135("EC135", true),
    MAVERICK("Maverick", true),
    VALKYRE("Valkyre", true),
    VECTREII("Vectre II", false);

    private final String displayName;
    private final boolean isHelicopter;

    AircraftType(String displayName, boolean isHelicopter) {
        this.displayName = displayName;
        this.isHelicopter = isHelicopter;
    }

    /**
     * Gets the vehicle type display name (Helicóptero or Viatura)
     */
    public String getVehicleTypeDisplayName() {
        return isHelicopter ? "Helicóptero" : "Viatura";
    }

}