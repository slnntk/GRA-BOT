/**
 * User repository interface
 * Follows Interface Segregation Principle - specific to user operations
 * Follows Dependency Inversion Principle - defines abstraction for data access
 */
class IUserRepository {
  /**
   * Finds a user by Discord ID
   * @param {string} discordId - Discord user ID
   * @returns {Promise<User|null>} User instance or null if not found
   */
  async findById(_discordId) {
    throw new Error('Method must be implemented by concrete repository');
  }

  /**
   * Saves a user (create or update)
   * @param {User} user - User instance to save
   * @returns {Promise<User>} Saved user instance
   */
  async save(_user) {
    throw new Error('Method must be implemented by concrete repository');
  }

  /**
   * Deletes a user by Discord ID
   * @param {string} discordId - Discord user ID
   * @returns {Promise<boolean>} True if deleted, false if not found
   */
  async delete(_discordId) {
    throw new Error('Method must be implemented by concrete repository');
  }

  /**
   * Finds users by username pattern
   * @param {string} usernamePattern - Pattern to search for
   * @returns {Promise<User[]>} Array of matching users
   */
  async findByUsernamePattern(_usernamePattern) {
    throw new Error('Method must be implemented by concrete repository');
  }
}

export default IUserRepository;