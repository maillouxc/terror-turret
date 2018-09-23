#!/usr/bin/env python3

# This Python program is the main Rpi control program for the turret

import sys
import argparse
import serial
import serial.tools.list_ports
from threading import Thread
from time import sleep
import colorama
from colorama import Fore
from colorama import Style

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

turretSerialPort = '/dev/ttyUSB0'

arduinoSerialConn = serial.Serial()

inTestMode = False

# Used to know when are about to exit, to allow threads to clean up things
exiting = False


def main():
    colorama.init()
    parseCommandLineArguments()
    print("\nTurret manager software started.\n")
    establishConnectionToTurret()

    loggingThread = Thread(target = SerialLoggingThread)
    loggingThread.start()

    if testMode:
        testTurretCommands()
        cleanup()
        exit(0)

    # TODO
    
    cleanup()
    exit(0)


def parseCommandLineArguments():
    programDescription = "Main control software for the Terror Turret."
    parser = argparse.ArgumentParser(description = programDescription)
    parser.add_argument(
        '--test-mode',
        type = bool,
        default = False,
        dest = 'testMode',
        help = "Runs the test script instead of normal program")
    parser.add_argument(
        '--serial-port',
        default = '/dev/ttyUSB0',
        dest = 'serialPort',
        help = "The name of the serial port to connect from.")

    # It pains me to use 'global' here - we need to refactor this when we can
    parsedArgs = parser.parse_args()
    global testMode
    testMode = parsedArgs.testMode
    global turretSerialPort
    turretSerialPort = parsedArgs.serialPort


def cleanup():
    global exiting
    exiting = True
    # Allow threads to have a moment to react
    sleep(1)
    arduinoSerialConn.close()
    print("\nTurret manager software exited.\n")
    colorama.deinit()


def establishConnectionToTurret():
    print("Available serial ports: ")
    for port in serial.tools.list_ports.comports():
        print(str(port))
    print("")

    # TODO we need a way to programatically determine which port the turret is on
    # We'll have to use some sort of negotation, ping each port and listening for correct response

    print("Attempting to connect to turret on " + turretSerialPort + "...")
    try:
        # The serial port takes some time to init 
        arduinoSerialConn.baudrate = SERIAL_BAUD_RATE
        arduinoSerialConn.port = turretSerialPort
        arduinoSerialConn.timeout = 2
        arduinoSerialConn.close()
        arduinoSerialConn.open()
        sleep(3)
        print("Connection established")
    except serial.SerialException as e:
        crash("Failed to connect to turret on " + str(turretSerialPort) + "\n\n" + str(e))


def commandTurret(command):
    print("Sending command: " + hex(command))
    arduinoSerialConn.write(chr(command).encode())
    

def testTurretCommands():
    print("\nInitiating turret commands test...\n")
    sleep(3)

    print("Commanding SAFETY OFF")
    commandTurret(CMD_SAFETY_OFF)
    sleep(3)

    print("Commanding SAFETY ON")
    commandTurret(CMD_SAFETY_ON)
    sleep(3)

    print("Commanding SAFETY OFF")
    commandTurret(CMD_SAFETY_OFF)
    sleep(3)

    print("Firing for 1 second")
    commandTurret(CMD_FIRE)
    sleep(1)
    commandTurret(CMD_STOP_FIRE)
    sleep(3)

    print("Left at speed 7")
    commandTurret(CMD_ROTATE_ZERO - 7)
    sleep(7)

    print("Right at speed 3")
    commandTurret(CMD_ROTATE_ZERO + 3)
    sleep(7)

    print("Up at speed 10")
    commandTurret(CMD_PITCH_UP_MAX)
    sleep(7)

    print("Down at speed 1")
    commandTurret(CMD_PITCH_ZERO - 1)
    sleep(7)

    print("Testing moving and firing")
    commandTurret(CMD_ROTATE_ZERO + 3)
    commandTurret(CMD_FIRE)
    sleep(1)
    commandTurret(CMD_STOP_FIRE)
    sleep(7)

    print("Turning safety back on")
    commandTurret(CMD_SAFETY_ON)
    sleep(2)
    
    print("Test complete.")


def crash(reason):
    print(Fore.RED + reason + Style.RESET_ALL)
    colorama.deinit()
    exit(1)


def SerialLoggingThread():
    while(not exiting):
        if arduinoSerialConn.isOpen():
            turretOutput = str(arduinoSerialConn.readline(), "utf-8")
            if turretOutput != "":
                print("Turret: " + turretOutput)
        else:
            return


if __name__ == "__main__":
    main()

