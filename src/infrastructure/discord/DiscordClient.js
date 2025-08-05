import { Client, GatewayIntentBits, Events } from 'discord.js';
import config from '../config/Config.js';
import container from '../config/DIContainer.js';
import logger from '../utils/logger.js';
import { ErrorHandler } from '../utils/errors.js';

/**
 * Discord client wrapper following Single Responsibility Principle
 * Handles Discord connection and basic event management
 */
class DiscordClient {
  constructor() {
    this._client = new Client({
      intents: [
        GatewayIntentBits.Guilds,
        GatewayIntentBits.GuildMessages,
        GatewayIntentBits.MessageContent,
        GatewayIntentBits.GuildMembers
      ]
    });

    this._isReady = false;
    this._setupEventHandlers();
  }

  /**
   * Sets up basic Discord event handlers
   * @private
   */
  _setupEventHandlers() {
    this._client.once(Events.ClientReady, async (readyClient) => {
      try {
        logger.info(`Discord bot ready! Logged in as ${readyClient.user.tag}`);
        logger.info(`Bot is in ${readyClient.guilds.cache.size} guilds`);

        this._isReady = true;

        // Perform health check
        const health = await container.healthCheck();
        if (health.status === 'healthy') {
          logger.info('All systems healthy - bot fully operational');
        } else {
          logger.warn('Some systems unhealthy', { health });
        }
      } catch (error) {
        logger.error('Error during bot ready event', { error: error.message });
      }
    });

    this._client.on(Events.Error, (error) => {
      ErrorHandler.logError(error, logger);
    });

    this._client.on(Events.Warn, (warning) => {
      logger.warn('Discord warning', { warning });
    });

    // Graceful shutdown
    process.on('SIGINT', () => {
      logger.info('Received SIGINT, shutting down gracefully...');
      this.shutdown();
    });

    process.on('SIGTERM', () => {
      logger.info('Received SIGTERM, shutting down gracefully...');
      this.shutdown();
    });
  }

  /**
   * Connects to Discord
   * @returns {Promise<void>}
   */
  async connect() {
    try {
      logger.info('Connecting to Discord...');
      await this._client.login(config.discord.token);
    } catch (error) {
      logger.error('Failed to connect to Discord', { error: error.message });
      throw error;
    }
  }

  /**
   * Disconnects from Discord and shuts down
   * @returns {Promise<void>}
   */
  async shutdown() {
    try {
      logger.info('Shutting down Discord client...');

      if (this._client) {
        this._client.destroy();
      }

      await container.shutdown();
      logger.info('Shutdown complete');
      process.exit(0);
    } catch (error) {
      logger.error('Error during shutdown', { error: error.message });
      process.exit(1);
    }
  }

  /**
   * Gets the Discord client instance
   * @returns {Client} Discord client
   */
  getClient() {
    return this._client;
  }

  /**
   * Checks if the bot is ready
   * @returns {boolean} True if ready
   */
  isReady() {
    return this._isReady;
  }

  /**
   * Checks if a user has a specific role in a guild
   * @param {string} guildId - Guild ID
   * @param {string} userId - User ID
   * @param {string} roleId - Role ID
   * @returns {boolean} True if user has role
   */
  async checkUserHasRole(guildId, userId, roleId) {
    try {
      const guild = this._client.guilds.cache.get(guildId);
      if (!guild) {
        return false;
      }

      const member = await guild.members.fetch(userId);
      if (!member) {
        return false;
      }

      return member.roles.cache.has(roleId);
    } catch (error) {
      logger.warn('Error checking user role', {
        guildId,
        userId,
        roleId,
        error: error.message
      });
      return false;
    }
  }

  /**
   * Gets a guild by ID
   * @param {string} guildId - Guild ID
   * @returns {Guild|null} Guild or null if not found
   */
  getGuild(guildId) {
    return this._client.guilds.cache.get(guildId) || null;
  }

  /**
   * Gets a user by ID
   * @param {string} userId - User ID
   * @returns {Promise<User|null>} User or null if not found
   */
  async getUser(userId) {
    try {
      return await this._client.users.fetch(userId);
    } catch (error) {
      logger.warn('Error fetching user', { userId, error: error.message });
      return null;
    }
  }
}

export default DiscordClient;