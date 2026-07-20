FROM php:8.3-apache

RUN docker-php-ext-install pdo_mysql \
    && a2enmod rewrite headers

COPY server/apache/railway.conf /etc/apache2/conf-available/railway.conf
RUN a2enconf railway

COPY server/android_sample_api/ /var/www/html/android_sample_api/

RUN mkdir -p /var/www/html/android_sample_api/uploads \
    && chown -R www-data:www-data /var/www/html/android_sample_api/uploads

ENV PORT=8080
EXPOSE 8080

CMD ["sh", "-c", "mkdir -p /var/www/html/android_sample_api/uploads && chown -R www-data:www-data /var/www/html/android_sample_api/uploads && sed -ri 's/Listen 80/Listen '${PORT:-8080}'/' /etc/apache2/ports.conf && sed -ri 's/<VirtualHost \*:80>/<VirtualHost *:'${PORT:-8080}'>/' /etc/apache2/sites-available/000-default.conf && apache2-foreground"]
