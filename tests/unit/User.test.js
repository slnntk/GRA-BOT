import { describe, it, expect, beforeEach } from '@jest/globals';
import User from '../../src/domain/entities/User.js';

describe('User Entity', () => {
  let user;
  const validDiscordId = '123456789';
  const validUsername = 'testuser';
  const validNickname = 'Test User';

  beforeEach(() => {
    user = new User(validDiscordId, validUsername, validNickname);
  });

  describe('Constructor', () => {
    it('should create a user with valid parameters', () => {
      expect(user.discordId).toBe(validDiscordId);
      expect(user.username).toBe(validUsername);
      expect(user.nickname).toBe(validNickname);
      expect(user.schedules).toEqual([]);
    });

    it('should trim whitespace from parameters', () => {
      const userWithSpaces = new User('  123  ', '  username  ', '  nickname  ');
      expect(userWithSpaces.discordId).toBe('123');
      expect(userWithSpaces.username).toBe('username');
      expect(userWithSpaces.nickname).toBe('nickname');
    });

    it('should throw error for missing discordId', () => {
      expect(() => new User('', validUsername, validNickname))
        .toThrow('Discord ID is required and must be a string');
    });

    it('should throw error for missing username', () => {
      expect(() => new User(validDiscordId, '', validNickname))
        .toThrow('Username is required and must be a string');
    });

    it('should throw error for missing nickname', () => {
      expect(() => new User(validDiscordId, validUsername, ''))
        .toThrow('Nickname is required and must be a string');
    });

    it('should throw error for non-string parameters', () => {
      expect(() => new User(123, validUsername, validNickname))
        .toThrow('Discord ID is required and must be a string');
      
      expect(() => new User(validDiscordId, 123, validNickname))
        .toThrow('Username is required and must be a string');
      
      expect(() => new User(validDiscordId, validUsername, 123))
        .toThrow('Nickname is required and must be a string');
    });
  });

  describe('Getters', () => {
    it('should return immutable schedules array', () => {
      const schedules = user.schedules;
      schedules.push(1); // Try to mutate
      expect(user.schedules).toEqual([]); // Original should be unchanged
    });
  });

  describe('updateNickname', () => {
    it('should update nickname with valid string', () => {
      const newNickname = 'New Nickname';
      user.updateNickname(newNickname);
      expect(user.nickname).toBe(newNickname);
    });

    it('should trim whitespace from new nickname', () => {
      user.updateNickname('  New Nickname  ');
      expect(user.nickname).toBe('New Nickname');
    });

    it('should throw error for empty nickname', () => {
      expect(() => user.updateNickname(''))
        .toThrow('Nickname must be a non-empty string');
    });

    it('should throw error for non-string nickname', () => {
      expect(() => user.updateNickname(123))
        .toThrow('Nickname must be a non-empty string');
    });
  });

  describe('Schedule Management', () => {
    it('should add schedule ID', () => {
      user.addSchedule(1);
      expect(user.schedules).toContain(1);
    });

    it('should not add duplicate schedule ID', () => {
      user.addSchedule(1);
      user.addSchedule(1);
      expect(user.schedules).toEqual([1]);
    });

    it('should throw error for non-integer schedule ID', () => {
      expect(() => user.addSchedule('1'))
        .toThrow('Schedule ID must be an integer');
    });

    it('should remove schedule ID', () => {
      user.addSchedule(1);
      user.addSchedule(2);
      user.removeSchedule(1);
      expect(user.schedules).toEqual([2]);
    });

    it('should handle removing non-existent schedule ID gracefully', () => {
      user.removeSchedule(999);
      expect(user.schedules).toEqual([]);
    });
  });

  describe('Serialization', () => {
    it('should convert to JSON correctly', () => {
      user.addSchedule(1);
      user.addSchedule(2);
      
      const json = user.toJSON();
      expect(json).toEqual({
        discordId: validDiscordId,
        username: validUsername,
        nickname: validNickname,
        schedules: [1, 2]
      });
    });

    it('should create from JSON correctly', () => {
      const userData = {
        discordId: validDiscordId,
        username: validUsername,
        nickname: validNickname,
        schedules: [1, 2]
      };
      
      const recreatedUser = User.fromJSON(userData);
      expect(recreatedUser.discordId).toBe(validDiscordId);
      expect(recreatedUser.username).toBe(validUsername);
      expect(recreatedUser.nickname).toBe(validNickname);
      expect(recreatedUser.schedules).toEqual([1, 2]);
    });

    it('should handle JSON without schedules array', () => {
      const userData = {
        discordId: validDiscordId,
        username: validUsername,
        nickname: validNickname
      };
      
      const recreatedUser = User.fromJSON(userData);
      expect(recreatedUser.schedules).toEqual([]);
    });
  });
});