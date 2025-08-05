# GRA-BOT - JavaScript Refactored Version

## Overview

This is a modern JavaScript refactoring of the original Java Spring Boot Discord bot for managing flight schedules for aviation teams. The project has been completely redesigned using modern JavaScript practices, clean architecture principles, and SOLID design patterns.

## 🚀 Modern JavaScript Features Applied

### Language Features
- ✅ **ES6+ Modules**: Using `import`/`export` syntax
- ✅ **const/let**: No `var` declarations
- ✅ **Arrow Functions**: Used appropriately for callbacks and short functions
- ✅ **Template Literals**: For string interpolation
- ✅ **Destructuring**: Object and array destructuring
- ✅ **Async/Await**: For asynchronous operations
- ✅ **Promises**: Proper promise handling
- ✅ **Classes**: ES6 class syntax with proper encapsulation

### Best Practices
- ✅ **Pure Functions**: No global state mutations
- ✅ **Immutability**: Using `Object.freeze()` for enums
- ✅ **Error Handling**: Comprehensive try/catch and custom error classes
- ✅ **Input Validation**: Proper validation and sanitization
- ✅ **Type Safety**: JSDoc comments for better type hints

## 🏗️ Architecture

### Clean Architecture
The project follows clean architecture principles with clear separation of concerns:

```
src/
├── domain/               # Business logic and entities
│   ├── entities/        # Domain entities (User, Schedule)
│   ├── enums/          # Domain enums (AircraftType, MissionType)
│   └── repositories/   # Repository interfaces
├── application/         # Use cases and business logic
│   └── use-cases/      # Application use cases
├── infrastructure/     # External concerns
│   ├── database/       # Database implementations
│   └── discord/        # Discord API integration
├── config/            # Configuration and DI container
└── utils/             # Utilities (logging, errors)
```

### SOLID Principles Implementation

#### 🎯 Single Responsibility Principle (SRP)
- Each class has one reason to change
- `User` entity only handles user data
- `Schedule` entity only handles schedule business logic
- `UserUseCase` only handles user operations
- `ScheduleUseCase` only handles schedule operations

#### 🔓 Open/Closed Principle (OCP)
- Entities are open for extension but closed for modification
- Repository interfaces allow new implementations without changing existing code
- Error classes can be extended without modifying base classes

#### 🔄 Liskov Substitution Principle (LSP)
- `PrismaUserRepository` can be substituted for `IUserRepository`
- `PrismaScheduleRepository` can be substituted for `IScheduleRepository`
- All implementations are interchangeable through interfaces

#### 🧩 Interface Segregation Principle (ISP)
- `IUserRepository` only contains user-specific methods
- `IScheduleRepository` only contains schedule-specific methods
- Clients depend only on interfaces they use

#### 🔄 Dependency Inversion Principle (DIP)
- High-level modules depend on abstractions, not concretions
- Use cases depend on repository interfaces, not implementations
- Dependency injection container manages all dependencies

## 🛠️ Technology Stack

- **Runtime**: Node.js with ES Modules
- **Database**: PostgreSQL with Prisma ORM
- **Discord**: discord.js v14
- **Testing**: Jest with ES Module support
- **Linting**: ESLint with modern rules
- **Logging**: Winston for structured logging
- **Environment**: dotenv for configuration

## 📊 Project Statistics

- **Test Coverage**: 41 passing tests
- **Code Quality**: 0 linting errors
- **Architecture**: Clean architecture with dependency injection
- **Error Handling**: Comprehensive error handling system
- **Documentation**: JSDoc comments throughout

## 🚦 Getting Started

### Prerequisites
- Node.js 18+ (with ES Module support)
- PostgreSQL database
- Discord bot token

### Installation

1. Clone the repository
2. Install dependencies:
   ```bash
   npm install
   ```

3. Set up environment variables:
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

4. Run database migrations:
   ```bash
   npx prisma migrate dev
   ```

5. Start the application:
   ```bash
   npm start
   ```

### Development

```bash
# Run in development mode with auto-reload
npm run dev

# Run tests
npm test

# Run tests in watch mode
npm run test:watch

# Lint code
npm run lint

# Fix linting issues
npm run lint:fix
```

## 🧪 Testing

The project includes comprehensive unit tests for all domain entities and business logic:

- **User Entity Tests**: 18 test cases covering all methods and edge cases
- **Schedule Entity Tests**: 23 test cases covering business logic and validation

Run tests with:
```bash
npm test
```

## 📝 Key Improvements from Java Version

### Code Quality
- **Modern Syntax**: ES6+ features throughout
- **Type Safety**: JSDoc comments for better IDE support
- **Immutability**: Proper immutable patterns
- **Pure Functions**: No side effects in business logic

### Architecture
- **Clean Architecture**: Clear separation of concerns
- **Dependency Injection**: Centralized dependency management
- **SOLID Principles**: All five principles implemented
- **Error Handling**: Comprehensive error management system

### Developer Experience
- **Fast Tests**: Jest with ES Module support
- **Linting**: ESLint with modern JavaScript rules
- **Auto-reload**: Nodemon for development
- **Logging**: Structured logging with Winston

## 🔒 Error Handling

The application includes a comprehensive error handling system:

- **Custom Error Classes**: Specific errors for different scenarios
- **Operational vs Programming Errors**: Clear distinction
- **User-Friendly Messages**: Formatted for Discord responses
- **Logging Integration**: Automatic error logging

## 📈 Future Enhancements

- [ ] Discord slash commands implementation
- [ ] Integration tests with test database
- [ ] Docker containerization
- [ ] CI/CD pipeline setup
- [ ] Performance monitoring
- [ ] Rate limiting implementation

## 🤝 Contributing

1. Follow the established architecture patterns
2. Maintain test coverage for new features
3. Use modern JavaScript practices
4. Follow SOLID principles
5. Add JSDoc comments for public APIs

## 📄 License

This project is licensed under the ISC License.