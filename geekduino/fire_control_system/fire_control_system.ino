/************************************************************************************
 * Fire control system for the Terror Turret.
 ************************************************************************************
 * Wiring is as follows:
 * 
 * Laser - Digital Pin 2
 * Trigger - Digital Pin 3
 * Pushbutton - Digital Pin 4
 * Pan Servo - Digital Pin 10
 * Tilt Servo - Digital Pin 11
 * 
 * Joystick (Horizontal) - Analog Pin 0
 * Joystick (Vertical) - Analog Pin 2
 ***********************************************************************************/

#include <Servo.h>

// Digital pin assignments
const int PAN_SERVO_PIN = 10;
const int TILT_SERVO_PIN = 11;
const int LASER_PIN = 2;
const int PUSHBUTTON_PIN = 2;

// Analog pin assignments
const int HORIZONTAL_JOY_PIN = 0;
const int VERTICAL_JOY_PIN = 1;

const int MAX_GUN_ELEVATION_DEGREES = 45;
const int MIN_GUN_ELEVATION_DEGREES = 30;
const int MAX_GUN_LEFT_DEGREES = 80;
const int MAX_GUN_RIGHT_DEGREES = 80;
const int TURRET_PITCH_CALIBRATION_DEGREES = 20;

bool isSafetyOn = true;

/**
 * Code in this method will run when the board is first powered.
 */
void setup() {
  engageSafety();
  moveTurretToCenteredPosition();
}

/**
 * Code in this method will run repeatedly.
 */
void loop() {
  /* TODO */
}

void engageSafety() {
  /* TODO turn off laser */
}

void disengageSafety() {
  /* TODO turn on laser */
}

void setLaserOn(bool laserOn) {
  /* TODO */
}

void moveTurretToCenteredPosition() {
  /* TODO */
}

void fire() {
  if (safetyIsOn) {
    return;
  }
  /* TODO */
}
