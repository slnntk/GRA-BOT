package com.gra.paradise.botattendance.service;

import com.gra.paradise.botattendance.model.*;
import com.gra.paradise.botattendance.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleManager {

    private final ScheduleRepository scheduleRepository;
    private final UserService userService;
    private final ScheduleLogManager logManager;

    @Transactional
    public Schedule createSchedule(String title, AircraftType aircraftType, MissionType missionType,
                                   String creatorId, String creatorNickname, ActionSubType actionSubType,
                                   String actionOption) {
        Schedule schedule = new Schedule();
        schedule.setTitle(title);
        schedule.setAircraftType(aircraftType);
        schedule.setMissionType(missionType);
        schedule.setActionSubType(actionSubType);
        schedule.setActionOption(actionOption);
        schedule.setStartTime(Instant.now());
        schedule.setCreatedById(creatorId);
        schedule.setCreatedByUsername(creatorNickname);
        schedule.setActive(true);

        Schedule savedSchedule = scheduleRepository.save(schedule);
        log.info("Escala criada: {} (ID: {}, Tipo: {}, Subtipo: {}, Opção: {})",
                title, savedSchedule.getId(), missionType.getDisplayName(),
                actionSubType != null ? actionSubType.getDisplayName() : "N/A",
                actionOption != null ? actionOption : "N/A");

        logManager.createScheduleLog(savedSchedule, "CREATED", creatorId, creatorNickname,
                "Escala criada com aeronave " + aircraftType.getDisplayName() +
                        " para missão de " + missionType.getDisplayName() +
                        (actionSubType != null ? " (" + actionSubType.getDisplayName() + ": " + actionOption + ")" : ""));

        return savedSchedule;
    }

    @Transactional
    public Schedule addCrewMember(Long scheduleId, String discordId, String username, String nickname) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Escala não encontrada"));

        if (!schedule.isActive()) {
            throw new IllegalStateException("Esta escala já foi encerrada");
        }

        boolean alreadyInCrew = schedule.getCrewMembers().stream()
                .anyMatch(user -> user.getDiscordId().equals(discordId));

        if (alreadyInCrew) {
            throw new IllegalStateException("Você já está embarcado nesta aeronave");
        }

        User user = userService.getOrCreateUser(discordId, username, nickname);
        schedule.addCrewMember(user);

        Schedule updatedSchedule = scheduleRepository.save(schedule);
        log.info("Tripulante {} embarcou na escala {} (ID: {})", nickname, schedule.getTitle(), scheduleId);

        logManager.createScheduleLog(updatedSchedule, "JOINED", discordId, nickname,
                "Tripulante embarcou na aeronave");

        return updatedSchedule;
    }

    @Transactional
    public Schedule addCrewMemberAndInitialize(Long scheduleId, String discordId, String username, String nickname) {
        Schedule schedule = addCrewMember(scheduleId, discordId, username, nickname);
        schedule.initializeCrewMembers();
        return schedule;
    }

    @Transactional
    public Schedule removeCrewMember(Long scheduleId, String discordId, String nickname) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Escala não encontrada"));

        if (!schedule.isActive()) {
            throw new IllegalStateException("Esta escala já foi encerrada");
        }

        User user = schedule.getCrewMembers().stream()
                .filter(u -> u.getDiscordId().equals(discordId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Você não está embarcado nesta aeronave"));

        schedule.removeCrewMember(user);

        Schedule updatedSchedule = scheduleRepository.save(schedule);
        log.info("Tripulante {} desembarcou da escala {} (ID: {})", nickname, schedule.getTitle(), scheduleId);

        logManager.createScheduleLog(updatedSchedule, "LEFT", discordId, nickname,
                "Tripulante desembarcou da aeronave");

        return updatedSchedule;
    }

    @Transactional
    public Schedule removeCrewMemberAndInitialize(Long scheduleId, String discordId, String nickname) {
        Schedule schedule = removeCrewMember(scheduleId, discordId, nickname);
        schedule.initializeCrewMembers();
        return schedule;
    }

    @Transactional
    public Schedule closeSchedule(Long scheduleId, String discordId, String nickname) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Escala não encontrada"));

        if (!schedule.isActive()) {
            throw new IllegalStateException("Esta escala já foi encerrada");
        }

        Instant endTime = Instant.now();
        schedule.setActive(false);
        schedule.setEndTime(endTime);

        Schedule updatedSchedule = scheduleRepository.save(schedule);
        log.info("Escala {} (ID: {}) encerrada por {}", schedule.getTitle(), scheduleId, nickname);

        logManager.createScheduleLog(updatedSchedule, "CLOSED", discordId, nickname, "Escala encerrada");

        List<String> crewNicknames = new ArrayList<>();
        if (updatedSchedule.getCrewMembers() != null) {
            for (User user : updatedSchedule.getCrewMembers()) {
                crewNicknames.add(user.getNickname());
            }
        }

        logManager.createFinalScheduleLogMessage(
                scheduleId,
                updatedSchedule.getTitle(),
                updatedSchedule.getAircraftType(),
                updatedSchedule.getMissionType(),
                updatedSchedule.getActionSubType(),
                updatedSchedule.getActionOption(),
                updatedSchedule.getStartTime(),
                endTime,
                updatedSchedule.getCreatedByUsername(),
                nickname,
                crewNicknames
        ).block();

        log.info("Escala {} (ID: {}) e seus logs foram excluídos do banco", schedule.getTitle(), scheduleId);

        return updatedSchedule;
    }

    @Transactional
    public Schedule closeScheduleAndInitialize(Long scheduleId, String discordId, String nickname) {
        Schedule schedule = closeSchedule(scheduleId, discordId, nickname);
        schedule.initializeCrewMembers();
        return schedule;
    }

    @Transactional
    public boolean addPassenger(String scheduleId, String userId, String userName) {
        try {
            Long id = Long.parseLong(scheduleId);
            addCrewMember(id, userId, userName, userName);
            return true;
        } catch (Exception e) {
            log.error("Erro ao adicionar passageiro: {}", e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public boolean removePassenger(String scheduleId, String userId) {
        try {
            Long id = Long.parseLong(scheduleId);
            Schedule schedule = scheduleRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Escala não encontrada"));

            String nickname = schedule.getCrewMembers().stream()
                    .filter(u -> u.getDiscordId().equals(userId))
                    .findFirst()
                    .map(User::getNickname)
                    .orElse("Desconhecido");

            removeCrewMember(id, userId, nickname);
            return true;
        } catch (Exception e) {
            log.error("Erro ao remover passageiro: {}", e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public boolean endSchedule(String scheduleId) {
        try {
            Long id = Long.parseLong(scheduleId);
            Schedule schedule = scheduleRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Escala não encontrada"));

            closeSchedule(id, schedule.getCreatedById(), schedule.getCreatedByUsername());
            return true;
        } catch (Exception e) {
            log.error("Erro ao encerrar escala {}: {}", scheduleId, e.getMessage(), e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean canEndSchedule(String scheduleId, String userId) {
        try {
            Long id = Long.parseLong(scheduleId);
            Schedule schedule = scheduleRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Escala não encontrada"));

            boolean canEnd = schedule.getCreatedById().equals(userId);
            log.debug("Verificação para encerrar escala {}: usuário {} {} permissão",
                    scheduleId, userId, canEnd ? "tem" : "não tem");
            return canEnd;
        } catch (Exception e) {
            log.error("Erro ao verificar permissão para encerrar: {}", e.getMessage(), e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<Schedule> getActiveSchedules() {
        return scheduleRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Schedule> getActiveSchedulesWithDetails() {
        List<Schedule> schedules = getActiveSchedules();
        for (Schedule schedule : schedules) {
            schedule.initializeCrewMembers();
        }
        return schedules;
    }

    @Transactional(readOnly = true)
    public Optional<Schedule> findByMessageInfo(String messageId, String channelId) {
        return scheduleRepository.findByMessageIdAndChannelId(messageId, channelId);
    }

    @Transactional
    public void updateMessageInfo(Long scheduleId, String messageId, String channelId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Escala não encontrada"));

        schedule.setMessageId(messageId);
        schedule.setChannelId(channelId);

        scheduleRepository.save(schedule);
    }

    @Transactional(readOnly = true)
    public String generateGraTitle() {
        int activeCount = getActiveSchedules().size() + 1;
        return "G.R.A - " + activeCount;
    }
}