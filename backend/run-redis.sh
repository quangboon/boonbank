#!/usr/bin/env bash
set -e

CONTAINER=bbank-redis
IMAGE=redis:7-alpine
VOLUME=bbank-redis-data
HOST_PORT=6389

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
  echo "Tạo container Redis mới..."
  docker run -d \
    --name ${CONTAINER} \
    -p ${HOST_PORT}:6379 \
    -v ${VOLUME}:/data \
    ${IMAGE} redis-server --appendonly yes
fi

echo ""
echo "Kết nối redis-cli: docker exec -it ${CONTAINER} redis-cli"
echo "Host port: ${HOST_PORT}"
