/************************************************************************************
   Fire control system for the Terror Turret.
 ************************************************************************************
   Wiring is described in the file `wiring_instructions.txt` in the docs folder.

   The joystick and button are only here for testing purposes at the moment.
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

// Input handling related constants - adjustable
const int JOY_DEADZONE_LOW = 150;
const int JOY_DEADZONE_HIGH = 540;
const int DEBOUNCE_DELAY_MS = 50;

const int JOY_MIN_VALUE = 0;
const int JOY_MAX_VALUE = 1024;

// Needed to compensate for servo centered position being slightly wrong
const int TURRET_PITCH_CALIBRATION = 120;

// These constants represent the microsecond PWM values defining the range of
// motion for the type of servos we are using - the servos we use have a 180 degree
// range, so the min value here represents 0 degrees and the max represents 180.
// Note that the actual allowed range of motion is limited by our code to prevent
// the gun from hitting the turret itself, plus a few other reasons.
const int SERVO_MIN = 600;
const int SERVO_MAX = 2400;

// Turret movement constraints
// These determine where the turret is ALLOWED to move, NOT where it CAN move.
const int PAN_MIN = 600;
const int PAN_MAX = 2400;
const int TILT_MIN = 900;
const int TILT_MAX = 2000;

const int INITIAL_PAN_VALUE = 1500; // Centered
const int INITIAL_TILT_VALUE = 1500; // Centered

const int MAX_SERVO_SPEED = 10; // Should be between 5 and 500
const int MAX_GUN_FIRING_TIME = 2000;

const int SERIAL_BAUD_RATE = 9600;
/***********************************************************************************/

// Servo objects used to control the servos
Servo panServo;
Servo tiltServo;

// Current state variables for the turret
int panValue = INITIAL_PAN_VALUE;
int tiltValue = INITIAL_TILT_VALUE;

bool isSafetyOn = true;
int laserState = LOW;

int buttonState = LOW;
int lastButtonState = LOW;
long lastDebounceTime = 0;

bool isFiring = false;
int firingStartTime;

/**
 * Code in this method will run when the board is first powered.
 */
void setup()
{
  Serial.begin(SERIAL_BAUD_RATE);
  setupPins();
  engageSafety();
  moveServosToInitialPosition();
}

/**
 * Code in this method will run repeatedly.
 */
void loop()
{
  startOrStopFiringBasedOnButtonState();
  readAnalogSticksAndSetDesiredServoPositions();
  ensureDesiredServoPositionsAreInAllowedRange();
  panServo.writeMicroseconds(panValue);
  tiltServo.writeMicroseconds(tiltValue);
  delay(15);
}

void setupPins()
{
  // Writing before attach is technically wrong but stops the intial autocentering
  // We want to handle the centering ourselves, autocentering can damage the turret
  panServo.writeMicroseconds(0);
  tiltServo.writeMicroseconds(0);
  
  panServo.attach(PAN_SERVO_PIN, SERVO_MIN, SERVO_MAX); 
  tiltServo.attach(TILT_SERVO_PIN, SERVO_MIN, SERVO_MAX);

  pinMode(PUSHBUTTON_PIN, INPUT);
  pinMode(LASER_PIN, OUTPUT);
}

void moveServosToInitialPosition()
{
  panServo.writeMicroseconds(panValue);
  tiltValue -= TURRET_PITCH_CALIBRATION;
  tiltServo.writeMicroseconds(tiltValue);
  delay(1000);
}

/**
 * When the safety is on, the laser will turn off and the weapon will not fire.
 */
void setSafetyOn(bool safetyOn)
{
  isSafetyOn = safetyOn;
  setLaserOn(!safetyOn);
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

  if (horizontalJoyValue < JOY_DEADZONE_LOW
      || horizontalJoyValue > JOY_DEADZONE_HIGH) {
    panValue += (1.0 * map(horizontalJoyValue, JOY_MIN_VALUE, JOY_MAX_VALUE, -speed, speed));
  }

  if (verticalJoyValue < JOY_DEADZONE_LOW 
      || verticalJoyValue > JOY_DEADZONE_HIGH) {
    tiltValue += (1.0 * map(verticalJoyValue, JOY_MIN_VALUE, JOY_MAX_VALUE, -speed, speed));
  }
}

/**
 * Gates the pan and tilt servo position values to be within the allowed range.
 * 
 * The allowed range is determined based on several factors, most important of
 * which is preventing the turret from hitting itself.
 */
void ensureDesiredServoPositionsAreInAllowedRange()
{
  panValue = max(panValue, PAN_MIN);
  panValue = min(panValue, PAN_MAX);
  tiltValue = max(tiltValue, TILT_MIN);
  tiltValue = min(tiltValue, TILT_MAX);
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
  setIsFiring(reading == HIGH); 
}

void setIsFiring(bool fireWeapon) {
  if (fireWeapon) {
    if (!isSafetyOn) {
      digitalWrite(TRIGGER_PIN, HIGH);
      firingStartTime = millis();
    }
  }
  else {
    digitalWrite(TRIGGER_PIN, LOW);
  }
}
