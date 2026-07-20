# Syntra - Kotlin + PHP + MySQL

Syntra is a complete Internet-backed user account management app built from the provided Android sample and the required course guide.

## What is included

- Login with username or email, Remember Me, automatic session login, and a password-visibility toggle
- Registration with duplicate username/email checks, a password-strength meter, and matching client/server validation
- Persistent or in-memory bearer-token sessions managed with `SharedPreferences`
- Searchable, sortable, pull-to-refresh user directory using `RecyclerView` and `ListAdapter`
- View, add, edit, soft-disable, restore, and reset-password account controls
- Current-user profile editing and a dedicated change-password screen
- JPG, PNG, or WEBP profile-picture upload to the hosted PHP server
- Live dashboard with active/disabled totals, recent users, current account, and server status
- Friendly offline/server errors, retry actions, progress indicators, confirmation dialogs, and success notifications
- Retrofit repository and lifecycle-aware ViewModel state
- PHP PDO API with prepared statements and password hashing
- MySQL schema with unique username/email constraints and expiring API tokens
- Railway-ready PHP/Apache container, database health check, and environment-variable configuration
- Material 3 home, dashboard, notification, authentication, and management screens with an Android splash screen

The Android app never contains MySQL credentials and never connects directly to MySQL. It communicates only with the hosted PHP API.

## Project map

- `app/` - Android Kotlin/XML application
- `server/android_sample_api/` - PHP API to upload to hosting
- `server/android_sample.sql` - database schema to import in phpMyAdmin
- `app/build/outputs/apk/debug/app-debug.apk` - locally compiled debug APK
- `release/AccountPortal-debug.apk` - convenient copy included inside the complete submission ZIP

## Configure the hosted API URL

After the server is online, add this line to the project-level `gradle.properties`:

```properties
API_BASE_URL=https://YOUR_DOMAIN/android_sample_api/
```

The URL must use HTTPS and must end with `/`. Rebuild the app after changing it.

For Railway, use `https://YOUR_DOMAIN.up.railway.app/android_sample_api/`. See `HOSTING_SETUP.md` for the exact `DB_*` reference variables and persistent upload volume path.

## Local verification commands

```bash
php -l server/android_sample_api/db.php
php -l server/android_sample_api/auth.php
php -l server/android_sample_api/users.php
php -l server/android_sample_api/password.php
php -l server/android_sample_api/stats.php
php -l server/android_sample_api/upload.php
GRADLE_USER_HOME="$PWD/.gradle-user" ./gradlew testDebugUnitTest assembleDebug lintDebug
```

## API routes

- `POST auth.php?action=register`
- `POST auth.php?action=login`
- `GET auth.php?action=me`
- `POST auth.php?action=logout`
- `GET users.php`
- `GET users.php?id=ID`
- `POST users.php`
- `PUT users.php?id=ID`
- `DELETE users.php?id=ID` (soft-disables the account)
- `POST users.php?action=restore&id=ID`
- `POST password.php?action=change`
- `POST password.php?action=reset&id=ID`
- `GET stats.php`
- `POST upload.php` using multipart field `photo`
- `GET health.php` (deployment/database health check; no authentication required)

All routes except login and registration require `Authorization: Bearer TOKEN`.
