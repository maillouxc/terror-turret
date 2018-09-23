#!/usr/bin/env bash

# This file contains a simple script to start the UV4L video stream

echo "Killing any existing UV4L stream stuff that may get in the way..."
sudo /usr/bin/pkill uv4l || echo "\n You must run this script as root!\n"

echo "Starting UV4L server on port 9000..."
uv4l --driver uvc \
    --verbosity 8 \
	--device-id 046d:0825 \
	--frame-buffers 2 \
	--auto-video_nr
	
echo "Turret stream *should* be up and running."
