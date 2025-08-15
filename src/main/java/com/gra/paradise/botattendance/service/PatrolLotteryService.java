package com.gra.paradise.botattendance.service;

import com.gra.paradise.botattendance.model.PatrolContest;
import com.gra.paradise.botattendance.model.PatrolParticipant;
import com.gra.paradise.botattendance.repository.PatrolParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatrolLotteryService {
    
    private final PatrolParticipantRepository patrolParticipantRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Transactional
    public List<PatrolParticipant> drawAfternoonWinners(PatrolContest contest) {
        List<PatrolParticipant> eligible = patrolParticipantRepository
            .findByContestIdAndAfternoonEligibleTrue(contest.getId());
        
        if (eligible.isEmpty()) {
            log.info("No eligible participants for afternoon drawing in contest {}", contest.getId());
            return new ArrayList<>();
        }
        
        // Reset previous winners (in case of re-drawing)
        eligible.forEach(p -> p.setAfternoonWinner(false));
        patrolParticipantRepository.saveAll(eligible);
        
        // Shuffle and select winners
        List<PatrolParticipant> shuffled = new ArrayList<>(eligible);
        Collections.shuffle(shuffled, secureRandom);
        
        int winnersCount = Math.min(contest.getAfternoonWinners(), shuffled.size());
        List<PatrolParticipant> winners = shuffled.subList(0, winnersCount);
        
        // Mark as winners
        for (PatrolParticipant winner : winners) {
            winner.setAfternoonWinner(true);
            log.info("Afternoon winner selected: {} ({}) with {:.1f}h afternoon, {:.1f}h total", 
                    winner.getNickname(), winner.getDiscordId(), 
                    winner.getTotalAfternoonHours(), winner.getTotalHours());
        }
        
        patrolParticipantRepository.saveAll(winners);
        
        log.info("Drew {} afternoon winners from {} eligible participants", winners.size(), eligible.size());
        return winners;
    }
    
    @Transactional
    public List<PatrolParticipant> drawNightVipWinners(PatrolContest contest) {
        // Night VIP winners include:
        // 1. Afternoon eligible participants who didn't win afternoon prizes
        // 2. Night eligible participants
        List<PatrolParticipant> afternoonNonWinners = patrolParticipantRepository
            .findAfternoonEligibleNonWinners(contest.getId());
        List<PatrolParticipant> nightEligible = patrolParticipantRepository
            .findByContestIdAndNightEligibleTrue(contest.getId());
        
        // Combine and deduplicate
        List<PatrolParticipant> eligible = new ArrayList<>(afternoonNonWinners);
        for (PatrolParticipant night : nightEligible) {
            if (!eligible.contains(night)) {
                eligible.add(night);
            }
        }
        
        if (eligible.isEmpty()) {
            log.info("No eligible participants for night VIP drawing in contest {}", contest.getId());
            return new ArrayList<>();
        }
        
        // Reset previous VIP winners (in case of re-drawing)
        eligible.forEach(p -> p.setNightVipWinner(false));
        patrolParticipantRepository.saveAll(eligible);
        
        // Shuffle and select winners
        List<PatrolParticipant> shuffled = new ArrayList<>(eligible);
        Collections.shuffle(shuffled, secureRandom);
        
        int winnersCount = Math.min(contest.getNightVipWinners(), shuffled.size());
        List<PatrolParticipant> winners = shuffled.subList(0, winnersCount);
        
        // Mark as VIP winners
        for (PatrolParticipant winner : winners) {
            winner.setNightVipWinner(true);
            log.info("Night VIP winner selected: {} ({}) with {:.1f}h night, {:.1f}h total", 
                    winner.getNickname(), winner.getDiscordId(), 
                    winner.getTotalNightHours(), winner.getTotalHours());
        }
        
        patrolParticipantRepository.saveAll(winners);
        
        log.info("Drew {} night VIP winners from {} eligible participants", winners.size(), eligible.size());
        return winners;
    }
    
    @Transactional(readOnly = true)
    public List<PatrolParticipant> getAfternoonWinners(Long contestId) {
        return patrolParticipantRepository.findByContestIdAndAfternoonWinnerTrue(contestId);
    }
    
    @Transactional(readOnly = true)
    public List<PatrolParticipant> getNightVipWinners(Long contestId) {
        return patrolParticipantRepository.findByContestIdAndNightVipWinnerTrue(contestId);
    }
    
    @Transactional(readOnly = true)
    public boolean hasDrawnWinners(Long contestId) {
        return !patrolParticipantRepository.findByContestIdAndAfternoonWinnerTrue(contestId).isEmpty() ||
               !patrolParticipantRepository.findByContestIdAndNightVipWinnerTrue(contestId).isEmpty();
    }
    
    public LotteryResults performFullLottery(PatrolContest contest) {
        log.info("Starting full lottery for contest: {}", contest.getTitle());
        
        List<PatrolParticipant> afternoonWinners = drawAfternoonWinners(contest);
        List<PatrolParticipant> nightVipWinners = drawNightVipWinners(contest);
        
        return new LotteryResults(afternoonWinners, nightVipWinners);
    }
    
    public static class LotteryResults {
        private final List<PatrolParticipant> afternoonWinners;
        private final List<PatrolParticipant> nightVipWinners;
        
        public LotteryResults(List<PatrolParticipant> afternoonWinners, List<PatrolParticipant> nightVipWinners) {
            this.afternoonWinners = afternoonWinners;
            this.nightVipWinners = nightVipWinners;
        }
        
        public List<PatrolParticipant> getAfternoonWinners() { return afternoonWinners; }
        public List<PatrolParticipant> getNightVipWinners() { return nightVipWinners; }
        public int getTotalWinners() { return afternoonWinners.size() + nightVipWinners.size(); }
    }
}