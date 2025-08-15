package com.gra.paradise.botattendance.repository;

import com.gra.paradise.botattendance.model.PatrolParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatrolParticipantRepository extends JpaRepository<PatrolParticipant, Long> {
    
    Optional<PatrolParticipant> findByContestIdAndDiscordId(Long contestId, String discordId);
    
    List<PatrolParticipant> findByContestIdAndEligibleTrue(Long contestId);
    
    List<PatrolParticipant> findByContestIdAndAfternoonEligibleTrue(Long contestId);
    
    List<PatrolParticipant> findByContestIdAndNightEligibleTrue(Long contestId);
    
    @Query("SELECT p FROM PatrolParticipant p WHERE p.contest.id = :contestId " +
           "AND p.afternoonEligible = true AND p.afternoonWinner = false")
    List<PatrolParticipant> findAfternoonEligibleNonWinners(@Param("contestId") Long contestId);
    
    List<PatrolParticipant> findByContestIdAndAfternoonWinnerTrue(Long contestId);
    
    List<PatrolParticipant> findByContestIdAndNightVipWinnerTrue(Long contestId);
}