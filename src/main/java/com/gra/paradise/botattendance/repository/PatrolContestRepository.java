package com.gra.paradise.botattendance.repository;

import com.gra.paradise.botattendance.model.PatrolContest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatrolContestRepository extends JpaRepository<PatrolContest, Long> {
    
    List<PatrolContest> findByGuildIdAndActiveTrue(String guildId);
    
    Optional<PatrolContest> findByGuildIdAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        String guildId, Instant currentTime1, Instant currentTime2);
    
    @Query("SELECT c FROM PatrolContest c WHERE c.guildId = :guildId AND c.active = true " +
           "AND c.startDate <= :currentTime AND c.endDate >= :currentTime")
    Optional<PatrolContest> findActiveContestForGuild(@Param("guildId") String guildId, 
                                                      @Param("currentTime") Instant currentTime);
}