import IUserRepository from '../../domain/repositories/IUserRepository.js';
import User from '../../domain/entities/User.js';
import { DatabaseError } from '../../utils/errors.js';

/**
 * User repository implementation using Prisma
 * Follows Dependency Inversion Principle - implements IUserRepository interface
 */
class PrismaUserRepository extends IUserRepository {
  /**
   * Creates a new PrismaUserRepository instance
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
   * Finds a user by Discord ID
   * @param {string} discordId - Discord user ID
   * @returns {Promise<User|null>} User instance or null if not found
   */
  async findById(discordId) {
    try {
      const userData = await this._prisma.user.findUnique({
        where: { discordId },
        include: {
          schedules: {
            select: { id: true }
          }
        }
      });

      if (!userData) {
        return null;
      }

      return this._mapToEntity(userData);
    } catch (error) {
      throw new DatabaseError(`Failed to find user by ID: ${error.message}`);
    }
  }

  /**
   * Saves a user (create or update)
   * @param {User} user - User instance to save
   * @returns {Promise<User>} Saved user instance
   */
  async save(user) {
    try {
      const userData = await this._prisma.user.upsert({
        where: { discordId: user.discordId },
        update: {
          username: user.username,
          nickname: user.nickname
        },
        create: {
          discordId: user.discordId,
          username: user.username,
          nickname: user.nickname
        },
        include: {
          schedules: {
            select: { id: true }
          }
        }
      });

      return this._mapToEntity(userData);
    } catch (error) {
      throw new DatabaseError(`Failed to save user: ${error.message}`);
    }
  }

  /**
   * Deletes a user by Discord ID
   * @param {string} discordId - Discord user ID
   * @returns {Promise<boolean>} True if deleted, false if not found
   */
  async delete(discordId) {
    try {
      const result = await this._prisma.user.delete({
        where: { discordId }
      });
      return !!result;
    } catch (error) {
      if (error.code === 'P2025') { // Record not found
        return false;
      }
      throw new DatabaseError(`Failed to delete user: ${error.message}`);
    }
  }

  /**
   * Finds users by username pattern
   * @param {string} usernamePattern - Pattern to search for
   * @returns {Promise<User[]>} Array of matching users
   */
  async findByUsernamePattern(usernamePattern) {
    try {
      const usersData = await this._prisma.user.findMany({
        where: {
          username: {
            contains: usernamePattern,
            mode: 'insensitive'
          }
        },
        include: {
          schedules: {
            select: { id: true }
          }
        },
        orderBy: {
          username: 'asc'
        }
      });

      return usersData.map(userData => this._mapToEntity(userData));
    } catch (error) {
      throw new DatabaseError(`Failed to find users by username pattern: ${error.message}`);
    }
  }

  /**
   * Maps Prisma user data to User entity
   * @private
   * @param {Object} userData - Prisma user data
   * @returns {User} User entity instance
   */
  _mapToEntity(userData) {
    const user = new User(userData.discordId, userData.username, userData.nickname);

    // Add schedule IDs if available
    if (userData.schedules) {
      userData.schedules.forEach(schedule => {
        user.addSchedule(schedule.id);
      });
    }

    return user;
  }
}

export default PrismaUserRepository;