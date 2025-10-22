package com.gra.paradise.botattendance.service;

import com.gra.paradise.botattendance.exception.OnlyCreatorCanCloseScheduleException;
import com.gra.paradise.botattendance.exception.ScheduleNotFoundException;
import com.gra.paradise.botattendance.model.Schedule;
import com.gra.paradise.botattendance.repository.ScheduleRepository;
import com.gra.paradise.botattendance.repository.ScheduleLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for verifying the force close functionality in ScheduleManager
 * This tests the specific scenario where a schedule is not found in the database
 * but the user has the special role to force close it.
 */
class ScheduleManagerForceCloseTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleLogRepository scheduleLogRepository;

    @Mock
    private DiscordService discordService;

    @Mock
    private UserService userService;

    @Mock
    private ScheduleLogManager logManager;

    @Mock
    private PerformanceMetricsService performanceMetrics;

    @Mock
    private CacheService cacheService;

    private ScheduleManager scheduleManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scheduleManager = new ScheduleManager(scheduleRepository, scheduleLogRepository, userService, logManager, discordService, performanceMetrics, cacheService);
    }

    @Test
    void testForceCloseScheduleWithSpecialRole() {
        // Given
        String guildId = "testGuildId";
        Long scheduleId = 123L;
        String discordId = "userWithSpecialRole";
        String nickname = "TestUser";
        String specialRoleId = "1393974475321507953";

        // Mock that schedule is not found in database
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());
        
        // Mock that user has the special role
        when(discordService.checkUserHasRole(guildId, discordId, specialRoleId)).thenReturn(true);

        // When
        Schedule result = scheduleManager.closeSchedule(guildId, scheduleId, discordId, nickname);

        // Then
        assertNull(result, "Force close should return null to indicate force close scenario");
        verify(discordService).checkUserHasRole(guildId, discordId, specialRoleId);
        verify(scheduleRepository).findById(scheduleId);
        // Should not attempt to save anything since schedule doesn't exist
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void testForceCloseScheduleWithoutSpecialRole() {
        // Given
        String guildId = "testGuildId";
        Long scheduleId = 123L;
        String discordId = "userWithoutSpecialRole";
        String nickname = "TestUser";
        String specialRoleId = "1393974475321507953";

        // Mock that schedule is not found in database
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());
        
        // Mock that user does NOT have the special role
        when(discordService.checkUserHasRole(guildId, discordId, specialRoleId)).thenReturn(false);

        // When & Then
        assertThrows(ScheduleNotFoundException.class, () -> {
            scheduleManager.closeSchedule(guildId, scheduleId, discordId, nickname);
        }, "Should throw ScheduleNotFoundException when user doesn't have special role and schedule doesn't exist");

        verify(discordService).checkUserHasRole(guildId, discordId, specialRoleId);
        verify(scheduleRepository).findById(scheduleId);
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void testNormalCloseScheduleWhenScheduleExists() {
        // Given
        String guildId = "testGuildId";
        Long scheduleId = 123L;
        String discordId = "creator";
        String nickname = "TestUser";

        Schedule existingSchedule = new Schedule();
        existingSchedule.setId(scheduleId);
        existingSchedule.setGuildId(guildId);
        existingSchedule.setActive(true);
        existingSchedule.setCreatedById(discordId); // User is creator

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(existingSchedule));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(existingSchedule);
        when(logManager.createFinalScheduleLogMessage(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(reactor.core.publisher.Mono.empty());

        // When
        Schedule result = scheduleManager.closeSchedule(guildId, scheduleId, discordId, nickname);

        // Then
        assertNotNull(result, "Normal close should return the closed schedule");
        assertFalse(result.isActive(), "Schedule should be marked as inactive");
        verify(scheduleRepository).save(any(Schedule.class));
    }
}