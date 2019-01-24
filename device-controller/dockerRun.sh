#!/bin/bash
#This shell script will try to build device controller project and create docker image using it.

USAGE="Usage:$0 <device controller project root directory>";
DOCKER_IMAGE_NAME="device-controller";
read  -p "Enter your docker account name: " DOCKER_ACT && \
read -p "Enter your docker account password: " -s DOCKER_ACT_PASSWD;
[[ -z "$1" || ! -d "$1"  ]] && { echo $USAGE && exit 1;};
#{ (cd "$1" && mvn clean install >/dev/null;) || { echo "Failed to execute maven build"; exit 2; } ; } && \
{ (cd "$1" && mvn clean install ) || { echo "Failed to execute maven build"; exit 2; } ; } && \
cp "${1}device-controllers-ear/target/device-controllers-ear.ear" . && \
{ docker build -t "$DOCKER_ACT/$DOCKER_IMAGE_NAME:latest" -f DockerFile . || { echo "$?"; echo "Failed to build docker image"; exit 3; } ; } && \
{ docker login -u "$DOCKER_ACT" -p "$DOCKER_ACT_PASSWD" || { echo "Failed to login to docker hub account.Please check your docker hub credentials."; exit 4; } ; } && \
{ docker push "$DOCKER_ACT/$DOCKER_IMAGE_NAME" || { echo "Failed to push docker image to docker hub cloud"; docker logout; exit 5; } ; } && \
echo "Sucessfully uploaded docker image to docker hub" && exit;

