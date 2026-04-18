# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WhatsAppCleaner is an Android app for cleaning redundant WhatsApp media and files. Single-module Kotlin project using Jetpack Compose, targeting API 24-35. Licensed under GPLv3.

## Build & Development Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK (minified, shrunk)
./gradlew assemblePlay           # Build Google Play variant (.play suffix)
./gradlew testDebug              # Run unit tests
./gradlew spotlessCheck          # Check formatting (ktlint + google-java-format)
./gradlew spotlessApply          # Auto-fix formatting
```

CI runs `spotlessCheck` then `testDebug` on every push/PR.

## Build Variants

- **debug**: `com.vishnu.whatsappcleaner.debug`
- **release**: `com.vishnu.whatsappcleaner` (ProGuard minification + resource shrinking)
- **play**: `com.vishnu.whatsappcleaner.play` (Google Play distribution, based on release)

## Architecture

MVVM + Repository pattern, single-Activity app with Compose Navigation.

- **MainActivity** — sole Activity; sets up navigation between 3 screens (Permission → Home → Details)
- **MainViewModel** — manages all UI state via `StateFlow`; handles sorting, date filtering, multi-select, batch deletion
- **FileRepository** — static file system operations (directory scanning, file listing, deletion, size formatting)
- **StoreData** — DataStore Preferences wrapper for persisting user settings (grid view toggle, WhatsApp home URI)

### State Management

`ViewState<T>` sealed class (Loading/Success/Error) wraps async data. `Target` sealed class (Received/Sent/Private) categorizes file listings.

### Screens

1. **PermissionScreen** — storage permission request and WhatsApp directory picker
2. **HomeScreen** — directory listing showing WhatsApp media folders with sizes
3. **DetailsScreen** — file browser with tabs (Received/Sent/Private), sorting, date filtering, multi-select delete

### Storage Access

- API 24-29: `READ/WRITE_EXTERNAL_STORAGE` permissions
- API 30+: `MANAGE_EXTERNAL_STORAGE` with SAF URI tree picker

## Code Formatting

Spotless enforces formatting on all `.kt` and `.java` files:
- Kotlin: ktlint v1.5.0
- Java: Google Java Format (AOSP style)
- GPL v3 license header applied from `spotless-header` file

All source files must have the license header. Run `spotlessApply` before committing.
