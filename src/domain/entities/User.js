/**
 * User entity representing a Discord user
 * Follows Single Responsibility Principle - only handles user data
 */
class User {
  /**
   * Creates a new User instance
   * @param {string} discordId - Discord user ID (required)
   * @param {string} username - Discord username (required)
   * @param {string} nickname - Display nickname (required)
   */
  constructor(discordId, username, nickname) {
    if (!discordId || typeof discordId !== 'string') {
      throw new Error('Discord ID is required and must be a string');
    }
    if (!username || typeof username !== 'string') {
      throw new Error('Username is required and must be a string');
    }
    if (!nickname || typeof nickname !== 'string') {
      throw new Error('Nickname is required and must be a string');
    }

    this._discordId = discordId.trim();
    this._username = username.trim();
    this._nickname = nickname.trim();
    this._schedules = [];
  }

  // Getters (read-only properties)
  get discordId() {
    return this._discordId;
  }

  get username() {
    return this._username;
  }

  get nickname() {
    return this._nickname;
  }

  get schedules() {
    return [...this._schedules]; // Return copy to prevent external mutations
  }

  /**
   * Updates user nickname
   * @param {string} newNickname - New nickname
   */
  updateNickname(newNickname) {
    if (!newNickname || typeof newNickname !== 'string') {
      throw new Error('Nickname must be a non-empty string');
    }
    this._nickname = newNickname.trim();
  }

  /**
   * Adds a schedule to user's schedule list
   * @param {number} scheduleId - Schedule ID to add
   */
  addSchedule(scheduleId) {
    if (!Number.isInteger(scheduleId)) {
      throw new Error('Schedule ID must be an integer');
    }
    if (!this._schedules.includes(scheduleId)) {
      this._schedules.push(scheduleId);
    }
  }

  /**
   * Removes a schedule from user's schedule list
   * @param {number} scheduleId - Schedule ID to remove
   */
  removeSchedule(scheduleId) {
    const index = this._schedules.indexOf(scheduleId);
    if (index > -1) {
      this._schedules.splice(index, 1);
    }
  }

  /**
   * Converts user instance to plain object for serialization
   * @returns {Object} Plain object representation
   */
  toJSON() {
    return {
      discordId: this._discordId,
      username: this._username,
      nickname: this._nickname,
      schedules: [...this._schedules]
    };
  }

  /**
   * Creates User instance from plain object
   * @param {Object} userData - User data object
   * @returns {User} User instance
   */
  static fromJSON(userData) {
    const user = new User(userData.discordId, userData.username, userData.nickname);
    if (userData.schedules && Array.isArray(userData.schedules)) {
      userData.schedules.forEach(scheduleId => user.addSchedule(scheduleId));
    }
    return user;
  }
}

export default User;