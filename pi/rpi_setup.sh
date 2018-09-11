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
  printf "SUCCESS after ${runtime} seconds (${runtime_minutes} minutes)."
  printf "${OUTPUT_NOCOLOR}"
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
echo "COMPLETE\n"

echo "Installing needed packages..."
sudo apt-get update || exiterror "Failed to run apt-get update."
echo "Installing Arduino IDE..."
sudo apt-get install arduino || exiterror
echo "Installing Git..."
sudo apt-get install git || exiterror
echo "Installing pip package manager for Python..."
sudo apt-get install python-pip || exiterror
echo "Installing python-serial..."
sudo apt-get install python-serial || exiterror
echo "Finished installing needed packages.\n"

success

