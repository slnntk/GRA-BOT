package com.gra.paradise.botattendance.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AircraftTypeTest {

    @Test
    void testHelicopterVehicleTypes() {
        assertTrue(AircraftType.EC135.isHelicopter());
        assertTrue(AircraftType.MAVERICK.isHelicopter());
        assertTrue(AircraftType.VALKYRE.isHelicopter());
        
        assertEquals("Helicóptero", AircraftType.EC135.getVehicleTypeDisplayName());
        assertEquals("Helicóptero", AircraftType.MAVERICK.getVehicleTypeDisplayName());
        assertEquals("Helicóptero", AircraftType.VALKYRE.getVehicleTypeDisplayName());
    }

    @Test
    void testGroundVehicleTypes() {
        assertFalse(AircraftType.VECTREII.isHelicopter());
        assertEquals("Viatura", AircraftType.VECTREII.getVehicleTypeDisplayName());
        assertEquals("Vectre II", AircraftType.VECTREII.getDisplayName());
    }

    @Test
    void testDisplayNames() {
        assertEquals("EC135", AircraftType.EC135.getDisplayName());
        assertEquals("Maverick", AircraftType.MAVERICK.getDisplayName());
        assertEquals("Valkyre", AircraftType.VALKYRE.getDisplayName());
        assertEquals("Vectre II", AircraftType.VECTREII.getDisplayName());
    }
}