#!/bin/sh

set -eu

railway_port="${PORT:-8080}"
upload_directory="/var/www/html/android_sample_api/uploads"

mkdir -p "$upload_directory"
chown -R www-data:www-data "$upload_directory"

sed -ri "s/Listen 80/Listen ${railway_port}/" /etc/apache2/ports.conf
sed -ri "s/<VirtualHost \*:80>/<VirtualHost *:${railway_port}>/" /etc/apache2/sites-available/000-default.conf

exec apache2-foreground
