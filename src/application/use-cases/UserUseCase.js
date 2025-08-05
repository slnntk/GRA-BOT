import User from '../domain/entities/User.js';

/**
 * Use case for managing users
 * Follows Single Responsibility Principle - handles user business logic
 * Follows Dependency Inversion Principle - depends on abstractions
 */
class UserUseCase {
  /**
   * Creates a new UserUseCase instance
   * @param {IUserRepository} userRepository - User repository implementation
   */
  constructor(userRepository) {
    if (!userRepository) {
      throw new Error('User repository is required');
    }
    this._userRepository = userRepository;
  }

  /**
   * Gets or creates a user
   * Pure function that doesn't modify external state
   * @param {string} discordId - Discord user ID
   * @param {string} username - Discord username
   * @param {string} nickname - Display nickname
   * @returns {Promise<User>} User instance
   */
  async getOrCreateUser(discordId, username, nickname) {
    try {
      // Input validation
      if (!discordId || !username || !nickname) {
        throw new Error('Discord ID, username, and nickname are required');
      }

      // Try to find existing user
      const existingUser = await this._userRepository.findById(discordId);

      if (existingUser) {
        // Update nickname if it has changed
        if (existingUser.nickname !== nickname) {
          existingUser.updateNickname(nickname);
          return await this._userRepository.save(existingUser);
        }
        return existingUser;
      }

      // Create new user
      const newUser = new User(discordId, username, nickname);
      return await this._userRepository.save(newUser);
    } catch (error) {
      throw new Error(`Failed to get or create user: ${error.message}`);
    }
  }

  /**
   * Finds a user by Discord ID
   * @param {string} discordId - Discord user ID
   * @returns {Promise<User|null>} User instance or null if not found
   */
  async findUser(discordId) {
    try {
      if (!discordId) {
        throw new Error('Discord ID is required');
      }

      return await this._userRepository.findById(discordId);
    } catch (error) {
      throw new Error(`Failed to find user: ${error.message}`);
    }
  }

  /**
   * Updates a user's nickname
   * @param {string} discordId - Discord user ID
   * @param {string} newNickname - New nickname
   * @returns {Promise<User>} Updated user instance
   */
  async updateUserNickname(discordId, newNickname) {
    try {
      if (!discordId || !newNickname) {
        throw new Error('Discord ID and new nickname are required');
      }

      const user = await this._userRepository.findById(discordId);
      if (!user) {
        throw new Error('User not found');
      }

      user.updateNickname(newNickname);
      return await this._userRepository.save(user);
    } catch (error) {
      throw new Error(`Failed to update user nickname: ${error.message}`);
    }
  }

  /**
   * Searches users by username pattern
   * @param {string} pattern - Username pattern to search
   * @returns {Promise<User[]>} Array of matching users
   */
  async searchUsersByUsername(pattern) {
    try {
      if (!pattern) {
        throw new Error('Search pattern is required');
      }

      return await this._userRepository.findByUsernamePattern(pattern);
    } catch (error) {
      throw new Error(`Failed to search users: ${error.message}`);
    }
  }
}

export default UserUseCase;