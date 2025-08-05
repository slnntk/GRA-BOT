import { describe, it, expect, beforeEach } from '@jest/globals';
import Schedule from '../../src/domain/entities/Schedule.js';
import User from '../../src/domain/entities/User.js';
import AircraftType from '../../src/domain/enums/AircraftType.js';
import MissionType from '../../src/domain/enums/MissionType.js';
import ActionSubType from '../../src/domain/enums/ActionSubType.js';

describe('Schedule Entity', () => {
  let validScheduleParams;
  let user;

  beforeEach(() => {
    validScheduleParams = {
      guildId: '987654321',
      title: 'Test Mission',
      aircraftType: AircraftType.F18,
      missionType: MissionType.CAS,
      createdById: '123456789',
      createdByUsername: 'TestPilot'
    };

    user = new User('456789123', 'CrewMember', 'Crew Member');
  });

  describe('Constructor', () => {
    it('should create schedule with valid parameters', () => {
      const schedule = new Schedule(validScheduleParams);
      
      expect(schedule.guildId).toBe(validScheduleParams.guildId);
      expect(schedule.title).toBe(validScheduleParams.title);
      expect(schedule.aircraftType).toBe(validScheduleParams.aircraftType);
      expect(schedule.missionType).toBe(validScheduleParams.missionType);
      expect(schedule.createdById).toBe(validScheduleParams.createdById);
      expect(schedule.createdByUsername).toBe(validScheduleParams.createdByUsername);
      expect(schedule.active).toBe(true);
      expect(schedule.crewMembers).toEqual([]);
      expect(schedule.startTime).toBeInstanceOf(Date);
      expect(schedule.endTime).toBeNull();
    });

    it('should trim whitespace from string parameters', () => {
      const paramsWithSpaces = {
        ...validScheduleParams,
        guildId: '  987654321  ',
        title: '  Test Mission  ',
        createdById: '  123456789  ',
        createdByUsername: '  TestPilot  '
      };
      
      const schedule = new Schedule(paramsWithSpaces);
      expect(schedule.guildId).toBe('987654321');
      expect(schedule.title).toBe('Test Mission');
      expect(schedule.createdById).toBe('123456789');
      expect(schedule.createdByUsername).toBe('TestPilot');
    });

    it('should throw error for missing required fields', () => {
      const requiredFields = ['guildId', 'title', 'aircraftType', 'missionType', 'createdById', 'createdByUsername'];
      
      requiredFields.forEach(field => {
        const invalidParams = { ...validScheduleParams };
        delete invalidParams[field];
        
        expect(() => new Schedule(invalidParams))
          .toThrow();
      });
    });

    it('should throw error for invalid aircraft type', () => {
      const invalidParams = {
        ...validScheduleParams,
        aircraftType: 'INVALID_AIRCRAFT'
      };
      
      expect(() => new Schedule(invalidParams))
        .toThrow('Invalid aircraft type: INVALID_AIRCRAFT');
    });

    it('should throw error for invalid mission type', () => {
      const invalidParams = {
        ...validScheduleParams,
        missionType: 'INVALID_MISSION'
      };
      
      expect(() => new Schedule(invalidParams))
        .toThrow('Invalid mission type: INVALID_MISSION');
    });

    it('should require description for OUTROS missions', () => {
      const outrosParams = {
        ...validScheduleParams,
        missionType: MissionType.OUTROS
      };
      
      expect(() => new Schedule(outrosParams))
        .toThrow('Description is required for OUTROS missions');
      
      // Should work with description
      const validOutrosParams = {
        ...outrosParams,
        outrosDescription: 'Custom mission description'
      };
      
      const schedule = new Schedule(validOutrosParams);
      expect(schedule.outrosDescription).toBe('Custom mission description');
    });

    it('should handle ACTION mission with action subtype', () => {
      const actionParams = {
        ...validScheduleParams,
        missionType: MissionType.ACTION,
        actionSubType: ActionSubType.COMBAT,
        actionOption: 'Combat operation'
      };
      
      const schedule = new Schedule(actionParams);
      expect(schedule.actionSubType).toBe(ActionSubType.COMBAT);
      expect(schedule.actionOption).toBe('Combat operation');
    });
  });

  describe('Creator Management', () => {
    let schedule;

    beforeEach(() => {
      schedule = new Schedule(validScheduleParams);
    });

    it('should identify creator correctly', () => {
      expect(schedule.isCreator(validScheduleParams.createdById)).toBe(true);
      expect(schedule.isCreator('different_id')).toBe(false);
    });
  });

  describe('Crew Management', () => {
    let schedule;

    beforeEach(() => {
      schedule = new Schedule(validScheduleParams);
    });

    it('should add crew member successfully', () => {
      schedule.addCrewMember(user);
      
      expect(schedule.hasCrewMember(user.discordId)).toBe(true);
      expect(schedule.crewMembers).toHaveLength(1);
      expect(schedule.crewMembers[0].discordId).toBe(user.discordId);
    });

    it('should prevent adding creator as crew member', () => {
      const creator = new User(
        validScheduleParams.createdById,
        validScheduleParams.createdByUsername,
        'Creator Nickname'
      );
      
      expect(() => schedule.addCrewMember(creator))
        .toThrow('Creator cannot be added as crew member');
    });

    it('should prevent adding duplicate crew members', () => {
      schedule.addCrewMember(user);
      
      expect(() => schedule.addCrewMember(user))
        .toThrow('User is already in crew');
    });

    it('should remove crew member successfully', () => {
      schedule.addCrewMember(user);
      schedule.removeCrewMember(user.discordId);
      
      expect(schedule.hasCrewMember(user.discordId)).toBe(false);
      expect(schedule.crewMembers).toHaveLength(0);
    });

    it('should prevent removing creator', () => {
      expect(() => schedule.removeCrewMember(validScheduleParams.createdById))
        .toThrow('Creator cannot leave schedule');
    });

    it('should throw error when removing non-existent crew member', () => {
      expect(() => schedule.removeCrewMember('non_existent_id'))
        .toThrow('User is not in crew');
    });

    it('should prevent modifications when schedule is inactive', () => {
      schedule.close('some_id');
      
      expect(() => schedule.addCrewMember(user))
        .toThrow('Cannot modify inactive schedule');
      
      expect(() => schedule.removeCrewMember(user.discordId))
        .toThrow('Cannot modify inactive schedule');
    });
  });

  describe('Schedule Closure', () => {
    let schedule;

    beforeEach(() => {
      schedule = new Schedule(validScheduleParams);
    });

    it('should close schedule successfully', () => {
      const beforeClose = new Date();
      schedule.close('some_id');
      const afterClose = new Date();
      
      expect(schedule.active).toBe(false);
      expect(schedule.endTime).toBeInstanceOf(Date);
      expect(schedule.endTime.getTime()).toBeGreaterThanOrEqual(beforeClose.getTime());
      expect(schedule.endTime.getTime()).toBeLessThanOrEqual(afterClose.getTime());
    });

    it('should prevent closing already closed schedule', () => {
      schedule.close('some_id');
      
      expect(() => schedule.close('another_id'))
        .toThrow('Schedule is already closed');
    });
  });

  describe('Serialization', () => {
    let schedule;

    beforeEach(() => {
      schedule = new Schedule(validScheduleParams);
      schedule.setId(123);
      schedule.addCrewMember(user);
    });

    it('should convert to JSON correctly', () => {
      const json = schedule.toJSON();
      
      expect(json.id).toBe(123);
      expect(json.guildId).toBe(validScheduleParams.guildId);
      expect(json.title).toBe(validScheduleParams.title);
      expect(json.aircraftType).toBe(validScheduleParams.aircraftType);
      expect(json.missionType).toBe(validScheduleParams.missionType);
      expect(json.active).toBe(true);
      expect(json.crewMembers).toHaveLength(1);
      expect(json.startTime).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/);
      expect(json.endTime).toBeNull();
    });

    it('should create from JSON correctly', () => {
      const originalJson = schedule.toJSON();
      const recreatedSchedule = Schedule.fromJSON(originalJson);
      
      expect(recreatedSchedule.id).toBe(schedule.id);
      expect(recreatedSchedule.guildId).toBe(schedule.guildId);
      expect(recreatedSchedule.title).toBe(schedule.title);
      expect(recreatedSchedule.aircraftType).toBe(schedule.aircraftType);
      expect(recreatedSchedule.missionType).toBe(schedule.missionType);
      expect(recreatedSchedule.active).toBe(schedule.active);
      expect(recreatedSchedule.crewMembers).toEqual(schedule.crewMembers);
    });
  });

  describe('ID Management', () => {
    let schedule;

    beforeEach(() => {
      schedule = new Schedule(validScheduleParams);
    });

    it('should set ID successfully', () => {
      schedule.setId(456);
      expect(schedule.id).toBe(456);
    });

    it('should prevent changing ID once set', () => {
      schedule.setId(456);
      
      expect(() => schedule.setId(789))
        .toThrow('Schedule ID cannot be changed once set');
    });

    it('should validate ID is integer', () => {
      expect(() => schedule.setId('not_a_number'))
        .toThrow('Schedule ID must be an integer');
    });
  });
});