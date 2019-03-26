#!/usr/bin/env python3

# This Python program is the main Rpi control program for the turret


import sys
import argparse
import serial
import serial.tools.list_ports
from threading import Thread
import wave
import time
from time import sleep
import pymedia.audio.sound as sound
import colorama
from colorama import Fore
from colorama import Style
from SimpleWebSocketServer import SimpleWebSocketServer
from SimpleWebSocketServer import WebSocket


CMD_FIRE = 0x21
CMD_STOP_FIRE = 0x22
CMD_SAFETY_ON = 0x23
CMD_SAFETY_OFF = 0x24
CMD_REBOOT = 0x25
CMD_ROTATE_LEFT_MAX = 0x26
CMD_ROTATE_ZERO = 0x30
CMD_ROTATE_RIGHT_MAX = 0x3A
CMD_PITCH_DOWN_MAX = 0x3B
CMD_PITCH_ZERO = 0x45
CMD_PITCH_UP_MAX = 0x4F

SERIAL_BAUD_RATE = 9600

turret_serial_port = None

arduino_serial_conn = serial.Serial()

in_test_mode = False

# Used to know when are about to exit, to allow threads to clean up things
exiting = False


def main():
    colorama.init()
    parse_command_line_arguments()
    print("\nTurret manager software started.\n")
    establish_connection_to_turret()

    logging_thread = Thread(target = SerialLoggingThread)
    logging_thread.start()

    if testmode:
        test_turret_commands()
        cleanup()
        exit(0)

    # Lets the user know the gun is ready
    # Doing it after the server is ready is much harder due to threading sadly
    play_shotgun_racking_sound()
    init_incoming_commands_server()
    cleanup()
    exit(0)


def parse_command_line_arguments():
    program_description = "Main control software for the Terror Turret."
    parser = argparse.ArgumentParser(description = program_description)
    parser.add_argument(
        '--test-mode',
        type = bool,
        default = False,
        dest = 'testmode',
        help = "Runs the test script instead of normal program")
    parser.add_argument(
        '--serial-port',
        default = '/dev/ttyUSB0',
        dest = 'serialport',
        help = "The name of the serial port to connect from.")
    parsed_args = parser.parse_args()

    global testmode
    testmode = parsed_args.testmode

    global turret_serial_port
    turret_serial_port = parsed_args.serialport


def cleanup():
    global exiting
    exiting = True
    # Allow threads to have a moment to react
    sleep(1)
    arduino_serial_conn.close()
    print("\nTurret manager software exited.\n")
    colorama.deinit()


def establish_connection_to_turret():
    print("Available serial ports: ")
    for port in serial.tools.list_ports.comports():
        print(str(port))
    print("")

    print("Attempting to connect to turret on " + turret_serial_port + "...")
    try:
        # The serial port takes some time to init
        arduino_serial_conn.baudrate = SERIAL_BAUD_RATE
        arduino_serial_conn.port = turret_serial_port
        arduino_serial_conn.timeout = 2
        arduino_serial_conn.close()
        arduino_serial_conn.open()
        sleep(3)
        print("Connection established")
    except serial.SerialException as e:
        crash("Failed to connect to turret on " + str(turret_serial_port) + "\n\n" + str(e))


def command_turret(command):
    print("Sending command: " + hex(command))
    arduino_serial_conn.write(chr(command).encode())


def test_turret_commands():
    print("\nInitiating turret commands test...\n")
    sleep(3)

    print("Commanding SAFETY OFF")
    command_turret(CMD_SAFETY_OFF)
    sleep(3)

    print("Commanding SAFETY ON")
    command_turret(CMD_SAFETY_ON)
    sleep(3)

    print("Commanding SAFETY OFF")
    command_turret(CMD_SAFETY_OFF)
    sleep(3)

    print("Firing for 1 second")
    command_turret(CMD_FIRE)
    sleep(1)
    command_turret(CMD_STOP_FIRE)
    sleep(3)

    print("Left at speed 7")
    command_turret(CMD_ROTATE_ZERO - 7)
    sleep(7)

    print("Right at speed 3")
    command_turret(CMD_ROTATE_ZERO + 3)
    sleep(7)

    print("Up at speed 10")
    command_turret(CMD_PITCH_UP_MAX)
    sleep(7)

    print("Down at speed 1")
    command_turret(CMD_PITCH_ZERO - 1)
    sleep(7)

    print("Testing moving and firing")
    command_turret(CMD_ROTATE_ZERO + 3)
    command_turret(CMD_FIRE)
    sleep(1)
    command_turret(CMD_STOP_FIRE)
    sleep(7)

    print("Turning safety back on")
    command_turret(CMD_SAFETY_ON)
    sleep(2)

    print("Test complete. Exiting program.")


def init_incoming_commands_server():
    global command_server
    print("Initializing incoming commands server...\n")
    port = 9001
    command_server = SimpleWebSocketServer('', port, TurretCommandServer)
    command_server.serveforever()


def play_shotgun_racking_sound():
    play_sound('shotgun_racking.wav')


def play_sound(wav_file_name):
    f = wave.open(wav_file_name, 'rb')
    sample_rate = f.getframerate()
    channels = f.getnchannels()
    format = sound.AFMT_S16_LE
    sound = sound.Output(sample_rate, channels, format)
    s = f.readframes(3000000)
    sound.play()


def crash(reason):
    print(Fore.RED + reason + Style.RESET_ALL)
    colorama.deinit()
    exit(1)


def SerialLoggingThread():
    print("Beginning turret output logging...\n")
    while not exiting:
        if arduino_serial_conn.isOpen():
            turret_output = str(arduino_serial_conn.readline(), "utf-8")
            if turret_output != "":
                print("Turret: " + turret_output, end='')
        else:
            return


class TurretCommandServer(WebSocket):

    IN_CMD_FIRE = "FIRE"
    IN_CMD_CEASE_FIRE = "CEASE FIRE"
    IN_CMD_SAFETY_ON = "SAFETY ON"
    IN_CMD_SAFETY_OFF = "SAFETY OFF"
    IN_CMD_ROTATE = "ROTATE SPEED"
    IN_CMD_PITCH = "PITCH SPEED"


    def handle_message(self):
        incoming_command = self.data
        print("Incoming command: " + incoming_command)
        self.process_incoming_command(incoming_command)


    def handle_connected(self):
        print("Client connected to command server.")


    def handle_close(self):
        print("Closing websocket server...")


    def process_incoming_command(self, command):
        if (command == self.IN_CMD_FIRE):
            command_turret(CMD_FIRE)
        elif (command == self.IN_CMD_CEASE_FIRE):
            command_turret(CMD_STOP_FIRE)
        elif (command == self.IN_CMD_SAFETY_ON):
            command_turret(CMD_SAFETY_ON)
        elif (command == self.IN_CMD_SAFETY_OFF):
            command_turret(CMD_SAFETY_OFF)
        elif (command.startswith(self.IN_CMD_ROTATE)):
            speed = command.split(' ')[2]
            command_turret(CMD_ROTATE_ZERO + int(speed))
        elif command.startswith(self.IN_CMD_PITCH):
            speed = command.split(' ')[2]
            command_turret(CMD_PITCH_ZERO + int(speed))
        else:
            print("Unrecognized command received: " + str(command))


if __name__ == "__main__":
    main()
