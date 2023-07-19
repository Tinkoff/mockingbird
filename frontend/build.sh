#!/bin/sh

set -ex

export PORT=3000
export APP_ID=mockingbird
export NODE_ENV=production
export RELATIVE_PATH=/mockingbird
export ASSETS_FOLDER_NAME=/assets
export ASSETS_PREFIX=/mockingbird/assets/
export MOCKINGBIRD_API=/api/internal/mockingbird
export MOCKINGBIRD_EXEC_API=/api/mockingbird/exec
export DEBUG_PLAIN=true

rm -rf ./env.development.js
rm -rf ./dist/out${ASSETS_FOLDER_NAME}
mkdir -p ./dist/out${ASSETS_FOLDER_NAME}
npm install
npm run build

cp -r ./dist/static${RELATIVE_PATH}/. ./dist/out
cp -a ./dist/client/. ./dist/out${ASSETS_FOLDER_NAME}
