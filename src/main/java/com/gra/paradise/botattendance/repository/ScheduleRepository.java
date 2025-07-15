package com.gra.paradise.botattendance.repository;

import com.gra.paradise.botattendance.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByActiveTrue();

    List<Schedule> findByActiveTrueAndGuildId(String guildId);

    Optional<Schedule> findByMessageIdAndChannelId(String messageId, String channelId);

    @Query("SELECT s FROM Schedule s JOIN FETCH s.crewMembers WHERE s.id = :scheduleId AND s.guildId = :guildId")
    Optional<Schedule> findByIdAndGuildIdWithCrew(@Param("scheduleId") Long scheduleId, @Param("guildId") String guildId);

    @Query("SELECT s FROM Schedule s WHERE s.id = :scheduleId AND s.guildId = :guildId")
    Optional<Schedule> findByIdAndGuildId(@Param("scheduleId") Long scheduleId, @Param("guildId") String guildId);
}
