#!/bin/sh

set -eu

railway_port="${PORT:-8080}"
upload_directory="/var/www/html/android_sample_api/uploads"

mkdir -p "$upload_directory"
chown -R www-data:www-data "$upload_directory"

# Railway's runtime can restore Apache module links from the base image layer.
# Select exactly one MPM before starting mod_php.
rm -f /etc/apache2/mods-enabled/mpm_event.load \
    /etc/apache2/mods-enabled/mpm_event.conf \
    /etc/apache2/mods-enabled/mpm_worker.load \
    /etc/apache2/mods-enabled/mpm_worker.conf
a2enmod mpm_prefork >/dev/null

sed -ri "s/Listen 80/Listen ${railway_port}/" /etc/apache2/ports.conf
sed -ri "s/<VirtualHost \*:80>/<VirtualHost *:${railway_port}>/" /etc/apache2/sites-available/000-default.conf

apache2ctl configtest
exec apache2-foreground
