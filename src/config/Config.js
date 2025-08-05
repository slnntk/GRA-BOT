import dotenv from 'dotenv';

// Load environment variables
dotenv.config();

/**
 * Application configuration
 * Centralized configuration management with validation
 */
class Config {
  constructor() {
    this._validateRequiredEnvVars();

    // Discord configuration
    this.discord = {
      token: process.env.DISCORD_TOKEN,
      clientId: process.env.DISCORD_CLIENT_ID,
      guildId: process.env.DISCORD_GUILD_ID
    };

    // Database configuration
    this.database = {
      url: process.env.DATABASE_URL,
      maxConnections: parseInt(process.env.DB_MAX_CONNECTIONS) || 10,
      connectionTimeout: parseInt(process.env.DB_CONNECTION_TIMEOUT) || 30000
    };

    // Application configuration
    this.app = {
      port: parseInt(process.env.PORT) || 3000,
      nodeEnv: process.env.NODE_ENV || 'development',
      logLevel: process.env.LOG_LEVEL || 'info',
      timezone: process.env.TIMEZONE || 'America/Fortaleza'
    };

    // Schedule configuration
    this.schedule = {
      cleanupIntervalHours: parseInt(process.env.SCHEDULE_CLEANUP_INTERVAL_HOURS) || 24,
      oldScheduleThresholdDays: parseInt(process.env.OLD_SCHEDULE_THRESHOLD_DAYS) || 30
    };
  }

  /**
   * Validates that required environment variables are present
   * @private
   */
  _validateRequiredEnvVars() {
    const required = [
      'DISCORD_TOKEN',
      'DATABASE_URL'
    ];

    const missing = required.filter(key => !process.env[key]);

    if (missing.length > 0) {
      throw new Error(`Missing required environment variables: ${missing.join(', ')}`);
    }
  }

  /**
   * Checks if the application is in development mode
   * @returns {boolean} True if in development mode
   */
  isDevelopment() {
    return this.app.nodeEnv === 'development';
  }

  /**
   * Checks if the application is in production mode
   * @returns {boolean} True if in production mode
   */
  isProduction() {
    return this.app.nodeEnv === 'production';
  }

  /**
   * Gets the timezone offset for the configured timezone
   * @returns {string} Timezone string
   */
  getTimezone() {
    return this.app.timezone;
  }
}

// Export singleton instance
const config = new Config();
export default config;