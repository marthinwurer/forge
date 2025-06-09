# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Forge is an open-source Magic: The Gathering rules engine and game client implemented in Java. The project has been in development since 2007 and provides a complete implementation of MTG rules with AI opponents, multiplayer support, and cross-platform compatibility.

## Build System

This is a Maven multi-module project requiring Java 17+ and Maven 3.8.1+.

### Essential Commands

```bash
# Clean build all modules
mvn clean compile

# Run tests (uses TestNG)
mvn test

# Run tests with GUI support (uses virtual framebuffer)
mvn test -Pheadless

# Build desktop application
mvn clean compile -Pwindows-linux

# Build Android version
mvn clean compile -Pandroid

# Run checkstyle (import order enforcement)
mvn checkstyle:check

# Package installer
cd forge-installer && mvn package
```

### Running the Application

```bash
# Desktop version
cd forge-gui-desktop/target/classes
java -jar forge-gui-desktop-1.6.63-SNAPSHOT.jar

# Mobile development version
cd forge-gui-mobile-dev/target/classes  
java -jar forge-gui-mobile-dev-1.6.63-SNAPSHOT.jar
```

## Architecture

The project follows a layered architecture with clear module dependencies:

### Core Modules (Bottom-Up)
- **forge-core**: Base utilities, card data structures, deck management
- **forge-game**: Complete MTG rules engine, game state management, player actions
- **forge-ai**: AI decision-making algorithms, Monte Carlo simulations, deck evaluation
- **forge-gui**: Shared GUI resources, network infrastructure, common UI components

### Platform Modules
- **forge-gui-desktop**: Java Swing-based desktop client
- **forge-gui-mobile**: LibGDX-based mobile implementation (shared logic)
- **forge-gui-mobile-dev**: Desktop version of mobile interface for development
- **forge-gui-android**: Android-specific packaging and resources
- **forge-gui-ios**: iOS-specific implementation

### Specialized Modules  
- **forge-lda**: Machine learning and statistical analysis
- **adventure-editor**: Tool for creating adventure mode content
- **forge-installer**: Application packaging and distribution

## Key Directories

- `forge-gui/res/`: Game resources including 25,000+ card definitions, AI profiles, game formats
- `forge-gui/res/cardsfolder/`: Individual card script implementations
- `forge-gui/res/editions/`: Set definitions and card lists by MTG expansion
- `forge-gui/res/ai/`: AI personality configurations
- `forge-gui/res/cube/` & `forge-gui/res/draft/`: Draft and sealed play formats

## Development Guidelines

### Code Quality
- Checkstyle enforces import organization (alphabetical, with proper grouping)
- All new features require corresponding test coverage using TestNG
- Card implementations follow established scripting patterns in `cardsfolder/`

### Module Dependencies
- Never add dependencies from lower layers to higher layers
- forge-core should remain independent of game logic
- GUI modules should not contain game rules logic
- AI modules should only depend on game state, not GUI components

### Card Implementation
- New cards go in `forge-gui/res/cardsfolder/[first-letter]/[card-name].txt`
- Follow existing card script format and use established keywords
- Set information goes in `forge-gui/res/editions/[set-code].txt`

### Testing
- GUI tests require the `headless` profile for CI environments
- Game logic tests should focus on rules interactions
- AI tests should verify decision-making algorithms

### Network and Multiplayer
- Network server runs on port 36743 by default
- Server configuration in forge-gui networking package
- Protocol handling in game state management classes

## Platform-Specific Notes

### Desktop (Swing)
- Main entry point: `forge.Forge` class
- UI components in `forge.screens` package hierarchy
- Platform-specific features in desktop-only modules

### Mobile (LibGDX)
- Entry point: mobile-specific application classes
- Shared game logic between mobile platforms
- Touch-optimized UI components and layouts
- Platform-specific packaging in android/ios modules

## AI System Architecture

Forge features a sophisticated AI system that provides challenging opponents with different personality profiles and advanced decision-making capabilities.

### AI Controller Hierarchy

**Main Classes:**
- `PlayerControllerAi` (forge-ai) - AI implementation of PlayerController interface
- `AiController` - Core AI "brain" that makes strategic decisions  
- `LobbyPlayerAi` - Manages AI player instances and profiles

**Architecture Pattern:**
- PlayerControllerAi delegates complex decisions to internal AiController
- AiController evaluates game states and chooses optimal actions
- Clear separation between AI interface (PlayerControllerAi) and logic (AiController)

### AI Decision-Making System

**Game State Evaluation:**
- `GameStateEvaluator` assigns numeric scores to complete game states
- Factors: life totals (2pts each), hand cards (5pts each), permanents (variable)
- Uses combat simulation to predict near-future outcomes
- Multi-layered evaluation: immediate impact, board position, card advantage

**Creature Evaluation (`CreatureEvaluator`):**
- Base value: 80 points + 20 for non-tokens
- Power/Toughness: Power × 15 + Toughness × 10  
- Keyword bonuses: Flying (+Power×10), First Strike (+10+Power×5), Double Strike (+10+Power×15)
- Protection abilities: Indestructible (+70), Hexproof (+35), Shroud (+30)
- Penalties: Defender (-Power×9-40), Can't untap (-50)

**Combat AI:**
- `AiAttackController` - Evaluates attacking decisions with aggression levels (0-6)
- `AiBlockController` - Calculates favorable trades and planeswalker protection
- Considers evasion abilities, combat tricks, and threat assessment

### AI Profiles and Difficulty

**Four Main Personalities:**
- **Default.ai** - Balanced strategy
- **Cautious.ai** - Conservative, defensive play  
- **Experimental.ai** - Tries riskier strategies
- **Reckless.ai** - Highly aggressive approach

**Configuration System (`AiProps`):**
- 100+ configurable properties control AI behavior
- Combat settings: attack aggression, trading willingness, combat trick usage
- Spell priorities: counterspell targeting, removal preferences
- Resource management: mana efficiency, card advantage optimization
- Risk thresholds: life total danger levels, threat evaluation

**Key Settings Examples:**
```
CHANCE_TO_ATTACK_INTO_TRADE ("40")     // 40% chance for trading attacks
MULLIGAN_THRESHOLD ("5")               // Keep hands with 5+ playable cards  
AI_IN_DANGER_THRESHOLD("4")           // Life < 4 considered dangerous
MIN_SPELL_CMC_TO_COUNTER ("0")        // Counter spells of any cost
```

### Advanced AI Features

**Rule-Based Decision Engine:**
- Uses sophisticated heuristics and evaluation functions for decision-making
- No reliance on game tree simulation (simulation mode is legacy and unreliable)
- Real-time evaluation of current game state without lookahead prediction
- Optimized for consistent performance and reliable decision-making

**AI Memory System (`AiCardMemory`):**
- Tracks revealed cards from opponent hands/libraries
- Remembers targeting choices to avoid repetition
- Monitors bounced cards to prevent immediate re-casting
- Memory categories: REVEALED_CARDS, TARGETED_THIS_TURN, BOUNCED_THIS_TURN

**Spell-Specific AI:**
- 100+ specialized AI classes in `forge.ai.ability` package
- Each major spell type has dedicated logic (CounterAi, DamageAi, etc.)
- Context-aware targeting and timing optimization
- Coordinated decision-making between different spell types

### AI Integration Points

**PlayerController Implementation:**
- AI implements all 100+ decision methods from PlayerController
- Delegates complex logic to AiController while handling simple responses directly
- Maintains game rule compliance and UI interaction requirements

**Performance Optimizations:**
- Fast heuristic-based evaluation prevents decision delays
- Caching systems for expensive evaluations within turns
- Efficient rule-based logic optimized for real-time gameplay
- Cheat shuffling available for testing and debugging

**Error Handling:**
- Fallback mechanisms default to simple heuristics if complex logic fails
- Illegal play prevention with action validation
- Extensive logging for AI behavior analysis and debugging

The AI system uses mature rule-based heuristics to provide varied and challenging gameplay through its profile system and comprehensive evaluation mechanisms, prioritizing consistency and reliability over complex simulation approaches.

This project demonstrates mature software engineering practices with clear separation of concerns, comprehensive testing, and professional build infrastructure.