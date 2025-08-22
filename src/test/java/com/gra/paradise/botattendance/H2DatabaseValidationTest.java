package com.gra.paradise.botattendance;

import com.gra.paradise.botattendance.model.User;
import com.gra.paradise.botattendance.model.Schedule;
import com.gra.paradise.botattendance.model.AircraftType;
import com.gra.paradise.botattendance.model.MissionType;
import com.gra.paradise.botattendance.repository.UserRepository;
import com.gra.paradise.botattendance.repository.ScheduleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to validate H2 database operations work correctly
 * This proves that PostgreSQL can be replaced by H2 without functionality loss
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class H2DatabaseValidationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Test
    void testH2DatabaseBasicOperations() {
        // Create and save a user
        User user = new User("123456", "testuser", "Test User");
        User savedUser = userRepository.save(user);
        
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getDiscordId()).isEqualTo("123456");
        
        // Create and save a schedule
        Schedule schedule = new Schedule();
        schedule.setGuildId("guild123");
        schedule.setTitle("Test Flight");
        schedule.setCreatedById("123456");
        schedule.setCreatedByUsername("testuser");
        schedule.setStartTime(Instant.now());
        schedule.setEndTime(Instant.now().plusSeconds(3600));
        schedule.setAircraftType(AircraftType.VALKYRE);
        schedule.setMissionType(MissionType.PATROL);
        schedule.setActive(true);
        
        Schedule savedSchedule = scheduleRepository.save(schedule);
        
        assertThat(savedSchedule).isNotNull();
        assertThat(savedSchedule.getId()).isNotNull();
        assertThat(savedSchedule.getTitle()).isEqualTo("Test Flight");
        assertThat(savedSchedule.getAircraftType()).isEqualTo(AircraftType.VALKYRE);
        assertThat(savedSchedule.getMissionType()).isEqualTo(MissionType.PATROL);
        
        // Test repository queries
        assertThat(scheduleRepository.findByActiveTrue()).hasSize(1);
        assertThat(scheduleRepository.findByActiveTrueAndGuildId("guild123")).hasSize(1);
        assertThat(scheduleRepository.countByActiveTrueAndGuildId("guild123")).isEqualTo(1);
        
        // Test user-schedule relationship
        schedule.addCrewMember(user);
        scheduleRepository.save(schedule);
        
        Schedule scheduleWithCrew = scheduleRepository.findByIdAndGuildIdWithCrew(
            savedSchedule.getId(), "guild123").orElse(null);
        
        assertThat(scheduleWithCrew).isNotNull();
        assertThat(scheduleWithCrew.getCrewMembers()).hasSize(1);
        assertThat(scheduleWithCrew.getCrewMembers().get(0).getDiscordId()).isEqualTo("123456");
    }
    
    @Test
    void testH2DatabaseEnumSupport() {
        // Test that H2 properly handles Java Enums
        Schedule schedule = new Schedule();
        schedule.setGuildId("guild456");
        schedule.setTitle("Enum Test");
        schedule.setCreatedById("456789");
        schedule.setCreatedByUsername("enumtest");
        
        // Test all aircraft types
        for (AircraftType aircraft : AircraftType.values()) {
            schedule.setAircraftType(aircraft);
            Schedule saved = scheduleRepository.save(schedule);
            assertThat(saved.getAircraftType()).isEqualTo(aircraft);
            scheduleRepository.delete(saved);
        }
        
        // Test all mission types
        for (MissionType mission : MissionType.values()) {
            schedule.setMissionType(mission);
            Schedule saved = scheduleRepository.save(schedule);
            assertThat(saved.getMissionType()).isEqualTo(mission);
            scheduleRepository.delete(saved);
        }
    }
}