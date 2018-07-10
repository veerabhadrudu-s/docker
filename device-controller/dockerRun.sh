#!/bin/bash


earFilePath=${HOME}"/.m2/repository/com/hpe/iot/dc/device-controllers-ear/0.0.1-SNAPSHOT/device-controllers-ear-0.0.1-SNAPSHOT.ear"

echo "Expected ear in file path - " $earFilePath

if [ -f $earFilePath ]
then
	echo "Ear file exists in the lccal repository"
else
	echo "Expected ear in file path - " $earFilePath " doesn't exist in maven local repository."
	exit 0
fi

echo pwd
cp ~/.m2/repository/com/hpe/iot/dc/device-controllers-ear/0.0.1-SNAPSHOT/device-controllers-ear-0.0.1-SNAPSHOT.ear .
mv device-controllers-ear-0.0.1-SNAPSHOT.ear device-controllers-ear.ear
docker build -t veerabhadrudu/device-controller:latest -f DockerFile .

