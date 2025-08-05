/**
 * Custom error classes for better error handling
 * Follows Single Responsibility Principle - each error has specific purpose
 */

/**
 * Base application error class
 */
class AppError extends Error {
  constructor(message, statusCode = 500, isOperational = true) {
    super(message);
    this.name = this.constructor.name;
    this.statusCode = statusCode;
    this.isOperational = isOperational;
    
    Error.captureStackTrace(this, this.constructor);
  }
}

/**
 * Validation error for invalid input
 */
class ValidationError extends AppError {
  constructor(message) {
    super(message, 400);
  }
}

/**
 * Not found error for missing resources
 */
class NotFoundError extends AppError {
  constructor(resource, identifier) {
    super(`${resource} with identifier '${identifier}' not found`, 404);
  }
}

/**
 * Schedule-specific errors
 */
class ScheduleError extends AppError {
  constructor(message) {
    super(message, 400);
  }
}

class ScheduleNotFoundError extends NotFoundError {
  constructor(scheduleId) {
    super('Schedule', scheduleId);
  }
}

class ScheduleAlreadyClosedError extends ScheduleError {
  constructor() {
    super('Schedule is already closed');
  }
}

class UserAlreadyBoardedError extends ScheduleError {
  constructor() {
    super('User is already on board');
  }
}

class UserNotBoardedError extends ScheduleError {
  constructor() {
    super('User is not on board');
  }
}

class PilotCannotBeCrewError extends ScheduleError {
  constructor() {
    super('Pilot cannot be added as crew member');
  }
}

class CreatorCannotLeaveError extends ScheduleError {
  constructor() {
    super('Schedule creator cannot leave');
  }
}

class OnlyCreatorCanCloseScheduleError extends ScheduleError {
  constructor() {
    super('Only the schedule creator can close it');
  }
}

/**
 * Discord-specific errors
 */
class DiscordError extends AppError {
  constructor(message) {
    super(message, 500);
  }
}

class InvalidCustomIdError extends DiscordError {
  constructor(customId) {
    super(`Invalid custom ID: ${customId}`);
  }
}

/**
 * Database errors
 */
class DatabaseError extends AppError {
  constructor(message) {
    super(message, 500);
  }
}

/**
 * Error handler utility functions
 */
class ErrorHandler {
  /**
   * Determines if an error is operational (expected) or a programming error
   * @param {Error} error - Error to check
   * @returns {boolean} True if operational error
   */
  static isOperationalError(error) {
    return error instanceof AppError && error.isOperational;
  }

  /**
   * Logs error with appropriate level based on type
   * @param {Error} error - Error to log
   * @param {Object} logger - Logger instance
   */
  static logError(error, logger) {
    if (this.isOperationalError(error)) {
      logger.warn(error.message, { error: error.stack });
    } else {
      logger.error(error.message, { error: error.stack });
    }
  }

  /**
   * Creates user-friendly error message for Discord responses
   * @param {Error} error - Error to format
   * @returns {string} User-friendly error message
   */
  static formatDiscordError(error) {
    if (error instanceof ValidationError) {
      return `❌ **Erro de validação:** ${error.message}`;
    }
    
    if (error instanceof NotFoundError) {
      return `❌ **Não encontrado:** ${error.message}`;
    }
    
    if (error instanceof ScheduleError) {
      return `❌ **Erro na escala:** ${error.message}`;
    }
    
    if (error instanceof DiscordError) {
      return `❌ **Erro do Discord:** ${error.message}`;
    }

    // For unexpected errors, don't expose internal details
    return '❌ **Erro interno:** Algo deu errado. Tente novamente mais tarde.';
  }
}

export {
  AppError,
  ValidationError,
  NotFoundError,
  ScheduleError,
  ScheduleNotFoundError,
  ScheduleAlreadyClosedError,
  UserAlreadyBoardedError,
  UserNotBoardedError,
  PilotCannotBeCrewError,
  CreatorCannotLeaveError,
  OnlyCreatorCanCloseScheduleError,
  DiscordError,
  InvalidCustomIdError,
  DatabaseError,
  ErrorHandler
};