FROM node:16-alpine

WORKDIR /app

ARG APP_ID
ARG PUBLIC_FOLDER=/app/public/${APP_ID}/compiled

COPY ./package.json /app/
COPY ./webpackAssets.json /app/
COPY ./dist/server /app
COPY ./dist/client/stats.json /app
COPY ./dist/client ${PUBLIC_FOLDER}

ENV APP_ID ${APP_ID}

ENTRYPOINT [ "node", "/app/server.js" ]
