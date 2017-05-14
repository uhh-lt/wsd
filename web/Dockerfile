#
FROM node:latest

# Build App
WORKDIR /usr/app/src
ADD package.json /usr/app/src/
RUN npm install

ARG public_url
ARG api_host
ARG external_api_endpoint
ARG image_api_name

ENV PUBLIC_URL="$public_url"
ENV REACT_APP_WSP_API_HOST="$api_host"
ENV REACT_APP_WSP_EXTERNAL_API_ENDPOINT="$external_api_endpoint"

ADD public/ /usr/app/src/public/
ADD src/ /usr/app/src/src/

RUN npm run build

ADD server.js /usr/app/src/server.js
ENV NODE_ENV="production"
EXPOSE 8080

CMD ["npm", "run", "server"]