/************************************************************************************
   Fire control system for the Terror Turret.
 ************************************************************************************
   Wiring is as follows:

   Laser - Digital Pin 2
   Pushbutton - Digital Pin 4
   Trigger - Digital Pin 7
   Pan Servo - Digital Pin 10
   Tilt Servo - Digital Pin 11

   Joystick (Horizontal) - Analog Pin 0
   Joystick (Vertical) - Analog Pin 2

   The joystick and button are only here for testing purposes at the moment.
   This will eventually take its instructions from a Raspberry Pi.
 ***********************************************************************************/

#include <Servo.h>

/************************************************************************************
   Constants
 ***********************************************************************************/
// Digital pin assignments
const int LASER_PIN = 2;
const int PUSHBUTTON_PIN = 4;
const int TRIGGER_PIN = 7;
const int PAN_SERVO_PIN = 10;
const int TILT_SERVO_PIN = 11;

// Analog pin assignments
const int HORIZONTAL_JOY_PIN = 0;
const int VERTICAL_JOY_PIN = 1;

// Input handling related adjustable constants
const int JOY_DEADZONE_X_LOW = 480;
const int JOY_DEADZONE_X_HIGH = 540;
const int JOY_DEADZONE_Y_LOW = 480;
const int JOY_DEADZONE_Y_HIGH = 540;
const int DEBOUNCE_DELAY_MS = 50;

// Needed to compensate for servo centered position being slightly wrong
const int TURRET_PITCH_CALIBRATION_MICROSECONDS = 100;

const int JOY_MIN_VALUE = 0;
const int JOY_MAX_VALUE = 1023;

// These constants represent the microsecond pulse values defining the range of
// motion for the type of servos we are using - the servos we use have a 180 degree
// range, so the min value here represents 0 degrees and the max represents 180.
// Note that the actual allowed range of motion is limited by our code to prevent
// the gun from hitting the turret itself, plus a few other reasons.
const int SERVO_MIN_MICROSECS = 600;
const int SERVO_MAX_MICROSECS = 2400;
const int SERVO_MIN_DEGREES = 0;
const int SERVO_MAX_DEGREES = 180;

// Turret movement constraint settings
// These determine where the turret is ALLOWED to move, NOT where it CAN move.
// Note that 90 degrees is centered
const int PAN_MIN_DEGREES = 0;
const int PAN_MAX_DEGREES = 180;
const int TILT_MIN_DEGREES = 45;
const int TILT_MAX_DEGREES = 135;

const int INITIAL_PAN_VALUE_MICROSECS = 1500; // Centered
const int INITIAL_TILT_VALUE_MICROSECS = 1500; // Centered

const int MAX_SERVO_SPEED = 10; // Should be between 5 and 500
/***********************************************************************************/

int degreesToMicrosecs(unsigned int);

// Servo objects used to control the servos
Servo panServo;
Servo tiltServo;

int panMinMicrosecs = degreesToMicrosecs(PAN_MIN_DEGREES);
int panMaxMicrosecs = degreesToMicrosecs(PAN_MAX_DEGREES);
int tiltMinMicrosecs = degreesToMicrosecs(TILT_MIN_DEGREES);
int tiltMaxMicrosecs = degreesToMicrosecs(TILT_MAX_DEGREES);

// Current state variables for the turret
int panValueMicrosecs = INITIAL_PAN_VALUE_MICROSECS;
int tiltValueMicrosecs = INITIAL_TILT_VALUE_MICROSECS;

bool isSafetyOn = true;
int laserState = LOW;
int buttonState = LOW;
int lastButtonState = LOW;
long lastDebounceTime = 0;

/**
   Code in this method will run when the board is first powered.
*/
void setup()
{
  setupPins();
  engageSafety();
  moveServosToInitialPosition();
}

/**
   Code in this method will run repeatedly.
*/
void loop()
{
  startOrStopFiringBasedOnButtonState();
  readAnalogSticksAndSetDesiredServoPositions();
  ensureDesiredServoPositionsAreInAllowedRange();
  panServo.writeMicroseconds(panValueMicrosecs);
  panServo.writeMicroseconds(tiltValueMicrosecs);
  delay(15);
}

void setupPins()
{
  panServo.attach(PAN_SERVO_PIN, SERVO_MIN_MICROSECS, SERVO_MAX_MICROSECS);
  tiltServo.attach(TILT_SERVO_PIN, SERVO_MIN_MICROSECS, SERVO_MAX_MICROSECS);

  pinMode(PUSHBUTTON_PIN, INPUT);
  pinMode(LASER_PIN, OUTPUT);
}

void moveServosToInitialPosition()
{
  panServo.write(panValueMicrosecs);
  delay(1000);
  tiltServo.write(tiltValueMicrosecs);
  delay(1000);
}

void engageSafety()
{
  isSafetyOn = true;
  setLaserOn(false);
}

void disengageSafety()
{
  isSafetyOn = false;
  setLaserOn(true);
}

void setLaserOn(bool laserOn)
{
  int pinValue = laserOn ? HIGH : LOW;
  digitalWrite(LASER_PIN, pinValue);
}

void readAnalogSticksAndSetDesiredServoPositions()
{
  // Analog stick values range between 0 and 1023
  int horizontalJoyValue = analogRead(HORIZONTAL_JOY_PIN);
  int verticalJoyValue = analogRead(VERTICAL_JOY_PIN);
  int speed = MAX_SERVO_SPEED;

  // Handle pan
  if (horizontalJoyValue < JOY_DEADZONE_X_LOW
      || horizontalJoyValue > JOY_DEADZONE_X_HIGH) {
    panValueMicrosecs += (1.0 * map(horizontalJoyValue, 0, 1023, -speed, speed));
  }

  // Handle tilt
  if (verticalJoyValue < JOY_DEADZONE_Y_LOW
      || verticalJoyValue > JOY_DEADZONE_Y_HIGH) {
    tiltValueMicrosecs += (1.0 * map(verticalJoyValue, 0, 1023, -speed, speed));
  }
}

void ensureDesiredServoPositionsAreInAllowedRange()
{
  panValueMicrosecs = max(panValueMicrosecs, panMinMicrosecs);
  panValueMicrosecs = min(panValueMicrosecs, panMaxMicrosecs);
  tiltValueMicrosecs = max(tiltValueMicrosecs, tiltMinMicrosecs);
  tiltValueMicrosecs = min(tiltValueMicrosecs, tiltMaxMicrosecs);
}

int readAndDebounceButton()
{
  int reading = digitalRead(PUSHBUTTON_PIN);
  if (reading != lastButtonState) {
    lastDebounceTime = millis();
  }

  long timeSinceStateChange = millis() - lastDebounceTime;
  if (timeSinceStateChange > DEBOUNCE_DELAY_MS) {
    if (reading != buttonState) {
      // Button is now debounced, so reading should be the correct state
      buttonState = reading;
    }
  }
  lastButtonState = reading;
  return buttonState;
}

void startOrStopFiringBasedOnButtonState()
{
  int reading = readAndDebounceButton();
  if (reading == HIGH) {
    // Begin firing
    if (!isSafetyOn) {
      digitalWrite(TRIGGER_PIN, HIGH);
    }
  } else {
    // Stop firing
    digitalWrite(TRIGGER_PIN, LOW);
  }
}

int degreesToMicrosecs(unsigned int angleDegrees)
{
  float microsecs = map(angleDegrees,
                        SERVO_MIN_MICROSECS,
                        SERVO_MAX_MICROSECS,
                        SERVO_MIN_DEGREES,
                        SERVO_MAX_DEGREES);

  return round(microsecs);
}
