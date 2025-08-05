import AircraftType from '../enums/AircraftType.js';
import MissionType from '../enums/MissionType.js';
import ActionSubType from '../enums/ActionSubType.js';

/**
 * Schedule entity representing a flight schedule
 * Follows Single Responsibility Principle - only handles schedule data and business logic
 */
class Schedule {
  /**
   * Creates a new Schedule instance
   * @param {Object} params - Schedule parameters
   * @param {string} params.guildId - Discord guild ID
   * @param {string} params.title - Schedule title
   * @param {string} params.aircraftType - Aircraft type from AircraftType enum
   * @param {string} params.missionType - Mission type from MissionType enum
   * @param {string} params.createdById - Creator's Discord ID
   * @param {string} params.createdByUsername - Creator's username
   * @param {string} [params.actionSubType] - Action subtype (for ACTION missions)
   * @param {string} [params.actionOption] - Action option (for ACTION missions)
   * @param {string} [params.outrosDescription] - Description (for OUTROS missions)
   */
  constructor({
    guildId,
    title,
    aircraftType,
    missionType,
    createdById,
    createdByUsername,
    actionSubType = null,
    actionOption = null,
    outrosDescription = null
  }) {
    this._validateRequiredFields({ guildId, title, aircraftType, missionType, createdById, createdByUsername });
    this._validateMissionSpecificFields({ missionType, actionSubType, actionOption, outrosDescription });

    this._id = null; // Will be set by repository
    this._guildId = guildId.trim();
    this._title = title.trim();
    this._aircraftType = aircraftType;
    this._missionType = missionType;
    this._actionSubType = actionSubType;
    this._actionOption = actionOption?.trim() || null;
    this._outrosDescription = outrosDescription?.trim() || null;
    this._startTime = new Date();
    this._endTime = null;
    this._createdById = createdById.trim();
    this._createdByUsername = createdByUsername.trim();
    this._active = true;
    this._crewMembers = [];
  }

  /**
   * Validates required fields
   * @private
   */
  _validateRequiredFields({ guildId, title, aircraftType, missionType, createdById, createdByUsername }) {
    if (!guildId || typeof guildId !== 'string') {
      throw new Error('Guild ID is required and must be a string');
    }
    if (!title || typeof title !== 'string') {
      throw new Error('Title is required and must be a string');
    }
    if (!Object.values(AircraftType).includes(aircraftType)) {
      throw new Error(`Invalid aircraft type: ${aircraftType}`);
    }
    if (!Object.values(MissionType).includes(missionType)) {
      throw new Error(`Invalid mission type: ${missionType}`);
    }
    if (!createdById || typeof createdById !== 'string') {
      throw new Error('Created by ID is required and must be a string');
    }
    if (!createdByUsername || typeof createdByUsername !== 'string') {
      throw new Error('Created by username is required and must be a string');
    }
  }

  /**
   * Validates mission-specific fields
   * @private
   */
  _validateMissionSpecificFields({ missionType, actionSubType, actionOption, outrosDescription }) {
    if (missionType === MissionType.OUTROS) {
      if (!outrosDescription || outrosDescription.trim() === '') {
        throw new Error('Description is required for OUTROS missions');
      }
    }
    
    if (missionType === MissionType.ACTION && actionSubType && !Object.values(ActionSubType).includes(actionSubType)) {
      throw new Error(`Invalid action sub type: ${actionSubType}`);
    }
  }

  // Getters
  get id() { return this._id; }
  get guildId() { return this._guildId; }
  get title() { return this._title; }
  get aircraftType() { return this._aircraftType; }
  get missionType() { return this._missionType; }
  get actionSubType() { return this._actionSubType; }
  get actionOption() { return this._actionOption; }
  get outrosDescription() { return this._outrosDescription; }
  get startTime() { return this._startTime; }
  get endTime() { return this._endTime; }
  get createdById() { return this._createdById; }
  get createdByUsername() { return this._createdByUsername; }
  get active() { return this._active; }
  get crewMembers() { return [...this._crewMembers]; } // Return copy to prevent mutations

  /**
   * Sets the schedule ID (used by repository)
   * @param {number} id - Schedule ID
   */
  setId(id) {
    if (this._id !== null) {
      throw new Error('Schedule ID cannot be changed once set');
    }
    if (!Number.isInteger(id)) {
      throw new Error('Schedule ID must be an integer');
    }
    this._id = id;
  }

  /**
   * Checks if a user is the creator of this schedule
   * @param {string} discordId - Discord user ID to check
   * @returns {boolean} True if user is creator
   */
  isCreator(discordId) {
    return this._createdById === discordId;
  }

  /**
   * Checks if a user is in the crew
   * @param {string} discordId - Discord user ID to check
   * @returns {boolean} True if user is in crew
   */
  hasCrewMember(discordId) {
    return this._crewMembers.some(member => member.discordId === discordId);
  }

  /**
   * Adds a crew member to the schedule
   * @param {User} user - User to add to crew
   * @throws {Error} If user is creator or already in crew
   */
  addCrewMember(user) {
    if (!this._active) {
      throw new Error('Cannot modify inactive schedule');
    }
    
    if (this.isCreator(user.discordId)) {
      throw new Error('Creator cannot be added as crew member');
    }
    
    if (this.hasCrewMember(user.discordId)) {
      throw new Error('User is already in crew');
    }
    
    this._crewMembers.push({
      discordId: user.discordId,
      username: user.username,
      nickname: user.nickname
    });
  }

  /**
   * Removes a crew member from the schedule
   * @param {string} discordId - Discord ID of user to remove
   * @throws {Error} If user is creator or not in crew
   */
  removeCrewMember(discordId) {
    if (!this._active) {
      throw new Error('Cannot modify inactive schedule');
    }
    
    if (this.isCreator(discordId)) {
      throw new Error('Creator cannot leave schedule');
    }
    
    const memberIndex = this._crewMembers.findIndex(member => member.discordId === discordId);
    if (memberIndex === -1) {
      throw new Error('User is not in crew');
    }
    
    this._crewMembers.splice(memberIndex, 1);
  }

  /**
   * Closes the schedule
   * @param {string} closedById - Discord ID of user closing the schedule
   * @throws {Error} If schedule is already closed
   */
  close(closedById) {
    if (!this._active) {
      throw new Error('Schedule is already closed');
    }
    
    this._active = false;
    this._endTime = new Date();
  }

  /**
   * Converts schedule to plain object for serialization
   * @returns {Object} Plain object representation
   */
  toJSON() {
    return {
      id: this._id,
      guildId: this._guildId,
      title: this._title,
      aircraftType: this._aircraftType,
      missionType: this._missionType,
      actionSubType: this._actionSubType,
      actionOption: this._actionOption,
      outrosDescription: this._outrosDescription,
      startTime: this._startTime.toISOString(),
      endTime: this._endTime?.toISOString() || null,
      createdById: this._createdById,
      createdByUsername: this._createdByUsername,
      active: this._active,
      crewMembers: [...this._crewMembers]
    };
  }

  /**
   * Creates Schedule instance from plain object
   * @param {Object} scheduleData - Schedule data object
   * @returns {Schedule} Schedule instance
   */
  static fromJSON(scheduleData) {
    const schedule = new Schedule({
      guildId: scheduleData.guildId,
      title: scheduleData.title,
      aircraftType: scheduleData.aircraftType,
      missionType: scheduleData.missionType,
      createdById: scheduleData.createdById,
      createdByUsername: scheduleData.createdByUsername,
      actionSubType: scheduleData.actionSubType,
      actionOption: scheduleData.actionOption,
      outrosDescription: scheduleData.outrosDescription
    });

    if (scheduleData.id !== null) {
      schedule.setId(scheduleData.id);
    }
    schedule._startTime = new Date(scheduleData.startTime);
    schedule._endTime = scheduleData.endTime ? new Date(scheduleData.endTime) : null;
    schedule._active = scheduleData.active;
    schedule._crewMembers = [...scheduleData.crewMembers];

    return schedule;
  }
}

export default Schedule;