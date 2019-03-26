#!/usr/bin/env bash

# This script sets up the Raspberry Pi 3B+ as needed for our project

starttime=$(date +%s)

# Colors that can be used in the output
OUTPUT_RED='\033[0;31m'
OUTPUT_GREEN='\033[0;32m'
OUTPUT_NO_COLOR='\033[039m'

# Prints the provided string red to indicate an error
error() {
  printf "${OUTPUT_RED}"
  printf "$@\n"
  printf "${OUTPUT_NO_COLOR}"
}

# Just like error(), but exits with exit code 1
exiterror() {
  error "$@"
  endtime=$(date +%s)
  runtime=$((endtime - starttime))
  runtime_minutes=$((runtime/60))
  error "FAILED after ${runtime} seconds (${runtime_minutes} minutes)."
  exit 1
}

# Called when the script finished successfully - prints a message in green
success() {
  printf "${OUTPUT_GREEN}"
  printf "Setup completed successfully; cya!\n"
  endtime=$(date +%s)
  runtime=$((endtime - starttime))
  runtime_minutes=$((runtime/60))
  printf "SUCCESS after ${runtime} seconds (${runtime_minutes} minutes).
  printf "${OUTPUT_NO_COLOR}"
  printf "\n Rebooting the Pi to make sure everything works as expected..."
  printf "You have 5 seconds to cancel with Ctrl + C."
  sleep 5
  sudo reboot
  exit 0
}

printf "\nNow setting up RPi 3B+ for use with Terror-Turret...\n\n"
sleep 1s

# First make sure the user is root so we can do what we need to
echo "Ensuring you have root privileges..."
if [[ $EUID != 0 ]]
then
  exiterror "This script must be run as root. Please re-run with sudo."
fi

echo "Installing and updating needed packages..."
sudo apt-get update || exiterror "Failed to run apt-get update."
sudo apt-get upgrade || exiterror "Failed to run apt-get upgrade."
echo "Installing Git if not already present..."
sudo apt-get install git || exiterror

echo -e "\nInstalling pip package manager for Python..."
sudo apt-get install python-pip || exiterror
echo "Installing needed Python packages..."
sudo pip3 install pyserial || exiterror "Unable to install pyserial!"
sudo pip3 install colorama || exiterror "Unable to install colorama!"
sudo pip3 install simplewebsocketserver || exiterror "Unable to install simplewebsocketserver!"

echo -e "\nInstalling UV4L..."
curl http://www.linux-projects.org/listing/uv4l_repo/lpkey.asc | sudo apt-key add -
echo "deb http://www.linux-projects.org/listing/uv4l_repo/raspbian/stretch stretch main" | sudo tee -a /etc/apt/sources.list
sudo apt-get install uv4l || exiterror
sudo apt-get install uv4l-raspicam || exiterror
sudo apt-get install uv4l-raspicam-extras || exiterror
sudo apt-get install uv4l-decoder || exiterror
sudo apt-get install uv4l-encoder || exiterror
sudo apt-get install uv4l-renderer || exiterror
sudo apt-get install uv4l-mjpegstream || exiterror
sudo apt-get install uv4l-server || exiterror
sudo apt-get install uv4l-webrtc || exiterror
sudo apt-get install uv4l-demos || exiterror
sudo apt-get install uv4l-uvc || exiterror

echo -e "Finished installing all needed packages.\n"

echo -e "\nCloning project code from github..."
sudo mkdir -p code
sudo git clone https://www.github.com/maillouxc/terror-turret.git ./code/terror-turret

echo -e "\nYou need to edit rc.local to run the installed program on boot...\n"
echo "We can\'t easily do this for you, sorry..."

success
