#!/usr/bin/env bash

# This file contains a simple script to start the UV4L video stream
# and the turret control software

echo "Killing any existing UV4L stream stuff that may get in the way..."
sudo /usr/bin/pkill uv4l

# Give some time so that hopefully the process is dead before continuing
sleep 3

# Start UV4L video streaming server
echo "Starting UV4L server on port 9000..."
uv4l --driver uvc \
	--config-file "/home/pi/code/terror-turret/pi/uv4l-config.conf" \
	--driver-config-file "/home/pi/code/terror-turret/pi/uv4l-config.conf"

echo -e "\nTurret stream *should* be up and running!\n"

echo "If the device name just printed above isn't /dev/video0, it didn't work."
echo "It can sometimes help to run this script again in that case."

# Start the turret control software
echo -e "\nStarting turret control software on port 9001...\n"
sudo python3 /home/pi/code/terror-turret/pi/rpi-turret-manager.py
echo "Both services *should* be now up and running!"
