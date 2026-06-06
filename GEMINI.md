# MatchSimulation

A Java-based project for simulating matches, built with Gradle.

## Project Overview

*   **Technology Stack:** Java, Gradle.
*   **Testing Framework:** JUnit 6.
*   **Main Entry Point:** `com.pebble.mvp.Main`.

## Building and Running

### Prerequisites

*   JDK 17 or higher recommended.
*   Gradle (provided via `gradlew` wrapper).

### Key Commands

| Task | Command |
| :--- | :--- |
| **Build Project** | `./gradlew build` |
| **Clean Build** | `./gradlew clean build` |
| **Run Tests** | `./gradlew test` |
| **Run Application** | `./gradlew run` (Note: requires `application` plugin in `build.gradle`) |
| **Execute Main Class** | `java -cp build/classes/java/main com.pebble.mvp.Main` (after build) |

> **Note:** To use `./gradlew run`, ensure the `application` plugin is applied in `build.gradle` with `mainClass = 'com.pebble.mvp.Main'`.

## Development Conventions

*   **Package Naming:** Follows `com.pebble.mvp` convention.
*   **Testing:** Use JUnit 5 for unit and integration tests. Place tests in `src/test/java`.
*   **Code Style:** Standard Java coding conventions.

## Project Structure

*   `src/main/java`: Application source code.
*   `src/test/java`: Test source code.
*   `build.gradle`: Project build configuration and dependencies.
*   `settings.gradle`: Gradle project settings.
