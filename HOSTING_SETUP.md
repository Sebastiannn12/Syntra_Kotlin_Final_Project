# Railway deployment

The final architecture remains Android Kotlin -> Retrofit -> PHP API on Railway -> MySQL on Railway. Do not commit or place database credentials in the Android app.

## 1. Confirm the Railway MySQL service

The imported database must contain both `tblusers` and `api_tokens`. The PHP service will read the connection through these environment variables:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASS
```

If the database service is named `MySQL`, define reference variables on the PHP service as follows:

```text
DB_HOST=${{MySQL.MYSQLHOST}}
DB_PORT=${{MySQL.MYSQLPORT}}
DB_NAME=${{MySQL.MYSQLDATABASE}}
DB_USER=${{MySQL.MYSQLUSER}}
DB_PASS=${{MySQL.MYSQLPASSWORD}}
```

Use the actual Railway service name if yours is not named `MySQL`.

## 2. Deploy the PHP service

1. Create a Railway service from this GitHub repository in the same project as MySQL.
2. Railway automatically detects the root `Dockerfile` and `railway.json`.
3. Add the five database reference variables above to the PHP service.
4. Generate a public domain under the PHP service's **Settings > Networking**.
5. The health check at `/android_sample_api/health.php` must return:

```json
{"success":true,"message":"API and database are ready"}
```

6. For profile photos to survive redeployments, attach a Railway Volume to the PHP service at this exact mount path:

```text
/var/www/html/android_sample_api/uploads
```

The container fixes the mounted folder permissions when it starts.

## 3. Configure Android

Add the generated HTTPS domain to the project-level `gradle.properties`, including the path and final slash:

```properties
API_BASE_URL=https://YOUR_DOMAIN.up.railway.app/android_sample_api/
```

Then sync, rebuild, and reinstall the app. The URL is compiled into `BuildConfig`, while database credentials remain only on Railway.

## 4. Verify the deployment

Run registration first to create a test account and token, then verify login, current user, list/search/view, create/update/disable/restore, password change/reset, dashboard statistics, and multipart photo upload. The protected endpoints should return `Authentication required` when called without a bearer token.

## Legacy PHP hosting instructions

The steps below are retained only for compatibility with conventional cPanel/InfinityFree hosting. They are not needed for the Railway deployment.

### Create the hosted database

1. Sign in to a PHP/MySQL hosting provider such as InfinityFree or any cPanel host.
2. Open **MySQL Databases** and create one database and one database user.
3. Record the exact database host, database name, username, and password. Free hosts often prefix the database and username with your account name; use the full values shown by the host.
4. Open the host's **phpMyAdmin** for that database.
5. Select the database, choose **Import**, select `server/android_sample.sql`, and run the import.
6. Confirm that both `tblusers` and `api_tokens` exist.

### Configure the PHP API

1. On your computer, duplicate `server/android_sample_api/config.example.php` and rename the copy to `config.php`.
2. Open `config.php` and replace the four placeholder values with the exact database credentials from the host.
3. Keep `config.php` private. It is already excluded by `.gitignore`.
4. In the hosting file manager, create a public folder named `android_sample_api` inside `htdocs`, `public_html`, or the web root used by the provider.
5. Upload these files into that folder:
   - `.htaccess`
   - `auth.php`
   - `db.php`
   - `password.php`
   - `stats.php`
   - `upload.php`
   - `users.php`
   - the `uploads` folder and its `.htaccess`
   - your private `config.php`
6. Do not upload `config.example.php`.

### Test the hosted API

1. Visit `https://YOUR_DOMAIN/android_sample_api/users.php` in a browser.
2. A correct protected API returns JSON similar to:

```json
{"success":false,"message":"Authentication required"}
```

3. If you see a PHP database error, recheck the host, database name, username, password, and whether the database user is assigned to the database.
4. If the browser downloads PHP or shows PHP source code, the hosting plan does not execute PHP; use a PHP-enabled host.
5. If photo uploads fail, make sure PHP can write to the `uploads` folder. Most hosts use directory permission `755`; do not use `777` unless the provider explicitly requires it.

### Point Android to the hosted API

1. Open the project-level `gradle.properties`.
2. Add the real HTTPS URL, including the final slash:

```properties
    API_BASE_URL=https://YOUR_DOMAIN/android_sample_api/
```

3. In Android Studio, choose **File > Sync Project with Gradle Files**.
4. Choose **Build > Rebuild Project**.
5. Run the app. The login screen displays the configured server URL at the bottom.

### Create the first account and verify all requirements

1. Tap **Create an account**.
2. Register with a unique username, a valid email address, and a password containing at least eight characters, uppercase, lowercase, a number, and a symbol.
3. Confirm that registration signs you in and that a row appears in `tblusers` in phpMyAdmin. The password column must contain a hash, not the password you typed.
4. Open **Users** from the toolbar.
5. Test search, sorting, pull-to-refresh, view, add, edit, profile-photo upload, disable, restore, and password reset.
6. Open **Edit profile** and **Change password** from the top-right menu. After changing the password, sign in again because all previous session tokens are intentionally revoked.
7. Sign out, then sign back in using either the username or email.
8. Confirm a row exists in `api_tokens`; this proves that authentication uses the hosted database.

## Troubleshooting

- **404:** Verify the public folder name and letter case.
- **HTTP 500 / Database error:** Correct `config.php` or assign the database user to the database.
- **Session expired immediately:** Ensure the host's MySQL clock works and `api_tokens.expires_at` is a `DATETIME`.
- **Authentication required after login:** Confirm `.htaccess` was uploaded and that the host forwards the `Authorization` header.
- **Cleartext error:** Use an HTTPS URL. This project intentionally blocks non-HTTPS traffic.
- **App still uses the old URL:** Sync Gradle, rebuild, and reinstall the APK after changing `API_BASE_URL`.
