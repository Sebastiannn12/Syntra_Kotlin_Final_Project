FROM php:8.3-apache

RUN docker-php-ext-install pdo_mysql \
    && a2enmod rewrite headers

COPY server/apache/railway.conf /etc/apache2/conf-available/railway.conf
COPY server/apache/start-railway.sh /usr/local/bin/start-railway
RUN a2enconf railway \
    && chmod +x /usr/local/bin/start-railway

COPY server/android_sample_api/ /var/www/html/android_sample_api/

RUN mkdir -p /var/www/html/android_sample_api/uploads \
    && chown -R www-data:www-data /var/www/html/android_sample_api/uploads

ENV PORT=8080
EXPOSE 8080

CMD ["/usr/local/bin/start-railway"]
