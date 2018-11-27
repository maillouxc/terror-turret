/************************************************************************************
   Fire control system for the Terror Turret.
 ************************************************************************************
   Wiring is described in the file `wiring_instructions.txt` in the docs folder.

   The only phyiscally attached control is the button, which engages the safety
   when pressed.
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

const int DEBOUNCE_DELAY_MS = 50;
const int TOGGLE_BUTTON_COOLDOWN = 200;

// Needed to compensate for servo centered position being slightly wrong
const int TURRET_PITCH_CALIBRATION = 120;

// These constants represent the microsecond PWM values defining the range of
// motion for the type of servos we are using - the servos we use have a 180 degree
// range, so the min value here represents 0 degrees and the max represents 180.
// Note that the actual allowed range of motion is limited by our code to prevent
// the gun from hitting the turret itself, plus a few other reasons.
const int SERVO_MIN = 600;
const int SERVO_MAX = 2400;

// These determine where the turret is ALLOWED to move, NOT where it CAN move.
const int PAN_MIN = 600;
const int PAN_MAX = 2400;
const int TILT_MIN = 900;
const int TILT_MAX = 2000;

const int INITIAL_PAN_VALUE = 1500; // Centered
const int INITIAL_TILT_VALUE = 1500; // Centered

const int NUM_SPEED_LEVELS = 10;
const int MAX_GUN_FIRING_TIME = 2000;

const int SERIAL_BAUD_RATE = 9600;
/***********************************************************************************/

/************************************************************************************
 Command-link protocol
 ************************************************************************************
 The following constants define the bytes that represent various commands that can
 be sent to this FCS from the Raspberry Pi Turret Manager.

 There is no particular reason that the bytes start at 0x21, other than that it is
 the first non-whitespace, non-control ASCII character. We aren't considering these
 commands to be ASCII characters, they are just numbers, but it's helpful for
 debugging to have a simple printed character in the logs or wherever.

 The rotation stuff is based on speed levels. There are 10 speed levels, so the
 command CMD_ROTATE_LEFT_MAX means rotate at speed -10, in other words turn left at 
 max speed. These commands represent a range. So, CMD_ROTATE_ZERO - 5 would tell
 the turret to rotate left at speed 5. Negative speeds imply left rotation, and
 positive speeds imply right rotation. The same logic can be applied to pitching.
/***********************************************************************************/
unsigned char CMD_FIRE = 0x21;
unsigned char CMD_STOP_FIRE = 0x22;
unsigned char CMD_SAFETY_ON = 0x23;
unsigned char CMD_SAFETY_OFF = 0x24;
unsigned char CMD_REBOOT = 0x25; // Currently does nothing
unsigned char CMD_ROTATE_LEFT_MAX = 0x26;
unsigned char CMD_ROTATE_ZERO = 0x30;
unsigned char CMD_ROTATE_RIGHT_MAX = 0x3A;
unsigned char CMD_PITCH_DOWN_MAX = 0x3B;
unsigned char CMD_PITCH_ZERO = 0x45;
unsigned char CMD_PITCH_UP_MAX = 0x4F;
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
long firingStartTime;
unsigned long firingCoolDownTime = 0;

int horizontalSpeedLevel = 0;
int verticalSpeedLevel = 0;

/**
 * Code in this method will run when the board is first powered.
 */
void setup()
{
  Serial.begin(SERIAL_BAUD_RATE);
  Serial.print("Initializing");
  setupPins();
  setSafetyOn(true);
  moveServosToInitialPosition();
  Serial.println("Init complete");
  Serial.println("Ready for commands");
}

/**
 * Code in this method will run repeatedly.
 */
void loop()
{
  acceptSerialCommandsFromRPi();
  stopFiringIfMaxBurstLengthExceeded();
  readAndDebounceButton();
  calculateNewServoPositions();
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
  pinMode(TRIGGER_PIN, OUTPUT);
}

void acceptSerialCommandsFromRPi()
{
  while (Serial.available()) {
    processCommand(Serial.read());
  }
}

void processCommand(unsigned char command)
{
  Serial.print("Command received: ");
  Serial.print(command, HEX);
  Serial.print("\n");
  
  // First process the movement cmds, since they are a ranged values -10 through 10
  if (command >= CMD_ROTATE_LEFT_MAX && command <= CMD_ROTATE_RIGHT_MAX) {
    horizontalSpeedLevel = command - CMD_ROTATE_ZERO;
  }
  else if (command >= CMD_PITCH_DOWN_MAX && command <= CMD_PITCH_UP_MAX) {
    verticalSpeedLevel = command - CMD_PITCH_ZERO;
  }
  else if (command == CMD_FIRE) {
    setIsFiring(true);
  }
  else if (command == CMD_STOP_FIRE) {
    setIsFiring(false);
  }
  else if (command == CMD_SAFETY_ON) {
    setSafetyOn(true);
  }
  else if (command == CMD_SAFETY_OFF) {
    setSafetyOn(false);
  }
  else if (command == CMD_REBOOT) {
    reboot();
  }
}

void moveServosToInitialPosition()
{
  panServo.writeMicroseconds(panValue);
  tiltValue -= TURRET_PITCH_CALIBRATION;
  tiltServo.writeMicroseconds(tiltValue);
  delay(1000);
}

void calculateNewServoPositions()
{
  // panValue subtracts because the servo thinks left is positive but we define right as positive
  panValue -= horizontalSpeedLevel;
  tiltValue += verticalSpeedLevel;
}

/**
 * When the safety is on, the laser will turn off and the weapon will not fire.
 */
void setSafetyOn(bool safetyOn)
{
  if (safetyOn) {
    Serial.println("Safety ON");
  }
  else {
    Serial.println("Safety OFF");
  }
  
  isSafetyOn = safetyOn;
  setLaserOn(!safetyOn);
}

void setLaserOn(bool laserOn)
{
  if (laserOn) {
    Serial.println("Laser ON");
  }
  else {
    Serial.println("Laser OFF");
  }
  
  int pinValue = laserOn ? HIGH : LOW;
  digitalWrite(LASER_PIN, pinValue);
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
      if (buttonState == HIGH) {
        onButtonPressed(); 
      }
    }
  }
  lastButtonState = reading;
  return buttonState;
}

/**
 * This helps prevent overheating the gun and damaging it.
 */
void stopFiringIfMaxBurstLengthExceeded()
{
  unsigned long currentTime = millis();
  if (currentTime - firingStartTime > MAX_GUN_FIRING_TIME) {
    setIsFiring(false);
    firingCoolDownTime = millis() + 5000;
  }
}

/**
 * Called by readAndDebounceButton().
 * 
 * Really, we should be using a function pointer to use the listener pattern here,
 * but this is really simple software and it wouldn't be worth our time.
 * Hardcoding in a listener function is bad, but in this case, it's okay.
 */
void onButtonPressed()
{
  setSafetyOn(!isSafetyOn);
}

void setIsFiring(bool fireWeapon) 
{
  // If we're already firing - just continue firing - no work needed
  if (isFiring && fireWeapon) {
    return;
  }
  
  if (fireWeapon) {
    if (!isSafetyOn && millis() > firingCoolDownTime) {
      isFiring = true;
      firingStartTime = millis();
      digitalWrite(TRIGGER_PIN, HIGH);
    }
  }
  else {
    digitalWrite(TRIGGER_PIN, LOW);
    isFiring = false;
  }
}

/**
 * Currently not implemented.
 */
void reboot() {
  // TODO
}
