import Schedule from '../domain/entities/Schedule.js';
import MissionType from '../domain/enums/MissionType.js';

/**
 * Use case for managing schedules
 * Follows Single Responsibility Principle - handles schedule business logic
 * Follows Dependency Inversion Principle - depends on abstractions
 */
class ScheduleUseCase {
  /**
   * Creates a new ScheduleUseCase instance
   * @param {IScheduleRepository} scheduleRepository - Schedule repository implementation
   * @param {UserUseCase} userUseCase - User use case for user operations
   */
  constructor(scheduleRepository, userUseCase) {
    if (!scheduleRepository) {
      throw new Error('Schedule repository is required');
    }
    if (!userUseCase) {
      throw new Error('User use case is required');
    }
    
    this._scheduleRepository = scheduleRepository;
    this._userUseCase = userUseCase;
  }

  /**
   * Creates a new schedule
   * Pure function that validates inputs and creates schedule entity
   * @param {Object} params - Schedule creation parameters
   * @returns {Promise<Schedule>} Created schedule
   */
  async createSchedule({
    guildId,
    title,
    aircraftType,
    missionType,
    creatorId,
    creatorNickname,
    actionSubType = null,
    actionOption = null
  }) {
    try {
      // Validation for OUTROS mission type
      if (missionType === MissionType.OUTROS && (!actionOption || actionOption.trim() === '')) {
        throw new Error('Description is required for OUTROS missions');
      }

      // Create schedule entity (validation happens in constructor)
      const schedule = new Schedule({
        guildId,
        title,
        aircraftType,
        missionType,
        createdById: creatorId,
        createdByUsername: creatorNickname,
        actionSubType,
        actionOption: missionType === MissionType.ACTION ? actionOption : null,
        outrosDescription: missionType === MissionType.OUTROS ? actionOption : null
      });

      // Save schedule
      const savedSchedule = await this._scheduleRepository.save(schedule);
      
      return savedSchedule;
    } catch (error) {
      throw new Error(`Failed to create schedule: ${error.message}`);
    }
  }

  /**
   * Adds a crew member to a schedule
   * @param {Object} params - Parameters for adding crew member
   * @returns {Promise<Schedule>} Updated schedule
   */
  async addCrewMember({ guildId, scheduleId, discordId, username, nickname }) {
    try {
      // Find and validate schedule
      const schedule = await this._findAndValidateSchedule(scheduleId, guildId);

      // Get or create user
      const user = await this._userUseCase.getOrCreateUser(discordId, username, nickname);

      // Add crew member (business logic in entity)
      schedule.addCrewMember(user);

      // Save updated schedule
      return await this._scheduleRepository.save(schedule);
    } catch (error) {
      throw new Error(`Failed to add crew member: ${error.message}`);
    }
  }

  /**
   * Removes a crew member from a schedule
   * @param {Object} params - Parameters for removing crew member
   * @returns {Promise<Schedule>} Updated schedule
   */
  async removeCrewMember({ guildId, scheduleId, discordId }) {
    try {
      // Find and validate schedule
      const schedule = await this._findAndValidateSchedule(scheduleId, guildId);

      // Remove crew member (business logic in entity)
      schedule.removeCrewMember(discordId);

      // Save updated schedule
      return await this._scheduleRepository.save(schedule);
    } catch (error) {
      throw new Error(`Failed to remove crew member: ${error.message}`);
    }
  }

  /**
   * Closes a schedule
   * @param {Object} params - Parameters for closing schedule
   * @returns {Promise<Schedule>} Closed schedule
   */
  async closeSchedule({ guildId, scheduleId, closedById }) {
    try {
      // Find and validate schedule
      const schedule = await this._findAndValidateSchedule(scheduleId, guildId);

      // Validate permissions (basic check - creator can always close)
      if (!schedule.isCreator(closedById)) {
        // Additional role-based permission checks could be added here
        // For now, we'll allow any user to close (business requirement)
      }

      // Close schedule (business logic in entity)
      schedule.close(closedById);

      // Save updated schedule
      return await this._scheduleRepository.save(schedule);
    } catch (error) {
      throw new Error(`Failed to close schedule: ${error.message}`);
    }
  }

  /**
   * Gets active schedules for a guild
   * @param {string} guildId - Discord guild ID
   * @returns {Promise<Schedule[]>} Array of active schedules
   */
  async getActiveSchedules(guildId) {
    try {
      if (!guildId) {
        throw new Error('Guild ID is required');
      }

      return await this._scheduleRepository.findActiveByGuildId(guildId);
    } catch (error) {
      throw new Error(`Failed to get active schedules: ${error.message}`);
    }
  }

  /**
   * Generates next GRA title based on active schedules
   * Pure function that calculates title
   * @param {string} guildId - Discord guild ID
   * @returns {Promise<string>} Generated title
   */
  async generateNextGraTitle(guildId) {
    try {
      if (!guildId) {
        return 'G.R.A - 1';
      }

      const activeSchedules = await this.getActiveSchedules(guildId);
      const nextNumber = activeSchedules.length + 1;
      return `G.R.A - ${nextNumber}`;
    } catch (error) {
      throw new Error(`Failed to generate GRA title: ${error.message}`);
    }
  }

  /**
   * Finds a schedule by ID and guild ID
   * @param {number} scheduleId - Schedule ID
   * @param {string} guildId - Discord guild ID
   * @returns {Promise<Schedule|null>} Schedule or null if not found
   */
  async findSchedule(scheduleId, guildId) {
    try {
      if (!scheduleId || !guildId) {
        throw new Error('Schedule ID and Guild ID are required');
      }

      return await this._scheduleRepository.findByIdAndGuildId(scheduleId, guildId);
    } catch (error) {
      throw new Error(`Failed to find schedule: ${error.message}`);
    }
  }

  /**
   * Cleans up old schedules (maintenance task)
   * @param {number} daysThreshold - Days threshold for cleanup (default: 30)
   * @returns {Promise<number>} Number of cleaned schedules
   */
  async cleanupOldSchedules(daysThreshold = 30) {
    try {
      const thresholdDate = new Date();
      thresholdDate.setDate(thresholdDate.getDate() - daysThreshold);

      const oldSchedules = await this._scheduleRepository.findByEndTimeBefore(thresholdDate);
      
      let cleanedCount = 0;
      for (const schedule of oldSchedules) {
        await this._scheduleRepository.delete(schedule.id);
        cleanedCount++;
      }

      return cleanedCount;
    } catch (error) {
      throw new Error(`Failed to cleanup old schedules: ${error.message}`);
    }
  }

  /**
   * Private helper method to find and validate schedule
   * @private
   * @param {number} scheduleId - Schedule ID
   * @param {string} guildId - Discord guild ID
   * @returns {Promise<Schedule>} Validated schedule
   */
  async _findAndValidateSchedule(scheduleId, guildId) {
    const schedule = await this._scheduleRepository.findByIdAndGuildId(scheduleId, guildId);
    
    if (!schedule) {
      throw new Error(`Schedule with ID ${scheduleId} not found`);
    }

    if (!schedule.active) {
      throw new Error('Schedule is already closed');
    }

    return schedule;
  }
}

export default ScheduleUseCase;