package com.gra.paradise.botattendance.service;

import com.gra.paradise.botattendance.exception.*;
import com.gra.paradise.botattendance.model.*;
import com.gra.paradise.botattendance.repository.ScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleManager {

    private final ScheduleRepository scheduleRepository;
    private final UserService userService;
    private final ScheduleLogManager logManager;
    private final DiscordService discordService;

    private Schedule validateScheduleForModification(String guildId, Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .map(schedule -> {
                    if (schedule.getGuildId() == null) {
                        schedule.setGuildId(guildId.trim());
                    }
                    if (!schedule.isActive()) {
                        throw new ScheduleAlreadyClosedException();
                    }
                    return schedule;
                })
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
    }

    @Transactional
    public Schedule createSchedule(String guildId, String title, AircraftType aircraftType, MissionType missionType,
                                   String creatorId, String creatorNickname, ActionSubType actionSubType,
                                   String actionOption) {
        if (guildId == null || title == null || aircraftType == null || missionType == null ||
                creatorId == null || creatorNickname == null) {
            throw new MissingRequiredParametersException("Parâmetros obrigatórios não fornecidos.");
        }
        if (missionType == MissionType.OTHER && (actionOption == null || actionOption.trim().isEmpty())) {
            throw new MissingRequiredParametersException("O campo de descrição é obrigatório para missões do tipo 'Outros'.");
        }

        Schedule schedule = new Schedule();
        schedule.setGuildId(guildId.trim());
        schedule.setTitle(title.trim());
        schedule.setAircraftType(aircraftType);
        schedule.setMissionType(missionType);
        schedule.setActionSubType(missionType == MissionType.OTHER ? null : actionSubType); // Não usar actionSubType para OUTROS
        schedule.setActionOption(actionOption != null ? actionOption.trim() : null);
        schedule.setStartTime(Instant.now());
        schedule.setCreatedById(creatorId.trim());
        schedule.setCreatedByUsername(creatorNickname.trim());
        schedule.setActive(true);
        schedule.setCrewMembers(new ArrayList<>());

        Schedule saved = scheduleRepository.save(schedule);
        log.info("Escala criada: {} (ID: {})", saved.getTitle(), saved.getId());
        logManager.sendScheduleCreationLog(guildId, saved).block();
        return saved;
    }

    @Transactional
    public Schedule addCrewMember(String guildId, Long scheduleId, String discordId, String username, String nickname) {
        Schedule schedule = validateScheduleForModification(guildId, scheduleId);

        if (discordId.equals(schedule.getCreatedById())) {
            throw new PilotCannotBeCrewException();
        }

        List<User> crew = Optional.ofNullable(schedule.getCrewMembers()).orElseGet(ArrayList::new);
        if (crew.stream().anyMatch(u -> discordId.equals(u.getDiscordId()))) {
            throw new UserAlreadyBoardedException();
        }

        User user = userService.getOrCreateUser(discordId.trim(), username.trim(), nickname.trim());
        crew.add(user);
        schedule.setCrewMembers(crew);

        Schedule saved = scheduleRepository.save(schedule);
        logManager.createScheduleLog(saved, "EMBARKED", discordId, nickname, " embarcou.");
        logManager.updateScheduleLogMessage(guildId, saved, " embarcou.").block();
        return saved;
    }

    @Transactional
    public Schedule removeCrewMember(String guildId, Long scheduleId, String discordId, String nickname) {
        Schedule schedule = validateScheduleForModification(guildId, scheduleId);

        if (discordId.equals(schedule.getCreatedById())) {
            throw new CreatorCannotLeaveException();
        }

        List<User> crew = Optional.ofNullable(schedule.getCrewMembers()).orElseGet(ArrayList::new);
        User user = crew.stream()
                .filter(u -> discordId.equals(u.getDiscordId()))
                .findFirst()
                .orElseThrow(UserNotBoardedException::new);

        crew.remove(user);
        schedule.setCrewMembers(crew);

        Schedule saved = scheduleRepository.save(schedule);
        logManager.createScheduleLog(saved, "DISEMBARKED", discordId, nickname, " desembarcou.");
        logManager.updateScheduleLogMessage(guildId, saved, " desembarcou.").block();
        return saved;
    }

    @Transactional
    public Schedule closeSchedule(String guildId, Long scheduleId, String discordId, String nickname) {
        Schedule schedule = validateScheduleForModification(guildId, scheduleId);

        boolean isCreator = discordId.equals(schedule.getCreatedById());
        boolean hasRequiredRole = checkUserHasRole(guildId, discordId, "1393974475321507953");

        if (!isCreator && !hasRequiredRole) {
            throw new OnlyCreatorCanCloseScheduleException();
        }

        Instant endTime = Instant.now();
        schedule.setActive(false);
        schedule.setEndTime(endTime);
        schedule.setCrewMembers(Optional.ofNullable(schedule.getCrewMembers()).orElseGet(ArrayList::new));

        Schedule saved = scheduleRepository.save(schedule);
        logManager.createScheduleLog(saved, "CLOSED", discordId, nickname, " encerrou a escala.");
        logManager.createFinalScheduleLogMessage(
                guildId,
                scheduleId,
                saved.getTitle(),
                saved.getAircraftType(),
                saved.getMissionType(),
                saved.getActionSubType(),
                saved.getActionOption(),
                saved.getStartTime(),
                endTime,
                saved.getCreatedByUsername(),
                nickname,
                saved.getCrewMembers().stream().map(User::getNickname).toList()
        ).block();
        return saved;
    }

    private boolean checkUserHasRole(String guildId, String discordId, String roleId) {
        return discordService.checkUserHasRole(guildId, discordId, roleId);
    }

    @Transactional
    public Schedule save(Schedule schedule) {
        return scheduleRepository.save(schedule);
    }

    @Transactional
    public List<Schedule> getActiveSchedules(String guildId) {
        if (guildId == null || guildId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Optional.ofNullable(scheduleRepository.findByActiveTrueAndGuildId(guildId))
                .orElse(Collections.emptyList());
    }

    @Transactional
    public String generateNextGraTitle(String guildId) {
        if (guildId == null || guildId.trim().isEmpty()) {
            return "G.R.A - 1";
        }
        int activeCount = getActiveSchedules(guildId).size() + 1;
        return "G.R.A - " + activeCount;
    }

    public Optional<Schedule> findByIdAndGuildId(Long scheduleId, String guildId) {
        return scheduleRepository.findByIdAndGuildId(scheduleId, guildId);
    }

    public Schedule findByIdAndGuildIdWithCrew(Long scheduleId, String guildId) {
        return scheduleRepository.findByIdAndGuildIdWithCrew(scheduleId, guildId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
    }

    public List<String> getOtherMissionSuggestions() {
        return OtherMissionSuggestions.getSuggestions();
    }
}