import winston from 'winston';
import config from '../config/Config.js';

/**
 * Logger configuration using Winston
 * Centralized logging with different levels and formats
 */
class Logger {
  constructor() {
    this._logger = winston.createLogger({
      level: config.app.logLevel,
      format: winston.format.combine(
        winston.format.timestamp({
          format: 'YYYY-MM-DD HH:mm:ss'
        }),
        winston.format.errors({ stack: true }),
        winston.format.json()
      ),
      defaultMeta: {
        service: 'gra-bot',
        environment: config.app.nodeEnv
      },
      transports: [
        // Write logs to console
        new winston.transports.Console({
          format: winston.format.combine(
            winston.format.colorize(),
            winston.format.simple(),
            winston.format.printf(({ timestamp, level, message, service, ...meta }) => {
              let metaStr = '';
              if (Object.keys(meta).length > 0) {
                metaStr = `\n${JSON.stringify(meta, null, 2)}`;
              }
              return `${timestamp} [${service}] ${level}: ${message}${metaStr}`;
            })
          )
        })
      ]
    });

    // Add file transport in production
    if (config.isProduction()) {
      this._logger.add(
        new winston.transports.File({
          filename: 'logs/error.log',
          level: 'error',
          format: winston.format.json()
        })
      );
      
      this._logger.add(
        new winston.transports.File({
          filename: 'logs/combined.log',
          format: winston.format.json()
        })
      );
    }
  }

  /**
   * Log error message
   * @param {string} message - Error message
   * @param {Object} [meta] - Additional metadata
   */
  error(message, meta = {}) {
    this._logger.error(message, meta);
  }

  /**
   * Log warning message
   * @param {string} message - Warning message
   * @param {Object} [meta] - Additional metadata
   */
  warn(message, meta = {}) {
    this._logger.warn(message, meta);
  }

  /**
   * Log info message
   * @param {string} message - Info message
   * @param {Object} [meta] - Additional metadata
   */
  info(message, meta = {}) {
    this._logger.info(message, meta);
  }

  /**
   * Log debug message
   * @param {string} message - Debug message
   * @param {Object} [meta] - Additional metadata
   */
  debug(message, meta = {}) {
    this._logger.debug(message, meta);
  }

  /**
   * Log verbose message
   * @param {string} message - Verbose message
   * @param {Object} [meta] - Additional metadata
   */
  verbose(message, meta = {}) {
    this._logger.verbose(message, meta);
  }

  /**
   * Creates a child logger with additional default metadata
   * @param {Object} defaultMeta - Default metadata for child logger
   * @returns {Logger} Child logger instance
   */
  child(defaultMeta) {
    const childLogger = this._logger.child(defaultMeta);
    return {
      error: (message, meta = {}) => childLogger.error(message, meta),
      warn: (message, meta = {}) => childLogger.warn(message, meta),
      info: (message, meta = {}) => childLogger.info(message, meta),
      debug: (message, meta = {}) => childLogger.debug(message, meta),
      verbose: (message, meta = {}) => childLogger.verbose(message, meta)
    };
  }

  /**
   * Gets the underlying Winston logger instance
   * @returns {winston.Logger} Winston logger instance
   */
  getWinstonLogger() {
    return this._logger;
  }
}

// Export singleton instance
const logger = new Logger();
export default logger;