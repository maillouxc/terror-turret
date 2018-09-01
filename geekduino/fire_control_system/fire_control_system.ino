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
 * 
 * We are currently not using the joystick to do anything, but it will remain
 * wired up for now in case we need it for something, and so the pins and other
 * relevant variables will remain defined in the code for potential usage later.
 ***********************************************************************************/

#include <Servo.h>

/************************************************************************************
 * Constants
 ***********************************************************************************/
// Digital pin assignments
const int PAN_SERVO_PIN = 10;
const int TILT_SERVO_PIN = 11;
const int LASER_PIN = 2;
const int PUSHBUTTON_PIN = 2;

// Analog pin assignments
const int HORIZONTAL_JOY_PIN = 0;
const int VERTICAL_JOY_PIN = 1;

// Calibration related constants
const int JOY_DEADZONE_LOW = 480;
const int JOY_DEADZONE_HIGH = 540;

// Turret movement constraint settings
// Note that 90 degrees is "centered" on these servos
const int PAN_MIN_DEGREES = 0;
const int PAN_MAX_DEGREES = 180;
const int TILT_MIN_DEGREES = 45;
const int TILT_MAX_DEGREES = 135;

// Needed to compensate for servo centered position being slightly wrong
const int TURRET_PITCH_CALIBRATION_DEGREES = 20;
/***********************************************************************************/

// Servo objects used to control the servos
Servo panServo;
Servo tiltServo;

// Current state variables for the turret
int panValue = 90;
int tiltValue = 90;
bool isSafetyOn = true;
int laserState = LOW;

/**
 * Code in this method will run when the board is first powered.
 */
void setup() {
  setupPins();
  engageSafety();
  moveServosToInitialPosition();
}

/**
 * Code in this method will run repeatedly.
 */
void loop() {
  /* TODO */
}

void setupPins() {
  panServo.attach(PAN_SERVO_PIN);
  tiltServo.attach(TILT_SERVO_PIN);
  pinMode(LASER_PIN, OUTPUT);
}

void moveServosToInitialPosition() {
  panServo.write(panValue);
  delay(1000);
  tiltServo.write(tiltValue);
  delay(1000);
}

void engageSafety() {
  isSafetyOn = true;
  setLaserOn(false);
}

void disengageSafety() {
  isSafetyOn = false;
  setLaserOn(true);
}

void setLaserOn(bool laserOn) {
  if (laserOn) {
    digitalWrite(LASER_PIN, HIGH);
  } else {
    digitalWrite(LASER_PIN, LOW);
  }
}

void fire() {
  if (isSafetyOn) {
    return;
  }
  /* TODO */
}
