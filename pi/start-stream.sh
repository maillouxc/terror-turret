#!/usr/bin/env bash

# This file contains a simple script to start the raspicam stream
# For whatever reason, we've screwed something up and it doesn't start
# by default anymore, and we can't figure out how to fix it.
# So instead, just run this script whenever you need to start the stream.

echo "Starting UV4L server on port 9000..."

uv4l --config-file='uv4l-config.conf'

