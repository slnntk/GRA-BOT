import { PrismaClient } from '@prisma/client';
import IScheduleRepository from '../../domain/repositories/IScheduleRepository.js';
import Schedule from '../../domain/entities/Schedule.js';
import { DatabaseError } from '../../utils/errors.js';

/**
 * Schedule repository implementation using Prisma
 * Follows Dependency Inversion Principle - implements IScheduleRepository interface
 */
class PrismaScheduleRepository extends IScheduleRepository {
  /**
   * Creates a new PrismaScheduleRepository instance
   * @param {PrismaClient} prisma - Prisma client instance
   */
  constructor(prisma) {
    super();
    if (!prisma) {
      throw new Error('Prisma client is required');
    }
    this._prisma = prisma;
  }

  /**
   * Finds a schedule by ID
   * @param {number} id - Schedule ID
   * @returns {Promise<Schedule|null>} Schedule instance or null if not found
   */
  async findById(id) {
    try {
      const scheduleData = await this._prisma.schedule.findUnique({
        where: { id },
        include: {
          crewMembers: true
        }
      });

      if (!scheduleData) {
        return null;
      }

      return this._mapToEntity(scheduleData);
    } catch (error) {
      throw new DatabaseError(`Failed to find schedule by ID: ${error.message}`);
    }
  }

  /**
   * Finds a schedule by ID and guild ID
   * @param {number} id - Schedule ID
   * @param {string} guildId - Discord guild ID
   * @returns {Promise<Schedule|null>} Schedule instance or null if not found
   */
  async findByIdAndGuildId(id, guildId) {
    try {
      const scheduleData = await this._prisma.schedule.findFirst({
        where: { 
          id,
          guildId
        },
        include: {
          crewMembers: true
        }
      });

      if (!scheduleData) {
        return null;
      }

      return this._mapToEntity(scheduleData);
    } catch (error) {
      throw new DatabaseError(`Failed to find schedule by ID and guild ID: ${error.message}`);
    }
  }

  /**
   * Finds active schedules for a guild
   * @param {string} guildId - Discord guild ID
   * @returns {Promise<Schedule[]>} Array of active schedules
   */
  async findActiveByGuildId(guildId) {
    try {
      const schedulesData = await this._prisma.schedule.findMany({
        where: {
          guildId,
          active: true
        },
        include: {
          crewMembers: true
        },
        orderBy: {
          startTime: 'desc'
        }
      });

      return schedulesData.map(scheduleData => this._mapToEntity(scheduleData));
    } catch (error) {
      throw new DatabaseError(`Failed to find active schedules: ${error.message}`);
    }
  }

  /**
   * Finds schedules that ended before a specific date
   * @param {Date} endTime - End time threshold
   * @returns {Promise<Schedule[]>} Array of old schedules
   */
  async findByEndTimeBefore(endTime) {
    try {
      const schedulesData = await this._prisma.schedule.findMany({
        where: {
          endTime: {
            lt: endTime
          }
        },
        include: {
          crewMembers: true
        }
      });

      return schedulesData.map(scheduleData => this._mapToEntity(scheduleData));
    } catch (error) {
      throw new DatabaseError(`Failed to find schedules by end time: ${error.message}`);
    }
  }

  /**
   * Saves a schedule (create or update)
   * @param {Schedule} schedule - Schedule instance to save
   * @returns {Promise<Schedule>} Saved schedule instance
   */
  async save(schedule) {
    try {
      const scheduleData = this._mapFromEntity(schedule);
      let savedScheduleData;

      if (schedule.id) {
        // Update existing schedule
        savedScheduleData = await this._prisma.schedule.update({
          where: { id: schedule.id },
          data: {
            ...scheduleData,
            crewMembers: {
              set: schedule.crewMembers.map(member => ({ discordId: member.discordId }))
            }
          },
          include: {
            crewMembers: true
          }
        });
      } else {
        // Create new schedule
        savedScheduleData = await this._prisma.schedule.create({
          data: {
            ...scheduleData,
            crewMembers: {
              connect: schedule.crewMembers.map(member => ({ discordId: member.discordId }))
            }
          },
          include: {
            crewMembers: true
          }
        });
      }

      return this._mapToEntity(savedScheduleData);
    } catch (error) {
      throw new DatabaseError(`Failed to save schedule: ${error.message}`);
    }
  }

  /**
   * Deletes a schedule by ID
   * @param {number} id - Schedule ID
   * @returns {Promise<boolean>} True if deleted, false if not found
   */
  async delete(id) {
    try {
      const result = await this._prisma.schedule.delete({
        where: { id }
      });
      return !!result;
    } catch (error) {
      if (error.code === 'P2025') { // Record not found
        return false;
      }
      throw new DatabaseError(`Failed to delete schedule: ${error.message}`);
    }
  }

  /**
   * Counts active schedules for a guild
   * @param {string} guildId - Discord guild ID
   * @returns {Promise<number>} Number of active schedules
   */
  async countActiveByGuildId(guildId) {
    try {
      return await this._prisma.schedule.count({
        where: {
          guildId,
          active: true
        }
      });
    } catch (error) {
      throw new DatabaseError(`Failed to count active schedules: ${error.message}`);
    }
  }

  /**
   * Maps Prisma schedule data to Schedule entity
   * @private
   * @param {Object} scheduleData - Prisma schedule data
   * @returns {Schedule} Schedule entity instance
   */
  _mapToEntity(scheduleData) {
    const schedule = Schedule.fromJSON({
      id: scheduleData.id,
      guildId: scheduleData.guildId,
      title: scheduleData.title,
      aircraftType: scheduleData.aircraftType,
      missionType: scheduleData.missionType,
      actionSubType: scheduleData.actionSubType,
      actionOption: scheduleData.actionOption,
      outrosDescription: scheduleData.outrosDescription,
      startTime: scheduleData.startTime.toISOString(),
      endTime: scheduleData.endTime?.toISOString() || null,
      createdById: scheduleData.createdById,
      createdByUsername: scheduleData.createdByUsername,
      active: scheduleData.active,
      crewMembers: scheduleData.crewMembers?.map(member => ({
        discordId: member.discordId,
        username: member.username,
        nickname: member.nickname
      })) || []
    });

    return schedule;
  }

  /**
   * Maps Schedule entity to Prisma data format
   * @private
   * @param {Schedule} schedule - Schedule entity
   * @returns {Object} Prisma data format
   */
  _mapFromEntity(schedule) {
    return {
      guildId: schedule.guildId,
      title: schedule.title,
      aircraftType: schedule.aircraftType,
      missionType: schedule.missionType,
      actionSubType: schedule.actionSubType,
      actionOption: schedule.actionOption,
      outrosDescription: schedule.outrosDescription,
      startTime: schedule.startTime,
      endTime: schedule.endTime,
      createdById: schedule.createdById,
      createdByUsername: schedule.createdByUsername,
      active: schedule.active
    };
  }
}

export default PrismaScheduleRepository;