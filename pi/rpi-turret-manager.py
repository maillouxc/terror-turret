#!/usr/bin/env python3

# This Python program is the main Rpi control program for the turret

import serial
from time import sleep

ARDUINO_SERIAL_CONN_PORT = '/dev/tty/USB0'
SERIAL_BAUD_RATE = 9600

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

arduinoSerialConn = serial.Serial(ARDUINO_SERIAL_CONN_PORT, SERIAL_BAUD_RATE)


def main():
    print("Turret manager software started.")
    testTurretCommands()


def commandTurret(command):
    serial.write(str(command))
    

def testTurretCommands():
    print("Initiating turret commands test...")
    sleep(1)

    print("Commanding SAFETY OFF")
    commandTurret(CMD_SAFETY_OFF)
    sleep(1)

    print("Commanding SAFETY ON")
    commandTurret(CMD_SAFETY_ON)
    sleep(1)

    print("Commanding SAFETY OFF")
    commandTurret(CMD_SAFETY_OFF)
    sleep(1)

    print("Firing for 1 second")
    commandTurret(CMD_FIRE)
    sleep(1)
    commandTurret(CMD_STOP_FIRE)
    sleep(1)

    print("Left at speed 7")
    commandTurret(CMD_ROTATE_ZERO - 7)
    sleep(2)

    print("Right at speed 3")
    commandTurret(CMD_ROTATE_ZERO + 3)
    sleep(3)

    print("Up at speed 10")
    commandTurret(CMD_PITCH_UP_MAX)
    sleep(2)

    print("Down at speed 1")
    commandTurret(CMD_PITCH_ZERO - 1)
    sleep(5)

    print("Testing moving and firing")
    commandTurret(CMD_ROTATE_ZERO + 3)
    commandTurret(CMD_FIRE)
    sleep(1)
    commandTurret(CMD_STOP_FIRE)
    sleep(2)

    print("Turning safety back on")
    comandTurret(CMD_SAFETY_ON)

    print("Test complete.")


if __name__ == "__main__":
    main()
