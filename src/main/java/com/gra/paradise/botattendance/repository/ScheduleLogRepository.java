package com.gra.paradise.botattendance.repository;

import com.gra.paradise.botattendance.model.ScheduleLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleLogRepository extends JpaRepository<ScheduleLog, Long> {
    List<ScheduleLog> findByScheduleIdOrderByTimestampAsc(Long scheduleId);

    void deleteByScheduleId(Long scheduleId);
}