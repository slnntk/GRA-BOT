package com.gra.paradise.botattendance.service;

import com.gra.paradise.botattendance.model.*;
import com.gra.paradise.botattendance.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        try {
            // Validate inputs
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalArgumentException("Título não pode ser nulo ou vazio");
            }
            if (aircraftType == null || missionType == null) {
                throw new IllegalArgumentException("Tipo de aeronave e missão são obrigatórios");
            }
            if (creatorId == null || creatorNickname == null) {
                throw new IllegalArgumentException("ID e apelido do criador são obrigatórios");
            }

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

            logManager.sendScheduleCreationLog(savedSchedule)
                    .doOnError(e -> log.error("Falha ao criar log inicial para escala {}: {}", savedSchedule.getId(), e.getMessage()))
                    .subscribe();

            return savedSchedule;
        } catch (DataAccessException e) {
            log.error("Erro ao salvar escala no banco de dados: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao criar escala devido a erro no banco de dados");
        }
    }

    @Transactional
    public Schedule addCrewMember(Long scheduleId, String discordId, String username, String nickname) {
        try {
            if (scheduleId == null || discordId == null || username == null || nickname == null) {
                throw new IllegalArgumentException("Parâmetros não podem ser nulos");
            }

            Schedule schedule = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new IllegalArgumentException("Escala não encontrada"));

            if (!schedule.isActive()) {
                throw new IllegalStateException("Esta escala já foi encerrada");
            }

            List<User> crewMembers = schedule.getCrewMembers();
            if (crewMembers == null) {
                schedule.setCrewMembers(new ArrayList<>());
                crewMembers = schedule.getCrewMembers();
            }

            boolean alreadyInCrew = crewMembers.stream()
                    .anyMatch(user -> discordId.equals(user.getDiscordId()));

            if (alreadyInCrew) {
                throw new IllegalStateException("Você já está embarcado nesta aeronave");
            }

            User user = userService.getOrCreateUser(discordId, username, nickname);
            schedule.addCrewMember(user);

            Schedule updatedSchedule = scheduleRepository.save(schedule);
            log.info("Tripulante {} embarcou na escala {} (ID: {})", nickname, schedule.getTitle(), scheduleId);

            logManager.createScheduleLog(updatedSchedule, "JOINED", discordId, nickname,
                    "Tripulante embarcou na aeronave");
            logManager.updateScheduleLogMessage(updatedSchedule, nickname + " embarcou na aeronave").subscribe();

            return updatedSchedule;
        } catch (DataAccessException e) {
            log.error("Erro ao adicionar tripulante na escala {}: {}", scheduleId, e.getMessage(), e);
            throw new RuntimeException("Falha ao adicionar tripulante devido a erro no banco de dados");
        }
    }

    @Transactional
    public Schedule addCrewMemberAndInitialize(Long scheduleId, String discordId, String username, String nickname) {
        Schedule schedule = addCrewMember(scheduleId, discordId, username, nickname);
        schedule.initializeCrewMembers();
        return schedule;
    }

    @Transactional
    public Schedule removeCrewMember(Long scheduleId, String discordId, String nickname) {
        try {
            if (scheduleId == null || discordId == null || nickname == null) {
                throw new IllegalArgumentException("Parâmetros não podem ser nulos");
            }

            Schedule schedule = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new IllegalArgumentException("Escala não encontrada"));

            if (!schedule.isActive()) {
                throw new IllegalStateException("Esta escala já foi encerrada");
            }

            List<User> crewMembers = schedule.getCrewMembers();
            if (crewMembers == null) {
                throw new IllegalStateException("Nenhum tripulante encontrado na escala");
            }

            User user = crewMembers.stream()
                    .filter(u -> discordId.equals(u.getDiscordId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Você não está embarcado nesta aeronave"));

            schedule.removeCrewMember(user);

            Schedule updatedSchedule = scheduleRepository.save(schedule);
            log.info("Tripulante {} desembarcou da escala {} (ID: {})", nickname, schedule.getTitle(), scheduleId);

            logManager.createScheduleLog(updatedSchedule, "LEFT", discordId, nickname,
                    "Tripulante desembarcou da aeronave");
            logManager.updateScheduleLogMessage(updatedSchedule, nickname + " desembarcou da aeronave").subscribe();

            return updatedSchedule;
        } catch (DataAccessException e) {
            log.error("Erro ao remover tripulante da escala {}: {}", scheduleId, e.getMessage(), e);
            throw new RuntimeException("Falha ao remover tripulante devido a erro no banco de dados");
        }
    }

    @Transactional
    public Schedule removeCrewMemberAndInitialize(Long scheduleId, String discordId, String nickname) {
        Schedule schedule = removeCrewMember(scheduleId, discordId, nickname);
        schedule.initializeCrewMembers();
        return schedule;
    }

    @Transactional
    public Schedule closeSchedule(Long scheduleId, String discordId, String nickname) {
        try {
            if (scheduleId == null || discordId == null || nickname == null) {
                throw new IllegalArgumentException("Parâmetros não podem ser nulos");
            }

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
            List<User> crewMembers = updatedSchedule.getCrewMembers();
            if (crewMembers != null) {
                for (User user : crewMembers) {
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

            log.info("Escala {} (ID: {}) e seus logs foram finalizados", schedule.getTitle(), scheduleId);

            return updatedSchedule;
        } catch (DataAccessException e) {
            log.error("Erro ao encerrar escala {}: {}", scheduleId, e.getMessage(), e);
            throw new RuntimeException("Falha ao encerrar escala devido a erro no banco de dados");
        }
    }

    @Transactional
    public Schedule closeScheduleAndInitialize(Long scheduleId, String discordId, String nickname) {
        Schedule schedule = closeSchedule(scheduleId, discordId, nickname);
        schedule.initializeCrewMembers();
        return schedule;
    }

    @Transactional(readOnly = true)
    public List<Schedule> getActiveSchedules() {
        try {
            return scheduleRepository.findByActiveTrue();
        } catch (DataAccessException e) {
            log.error("Erro ao buscar escalas ativas: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public String generateGraTitle() {
        int activeCount = getActiveSchedules().size() + 1;
        return "G.R.A - " + activeCount;
    }
}