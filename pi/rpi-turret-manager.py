#!/usr/bin/env python3

# This Python program is the main Rpi control program for the turret

import serial

ARDUINO_SERIAL_CONN_PORT = '/dev/tty/USB0'
SERIAL_BAUD_RATE = 9600

arduinoSerialConn = serial.Serial(ARDUINO_SERIAL_CONN_PORT, SERIAL_BAUD_RATE)

def main():
    print("Here we go...")

if __name__ == "__main__":
    main()

