FROM php

RUN apt-get update && apt-get install -y \
  curl \
  git

# https://www.digitalocean.com/community/tutorials/how-to-install-and-use-composer-on-ubuntu-14-04
RUN curl -sS https://getcomposer.org/installer | php -- --install-dir=/usr/local/bin --filename=composer

RUN mkdir /opt/downloader

ADD ./download.php /opt/downloader
ADD ./.env /opt/downloader
ADD ./print-statistics.sh /opt/downloader

RUN mkdir /opt/downloader/var
VOLUME /opt/downloader/var

ADD ./composer.json /opt/downloader/composer.json

WORKDIR /opt/downloader/
RUN composer install

CMD  ["php",  "-f", "download.php", "&&", "./print-statistics.sh"]