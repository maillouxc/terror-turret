#!/usr/bin/env python3

# This Python program is the main Rpi control program for the turret

import serial

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
    testTurretCommands()

def sendCommandToTurret(command):
    serial.write(command)
    
def testTurretCommands():
    print("TODO")

if __name__ == "__main__":
    main()
