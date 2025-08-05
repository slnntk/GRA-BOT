import { PrismaClient } from '@prisma/client';
import config from '../config/Config.js';
import logger from '../utils/logger.js';

// Repositories
import PrismaUserRepository from '../infrastructure/database/PrismaUserRepository.js';
import PrismaScheduleRepository from '../infrastructure/database/PrismaScheduleRepository.js';

// Use Cases
import UserUseCase from '../application/use-cases/UserUseCase.js';
import ScheduleUseCase from '../application/use-cases/ScheduleUseCase.js';

/**
 * Dependency Injection Container
 * Follows Dependency Inversion Principle - manages all dependencies centrally
 * Follows Single Responsibility Principle - only handles dependency creation and wiring
 */
class DIContainer {
  constructor() {
    this._dependencies = new Map();
    this._singletons = new Map();
    this._initialized = false;
  }

  /**
   * Initializes all dependencies
   * Should be called once at application startup
   */
  async initialize() {
    if (this._initialized) {
      throw new Error('Container already initialized');
    }

    try {
      // Initialize database connection
      await this._initializeDatabase();
      
      // Register repositories
      this._registerRepositories();
      
      // Register use cases
      this._registerUseCases();
      
      this._initialized = true;
      logger.info('Dependency injection container initialized successfully');
    } catch (error) {
      logger.error('Failed to initialize DI container', { error: error.message });
      throw error;
    }
  }

  /**
   * Gets a dependency by name
   * @param {string} name - Dependency name
   * @returns {*} Dependency instance
   */
  get(name) {
    if (!this._initialized) {
      throw new Error('Container not initialized. Call initialize() first.');
    }

    if (this._singletons.has(name)) {
      return this._singletons.get(name);
    }

    if (this._dependencies.has(name)) {
      const factory = this._dependencies.get(name);
      const instance = factory();
      
      // Store as singleton if marked as such
      if (factory.singleton) {
        this._singletons.set(name, instance);
      }
      
      return instance;
    }

    throw new Error(`Dependency '${name}' not found`);
  }

  /**
   * Registers a dependency with a factory function
   * @param {string} name - Dependency name
   * @param {Function} factory - Factory function that creates the dependency
   * @param {boolean} singleton - Whether to create as singleton
   */
  register(name, factory, singleton = false) {
    factory.singleton = singleton;
    this._dependencies.set(name, factory);
  }

  /**
   * Shuts down the container and cleans up resources
   */
  async shutdown() {
    try {
      // Disconnect Prisma
      const prisma = this._singletons.get('prisma');
      if (prisma) {
        await prisma.$disconnect();
      }

      this._dependencies.clear();
      this._singletons.clear();
      this._initialized = false;

      logger.info('Dependency injection container shut down successfully');
    } catch (error) {
      logger.error('Error during DI container shutdown', { error: error.message });
      throw error;
    }
  }

  /**
   * Initializes database connection
   * @private
   */
  async _initializeDatabase() {
    const prisma = new PrismaClient({
      datasources: {
        db: {
          url: config.database.url
        }
      },
      log: config.isDevelopment() ? ['query', 'info', 'warn', 'error'] : ['warn', 'error']
    });

    // Test connection
    await prisma.$connect();
    logger.info('Database connection established');

    // Store as singleton
    this._singletons.set('prisma', prisma);

    // Register database factory
    this.register('database', () => prisma, true);
  }

  /**
   * Registers repository dependencies
   * @private
   */
  _registerRepositories() {
    // User Repository
    this.register('userRepository', () => {
      const prisma = this.get('database');
      return new PrismaUserRepository(prisma);
    }, true);

    // Schedule Repository
    this.register('scheduleRepository', () => {
      const prisma = this.get('database');
      return new PrismaScheduleRepository(prisma);
    }, true);
  }

  /**
   * Registers use case dependencies
   * @private
   */
  _registerUseCases() {
    // User Use Case
    this.register('userUseCase', () => {
      const userRepository = this.get('userRepository');
      return new UserUseCase(userRepository);
    }, true);

    // Schedule Use Case
    this.register('scheduleUseCase', () => {
      const scheduleRepository = this.get('scheduleRepository');
      const userUseCase = this.get('userUseCase');
      return new ScheduleUseCase(scheduleRepository, userUseCase);
    }, true);
  }

  /**
   * Creates a health check for all dependencies
   * @returns {Promise<Object>} Health status of all dependencies
   */
  async healthCheck() {
    const health = {
      status: 'healthy',
      dependencies: {}
    };

    try {
      // Check database connection
      const prisma = this.get('database');
      await prisma.$queryRaw`SELECT 1`;
      health.dependencies.database = 'healthy';
    } catch (error) {
      health.dependencies.database = `unhealthy: ${error.message}`;
      health.status = 'unhealthy';
    }

    // Check if critical dependencies are available
    const criticalDeps = ['userRepository', 'scheduleRepository', 'userUseCase', 'scheduleUseCase'];
    
    for (const dep of criticalDeps) {
      try {
        this.get(dep);
        health.dependencies[dep] = 'healthy';
      } catch (error) {
        health.dependencies[dep] = `unhealthy: ${error.message}`;
        health.status = 'unhealthy';
      }
    }

    return health;
  }
}

// Export singleton instance
const container = new DIContainer();
export default container;