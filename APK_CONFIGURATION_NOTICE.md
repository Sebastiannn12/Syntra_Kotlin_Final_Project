# Debug APK configuration notice

`release/AccountPortal-debug.apk` is a successfully compiled debug build and is included for installation testing.

It currently uses the safe placeholder API URL `https://example.com/android_sample_api/` because no personal hosting URL or database credentials were provided. Login, registration, dashboard data, photos, and user management require the hosted PHP API.

Before the final classroom submission:

1. Follow `HOSTING_SETUP.md` to import the SQL database and upload the PHP API.
2. Set `API_BASE_URL=https://YOUR_DOMAIN/android_sample_api/` in `gradle.properties`.
3. Rebuild with `./gradlew assembleDebug`.
4. Replace this APK with the newly built `app/build/outputs/apk/debug/app-debug.apk`.

Never put MySQL credentials inside the Android app or submit your private `config.php`.
