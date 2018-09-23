#!/usr/bin/env bash

# This file contains a simple script to start the UV4L video stream

echo "Killing any existing UV4L stream stuff that may get in the way..."
sudo /usr/bin/pkill uv4l || echo "You must run this script as root!"

# Give some time so that hopefully the process is dead before continuing
sleep 3

echo "Starting UV4L server on port 8080..."
uv4l --driver uvc \
	--device-id 046d:0825 \
	--frame-buffers 2 \
	--auto-video_nr \
	--config-file "./uv4l-config.conf" \
	--driver-config-file "uv4l-config.conf"
	
echo -e "\nTurret stream *should* be up and running!\n"

echo "If the device name just printed above isn't /dev/video0, it didn't work."
echo "It can sometimes help to run this script again in that case."
