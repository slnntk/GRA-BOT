/**
 * Schedule repository interface
 * Follows Interface Segregation Principle - specific to schedule operations
 * Follows Dependency Inversion Principle - defines abstraction for data access
 */
class IScheduleRepository {
  /**
   * Finds a schedule by ID
   * @param {number} id - Schedule ID
   * @returns {Promise<Schedule|null>} Schedule instance or null if not found
   */
  async findById(_id) {
    throw new Error('Method must be implemented by concrete repository');
  }

  /**
   * Finds a schedule by ID and guild ID
   * @param {number} id - Schedule ID
   * @param {string} guildId - Discord guild ID
   * @returns {Promise<Schedule|null>} Schedule instance or null if not found
   */
  async findByIdAndGuildId(_id, _guildId) {
    throw new Error('Method must be implemented by concrete repository');
  }

  /**
   * Finds active schedules for a guild
   * @param {string} guildId - Discord guild ID
   * @returns {Promise<Schedule[]>} Array of active schedules
   */
  async findActiveByGuildId(_guildId) {
    throw new Error('Method must be implemented by concrete repository');
  }

  /**
   * Finds schedules that ended before a specific date
   * @param {Date} endTime - End time threshold
   * @returns {Promise<Schedule[]>} Array of old schedules
   */
  async findByEndTimeBefore(_endTime) {
    throw new Error('Method must be implemented by concrete repository');
  }

  /**
   * Saves a schedule (create or update)
   * @param {Schedule} schedule - Schedule instance to save
   * @returns {Promise<Schedule>} Saved schedule instance
   */
  async save(_schedule) {
    throw new Error('Method must be implemented by concrete repository');
  }

  /**
   * Deletes a schedule by ID
   * @param {number} id - Schedule ID
   * @returns {Promise<boolean>} True if deleted, false if not found
   */
  async delete(_id) {
    throw new Error('Method must be implemented by concrete repository');
  }

  /**
   * Counts active schedules for a guild
   * @param {string} guildId - Discord guild ID
   * @returns {Promise<number>} Number of active schedules
   */
  async countActiveByGuildId(_guildId) {
    throw new Error('Method must be implemented by concrete repository');
  }
}

export default IScheduleRepository;