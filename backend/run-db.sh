#!/usr/bin/env bash
set -e

CONTAINER=bbank-db
IMAGE=bbank-postgres
DB_NAME=bbank
VOLUME=bbank-data
HOST_PORT=5437

if [ "$1" = "--fresh" ]; then
  echo "Xoá container và volume cũ..."
  docker rm -f ${CONTAINER} 2>/dev/null || true
  docker volume rm ${VOLUME} 2>/dev/null || true
fi

if [ "$(docker ps -a -q -f name=^/${CONTAINER}$)" ]; then
  if [ "$(docker ps -q -f name=^/${CONTAINER}$)" ]; then
    echo "Container '${CONTAINER}' đang chạy. Đang connect..."
  else
    echo "Container '${CONTAINER}' đã tồn tại, đang start lại..."
    docker start ${CONTAINER}
  fi
else
  echo "Build image và tạo container mới..."
  docker build -t ${IMAGE} .
  docker run -d \
    --name ${CONTAINER} \
    -p ${HOST_PORT}:5432 \
    -v ${VOLUME}:/var/lib/postgresql/data \
    ${IMAGE}
fi

echo ""
echo "Kết nối psql: docker exec -it ${CONTAINER} psql -U postgres -d ${DB_NAME}"
