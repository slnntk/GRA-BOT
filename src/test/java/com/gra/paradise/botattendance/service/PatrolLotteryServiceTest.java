package com.gra.paradise.botattendance.service;

import com.gra.paradise.botattendance.model.PatrolContest;
import com.gra.paradise.botattendance.model.PatrolParticipant;
import com.gra.paradise.botattendance.repository.PatrolParticipantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PatrolLotteryServiceTest {

    @Mock
    private PatrolParticipantRepository patrolParticipantRepository;

    @InjectMocks
    private PatrolLotteryService patrolLotteryService;

    @Test
    void shouldDrawAfternoonWinners() {
        // Given
        PatrolContest contest = new PatrolContest();
        contest.setId(1L);
        contest.setAfternoonWinners(2);
        
        PatrolParticipant participant1 = new PatrolParticipant();
        participant1.setDiscordId("user1");
        participant1.setNickname("User One");
        participant1.setTotalAfternoonHours(20.0);
        participant1.setAfternoonEligible(true);
        
        PatrolParticipant participant2 = new PatrolParticipant();
        participant2.setDiscordId("user2");
        participant2.setNickname("User Two");
        participant2.setTotalAfternoonHours(18.5);
        participant2.setAfternoonEligible(true);
        
        PatrolParticipant participant3 = new PatrolParticipant();
        participant3.setDiscordId("user3");
        participant3.setNickname("User Three");
        participant3.setTotalAfternoonHours(19.0);
        participant3.setAfternoonEligible(true);
        
        List<PatrolParticipant> eligible = Arrays.asList(participant1, participant2, participant3);
        
        when(patrolParticipantRepository.findByContestIdAndAfternoonEligibleTrue(1L))
            .thenReturn(eligible);
        when(patrolParticipantRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        List<PatrolParticipant> winners = patrolLotteryService.drawAfternoonWinners(contest);
        
        // Then
        assertNotNull(winners);
        assertEquals(2, winners.size()); // Should draw 2 winners as configured
        
        // Verify all winners are marked as afternoon winners
        for (PatrolParticipant winner : winners) {
            assertTrue(winner.isAfternoonWinner());
        }
        
        verify(patrolParticipantRepository).findByContestIdAndAfternoonEligibleTrue(1L);
        verify(patrolParticipantRepository, times(2)).saveAll(any());
    }

    @Test
    void shouldDrawNightVipWinners() {
        // Given
        PatrolContest contest = new PatrolContest();
        contest.setId(1L);
        contest.setNightVipWinners(1);
        
        PatrolParticipant nightParticipant = new PatrolParticipant();
        nightParticipant.setDiscordId("night1");
        nightParticipant.setNickname("Night User");
        nightParticipant.setTotalNightHours(20.0);
        nightParticipant.setNightEligible(true);
        
        List<PatrolParticipant> afternoonNonWinners = Arrays.asList();
        List<PatrolParticipant> nightEligible = Arrays.asList(nightParticipant);
        
        when(patrolParticipantRepository.findAfternoonEligibleNonWinners(1L))
            .thenReturn(afternoonNonWinners);
        when(patrolParticipantRepository.findByContestIdAndNightEligibleTrue(1L))
            .thenReturn(nightEligible);
        when(patrolParticipantRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        List<PatrolParticipant> winners = patrolLotteryService.drawNightVipWinners(contest);
        
        // Then
        assertNotNull(winners);
        assertEquals(1, winners.size());
        assertTrue(winners.get(0).isNightVipWinner());
        assertEquals("night1", winners.get(0).getDiscordId());
        
        verify(patrolParticipantRepository).findAfternoonEligibleNonWinners(1L);
        verify(patrolParticipantRepository).findByContestIdAndNightEligibleTrue(1L);
    }

    @Test
    void shouldPerformFullLottery() {
        // Given
        PatrolContest contest = new PatrolContest();
        contest.setId(1L);
        contest.setTitle("Test Contest");
        contest.setAfternoonWinners(1);
        contest.setNightVipWinners(1);
        
        // Setup mocks for afternoon winners
        PatrolParticipant afternoonParticipant = new PatrolParticipant();
        afternoonParticipant.setDiscordId("afternoon1");
        when(patrolParticipantRepository.findByContestIdAndAfternoonEligibleTrue(1L))
            .thenReturn(Arrays.asList(afternoonParticipant));
        
        // Setup mocks for night VIP winners
        when(patrolParticipantRepository.findAfternoonEligibleNonWinners(1L))
            .thenReturn(Arrays.asList());
        
        PatrolParticipant nightParticipant = new PatrolParticipant();
        nightParticipant.setDiscordId("night1");
        when(patrolParticipantRepository.findByContestIdAndNightEligibleTrue(1L))
            .thenReturn(Arrays.asList(nightParticipant));
        
        when(patrolParticipantRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        PatrolLotteryService.LotteryResults results = patrolLotteryService.performFullLottery(contest);
        
        // Then
        assertNotNull(results);
        assertEquals(1, results.getAfternoonWinners().size());
        assertEquals(1, results.getNightVipWinners().size());
        assertEquals(2, results.getTotalWinners());
    }
}