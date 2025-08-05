import config from './config/Config.js';
import container from './config/DIContainer.js';
import DiscordClient from './infrastructure/discord/DiscordClient.js';
import logger from './utils/logger.js';
import { ErrorHandler } from './utils/errors.js';

/**
 * Main application class
 * Follows Single Responsibility Principle - orchestrates application startup and shutdown
 */
class Application {
  constructor() {
    this._discordClient = null;
    this._isRunning = false;
  }

  /**
   * Starts the application
   * @returns {Promise<void>}
   */
  async start() {
    try {
      logger.info('Starting GRA-BOT application...');
      logger.info(`Environment: ${config.app.nodeEnv}`);
      logger.info(`Log level: ${config.app.logLevel}`);

      // Initialize dependency injection container
      await container.initialize();
      logger.info('Dependencies initialized');

      // Initialize Discord client
      this._discordClient = new DiscordClient();

      // Connect to Discord
      await this._discordClient.connect();

      this._isRunning = true;
      logger.info('GRA-BOT application started successfully');

      // Setup periodic cleanup task
      this._setupCleanupTask();

    } catch (error) {
      logger.error('Failed to start application', { error: error.message, stack: error.stack });
      await this.shutdown();
      throw error;
    }
  }

  /**
   * Shuts down the application
   * @returns {Promise<void>}
   */
  async shutdown() {
    if (!this._isRunning) {
      return;
    }

    try {
      logger.info('Shutting down application...');
      this._isRunning = false;

      // Cleanup interval
      if (this._cleanupInterval) {
        clearInterval(this._cleanupInterval);
      }

      // Shutdown Discord client
      if (this._discordClient) {
        await this._discordClient.shutdown();
      }

      logger.info('Application shutdown complete');
    } catch (error) {
      logger.error('Error during application shutdown', { error: error.message });
      throw error;
    }
  }

  /**
   * Sets up periodic cleanup task for old schedules
   * @private
   */
  _setupCleanupTask() {
    const intervalMs = config.schedule.cleanupIntervalHours * 60 * 60 * 1000;

    this._cleanupInterval = setInterval(async () => {
      try {
        logger.info('Running scheduled cleanup task...');

        const scheduleUseCase = container.get('scheduleUseCase');
        const cleanedCount = await scheduleUseCase.cleanupOldSchedules(
          config.schedule.oldScheduleThresholdDays
        );

        if (cleanedCount > 0) {
          logger.info(`Cleanup task completed - removed ${cleanedCount} old schedules`);
        } else {
          logger.debug('Cleanup task completed - no old schedules to remove');
        }
      } catch (error) {
        logger.error('Error during scheduled cleanup', { error: error.message });
      }
    }, intervalMs);

    logger.info(`Scheduled cleanup task every ${config.schedule.cleanupIntervalHours} hours`);
  }

  /**
   * Gets the Discord client instance
   * @returns {DiscordClient|null} Discord client or null if not initialized
   */
  getDiscordClient() {
    return this._discordClient;
  }

  /**
   * Checks if the application is running
   * @returns {boolean} True if running
   */
  isRunning() {
    return this._isRunning;
  }

  /**
   * Performs application health check
   * @returns {Promise<Object>} Health status
   */
  async healthCheck() {
    const health = {
      status: 'healthy',
      timestamp: new Date().toISOString(),
      application: {
        running: this._isRunning,
        discord: this._discordClient?.isReady() || false
      }
    };

    try {
      // Check container health
      const containerHealth = await container.healthCheck();
      health.dependencies = containerHealth.dependencies;

      if (containerHealth.status !== 'healthy') {
        health.status = 'unhealthy';
      }

      if (!this._isRunning || !this._discordClient?.isReady()) {
        health.status = 'unhealthy';
      }

    } catch (error) {
      health.status = 'unhealthy';
      health.error = error.message;
    }

    return health;
  }
}

/**
 * Global error handlers
 */
process.on('unhandledRejection', (reason, promise) => {
  logger.error('Unhandled Promise Rejection', {
    reason: reason?.message || reason,
    stack: reason?.stack,
    promise: promise.toString()
  });
});

process.on('uncaughtException', (error) => {
  logger.error('Uncaught Exception', {
    error: error.message,
    stack: error.stack
  });

  // Graceful shutdown on uncaught exception
  setTimeout(() => {
    process.exit(1);
  }, 1000);
});

/**
 * Main application entry point
 */
async function main() {
  const app = new Application();

  try {
    await app.start();
  } catch (error) {
    ErrorHandler.logError(error, logger);
    process.exit(1);
  }
}

// Start the application if this file is run directly
if (import.meta.url === `file://${process.argv[1]}`) {
  main();
}

export default Application;