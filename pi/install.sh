#!/bin/bash
# version: 0.4.14.19

# This script performs a fresh install of the turret's Linux software.
# The turret software has only been tested on a Raspberry Pi 3B+ running
# Raspbian 9 Stretch, but it should work with other Debian based systems.
# See https://github.com/maillouxc/terror-turret/pi/README.md for more details.

starttime=$(date +%s)
DEFAULT_PROJECT_NAME="terror-turret"
DEFAULT_PASSWORD="Z"
OS_VERSION=${cat /etc/os-release | grep 'VERSION=' | awk -F'[(|)]' '{print $2}'}
BASE_PACKAGES="git python-pip"
UV4L_PACKAGES="uv4l uv4l-decoder uv4l-encoder uv4l-renderer "\
              "uv4l-mjpegstream uv4l-server uv4l-webrtc"\
              "uv4l-demos uv4l-uvc"
quiet=false
prompts=true

# if -q is passed as an argument, then perform a quiet install. No prompts, 
#   no prints. Output is redirected to /tmp/turret_install.log
[[ ! -z $1 ]] && [[ $1 -eq "-q" ]] && quiet=true && prompts=false
[[ $quiet ]] && exec &> /tmp/turret_install.log

# if -d is passed as an argument, then use default settings. No prompts.
[[ ! -z $1 ]] && [[ $1 -eq "-d" ]] && prompts="False"

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
  printf "${OUTPUT_NO_COLOR}"
  printf "\n Rebooting the Pi to make sure everything works as expected..."
  printf "You have 5 seconds to cancel with Ctrl + C."
  sleep 5
  sudo reboot
  exit 0
}

set_project_name() {
  # NOTE: there is no validation or error checking, this could break things
  echo -e "Please enter a project name or press Enter for default."
  read -p "($DEFAULT_PROJECT_NAME) : " project_name
  [[ -z "$project_name" ]] && project_name="$DEFAULT_PROJECT_NAME"
}

set_turret_password() {
  # NOTE: there is no validation or error checking, this could break things
  echo -e "Please enter a password for the android app or press Enter for default."
  echo -n "($DEFAULT_PASSWORD) : "
  read turret_password
  [[ -z "$turret_password" ]] && turret_password="$DEFAULT_PASSWORD"
}

printf "\nNow setting up RPi 3B+ for use with Terror-Turret...\n\n"
sleep 1s

echo "Installing system updates..."
sudo apt update && sudo apt upgrade -y || exiterror "apt failed to update and upgrade."
echo "Installing git and pip if needed..."
sudo apt install $BASE_PACKAGES -y || exiterror "apt failed to install $BASE_PACKAGES."

echo "Installing needed Python packages..."
sudo pip3 install pyserial || exiterror "Unable to install pyserial!"
sudo pip3 install colorama || exiterror "Unable to install colorama!"
sudo pip3 install simplewebsocketserver || exiterror "Unable to install simplewebsocketserver!"

echo -e "\nInstalling UV4L..."
curl http://www.linux-projects.org/listing/uv4l_repo/lpkey.asc | apt-key add -
echo "deb http://www.linux-projects.org/listing/uv4l_repo/raspbian/stretch stretch main" | tee -a /etc/apt/sources.list
sudo apt install $UV4L_PACKAGES -y || exiterror

echo -e "Finished installing all needed packages.\n"

# ask user if they want to use a different project name
set_project_name

# ask user for a password for the turret manager
set_turret_password

# helper vars
TMP_DIR="/tmp/code/$DEFAULT_PROJECT_NAME/"
INSTALL_DIR="/usr/share/$project_name/"
DOC_DIR="/usr/share/doc/$project_name/"
CONFIG_DIR="/etc/$project_name"

echo -e "\n Cloning project code from github..."

mkdir /tmp/code || exiterror
git clone https://www.github.com/maillouxc/terror-turret.git $TMP_DIR || exiterror

# create needed project directories
sudo mkdir $INSTALL_DIR
sudo mkdir $DOC_DIR
sudo mkdir $CONFIG_DIR

# update project files with custom variables
sed -i 's/terror-turret/$project_name/g' $TMP_DIR/pi/turret.service
sed -i 's/terror-turret/$project_name/g' $TMP_DIR/pi/turret-manager.py
sed -i 's/terror-turret/$project_name/g' $TMP_DIR/pi/turret
sed -E -i 's/(password\x27\s*:\s*")(\w*)/\1${password}/g' $TMP_DIR/pi/turretManagerConfig.conf

# install turret documents
cp $TMP_DIR/LICENSE $DOC_DIR/LICENSE
cp $TMP_DIR/README.md $DOC_DIR/$project_name-about.md
cp $TMP_DIR/pi/README.md $DOC_DIR/$project_name-tls.md

# install control scripts
cp $TMP_DIR/pi/turret-manager.py $INSTALL_DIR/turret-manager.py
cp $TMP_DIR/pi/turretManagerConfig.py $INSTALL_DIR/turretManagerConfig.py
cp $TMP_DIR/pi/turret $INSTALL_DIR/turret
cp $TMP_DIR/pi/uv4l-config.conf $INSTALL_DIR/uv4l-config.conf
sudo cp $TMP_DIR/pi/turret.service /etc/systemd/system/turret.service
sudo mv /etc/uv4l/uv4l-uvc.conf /etc/uv4l/uv4l-uvc.conf.bak
cp $TMP_DIR/pi/shotgun_racking.wav $INSTALL_DIR/shotgun_racking.wav

# update needed permissions
chmod +x $INSTALL_DIR/turret
sudo chmod 644 /etc/systemd/system/$project_name.service

# symlink config files
sudo ln -s $INSTALL_DIR/turretManagerConfig.py $CONFIG_DIR/turretManagerConfig.py
sudo ln -s $INSTALL_DIR/turret /usr/local/bin/turret
sudo ln -s $INSTALL_DIR/uv4l-config.conf /etc/uv4l/uv4l-uvc.conf
sudo ln -s $INSTALL_DIR/shotgun_racking.wav $INSTALL_DIR/turret_ready.wav

# start the turret service 
sudo systemctrl daemon-reload
sudo systemctrl enable turret.service
sudo systemctrl start turret.service

success