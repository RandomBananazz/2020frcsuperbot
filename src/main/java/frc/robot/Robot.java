package frc.robot;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
// import edu.wpi.cscore.UsbCamera;
import edu.wpi.first.wpilibj.I2C;
// import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.wpilibj.ADXRS450_Gyro;
// import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DigitalOutput;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.GenericHID.Hand;
import edu.wpi.first.wpilibj.Relay;
import edu.wpi.first.wpilibj.SpeedControllerGroup;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.util.Color;
import com.revrobotics.ColorSensorV3;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.ColorMatchResult;
import com.revrobotics.CANSparkMax;
import com.revrobotics.ColorMatch;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableEntry;

/*
http://www.revrobotics.com/content/sw/max/sdk/REVRobotics.json
http://revrobotics.com/content/sw/color-sensor-v3/sdk/REVColorSensorV3.json
*/


/**
 * Button Layout / Axis 0 - Left stick left and right Axis 1 - Left stick up and
 * down Axis 2 - L2 Axis 3 - R2 Axis 4 - Right stick left and right Axis 5 -
 * Right stick up and down
 * 
 * Button 1 - x (cross) Button 2 - circle Button 3 - triangle Button 4 - square
 * Button 5 - L1 Button 6 - R1 Button 7 - Unknown Button 8 - start (plus) Button
 * 9 - end (minus)
 */

public class Robot extends TimedRobot {

  /** Drive Motor Controllers */
  CANSparkMax leftMaster = new CANSparkMax(3, MotorType.kBrushless);
  CANSparkMax rightMaster = new CANSparkMax(1, MotorType.kBrushless);
  CANSparkMax leftSide = new CANSparkMax(4, MotorType.kBrushless);
  CANSparkMax rightSide = new CANSparkMax(2, MotorType.kBrushless);

  SpeedControllerGroup leftGrouping = new SpeedControllerGroup(leftMaster, leftSide);
  SpeedControllerGroup rightGrouping = new SpeedControllerGroup(rightMaster, rightSide);

  DifferentialDrive Drive = new DifferentialDrive(leftGrouping, rightGrouping);

  /** Other Motor Controllers */
  TalonSRX FortuneWheel = new TalonSRX(5); // currently unused in code

  VictorSPX Shooter = new VictorSPX(6); // controls turret launch motor
  VictorSPX Aim = new VictorSPX(7); // controls turret aim motor
  VictorSPX IntakeWheel = new VictorSPX(8); // controls the intake wheels
  VictorSPX IntakeBelt = new VictorSPX(9); // controls the intake elevator motor
  VictorSPX IntakeUpandDown = new VictorSPX(10); // controls the raising/lowering of intake bar itself
  VictorSPX FortuneUpandDown = new VictorSPX(11); // currently unused in code
  VictorSPX toShoot = new VictorSPX(12); // brings the POWERCELL up to the actual firing mechanism
  VictorSPX elevator = new VictorSPX(13); //elevator system TODO since it features two motors

  /** Gamepad */
  XboxController _gamepadDrive = new XboxController(0); // driving
  Joystick _gamepadShoot = new Joystick(1); // turret control

  /** Vision / Raspberry Pi */
  NetworkTableInstance table = NetworkTableInstance.getDefault();
  NetworkTable cameraTable = table.getTable("chameleon-vision").getSubTable("TurretCam");
  NetworkTableEntry targetX;
  NetworkTableEntry poseArray; // 3D positioning [x distance meters, y distance meters, angle degrees]
  NetworkTableEntry isValid;

  boolean targetFound; // if a target was found
  double rotationOffset; // Current offset
  double angleTolerance = 5; // Deadzone for alignment
  double poseX; // X distance in meters
  double poseY; // Y distance in meters
  double aimDist; // distance magnitude in meters (pythagorean)

  // autonomous
  int count = 0;
  double autoForward = 0.25;
  double autoTurn = 0;
  boolean canTurn = false;
  double nowAngle = 0;
  boolean isPneumatic = false;
  double pneuCount = 0;

  // Gyro
  ADXRS450_Gyro gyroBoy = new ADXRS450_Gyro();
  double angle = 0;
  double rate = 0;
  boolean gyroConnected = false;

  // speed divider
  double speedDiv = 4.0;

  // ramping
  double prevVal = 0;

  // peripherals
  Relay lights = new Relay(0);
  Relay.Value lightsOn = Relay.Value.kForward;
  Relay.Value lightsOff = Relay.Value.kOff;

  // hatch light
  DigitalOutput hatchLight = new DigitalOutput(9);

  // reverse controls
  boolean circle;
  boolean x;
  boolean triangle;
  boolean square;
  boolean reverseControls = false;
  double reverseControlDelay = 1;
  boolean reverse = false;
  boolean aiming = false;
  double shootSpeed;
  double intakeSpeed;
  boolean intakeMove;
  double manualAim;
  boolean toFire;

  // safety
  boolean safety = false;

  /** Limit Switches */
  DigitalInput limitSwitchUpper = new DigitalInput(0);
  DigitalInput limitSwitchLower = new DigitalInput(1);

  /** IR Sensor (intake) */
  DigitalInput intakeIR = new DigitalInput(2);


  /** Color Sensor */
  private final I2C.Port i2cPort = I2C.Port.kOnboard;
  private final ColorSensorV3 m_colorSensor = new ColorSensorV3(i2cPort);
  private final ColorMatch m_colorMatcher = new ColorMatch();

  private final Color kBlueTarget = ColorMatch.makeColor(0.143, 0.427, 0.429);
  private final Color kGreenTarget = ColorMatch.makeColor(0.197, 0.561, 0.240);
  private final Color kRedTarget = ColorMatch.makeColor(0.441, 0.432, 0.174);
  private final Color kYellowTarget = ColorMatch.makeColor(0.361, 0.524, 0.113);

  /* *****************ROBOT INIT***************** */
  @Override
  public void robotInit() {
    gyroBoy.calibrate();
    m_colorMatcher.addColorMatch(kBlueTarget);
    m_colorMatcher.addColorMatch(kGreenTarget);
    m_colorMatcher.addColorMatch(kRedTarget);
    m_colorMatcher.addColorMatch(kYellowTarget);

    lights.set(lightsOn);

    /* Configure output direction */
    leftMaster.setInverted(false);
    leftSide.setInverted(false);
    rightMaster.setInverted(false);
    rightSide.setInverted(false);

    // turns lights off
    lights.set(lightsOn);

    // Get initial values (Vision)
    targetX = cameraTable.getEntry("yaw");
    poseArray = cameraTable.getEntry("targetPose");
    isValid = cameraTable.getEntry("isValid");
  }


  /*ROBOT PERIODIC*/
  @Override
  public void robotPeriodic() {
    SmartDashboard.putNumber("Aim Yaw: ", rotationOffset);
    SmartDashboard.putNumber("Aim Distance: ", aimDist);
  }

  /* *****************AUTO INIT***************** */
  @Override
  public void autonomousInit() {

  }

  /* *****************AUTO PERIODIC***************** */
  @Override
  public void autonomousPeriodic() {

  }

  /* *****************TELEOP INIT***************** */
  @Override
  public void teleopInit() {
    /** Light on */
    hatchLight.set(true);

  }

  /* *****************TELEOP PERIODIC***************** */
  @Override
  public void teleopPeriodic() {
    /* Vision stuff */
    rotationOffset = targetX.getDouble(0.0);
    poseX = poseArray.getDoubleArray(new double[] { 0.0, 0.0, 0.0 })[0];
    poseY = poseArray.getDoubleArray(new double[] { 0.0, 0.0, 0.0 })[1];

    aimDist = Math.sqrt(Math.pow(poseX, 2) + Math.pow(poseY, 2));
    targetFound = isValid.getBoolean(false);

    /** Drive initialization */
    double forward = 0;
    double turn = 0;

    /** Light on */
    hatchLight.set(true);

    /** gyroscope */
    angle = gyroBoy.getAngle();
    rate = gyroBoy.getRate();
    gyroConnected = gyroBoy.isConnected();

    if (angle > 360) {
      int angleNum = (int) angle % 360;
      angle -= 360 * angleNum;
    }

    if (angle < 0) {
      int angleNum = (int) angle % -360;
      angle += 360 * (angleNum + 1);
    }

    /** Colour sensor */
    Color detectedColor = m_colorSensor.getColor();
    String colorString;
    ColorMatchResult match = m_colorMatcher.matchClosestColor(detectedColor);

    if (match.color == kBlueTarget) {
      colorString = "Blue";
    } else if (match.color == kRedTarget) {
      colorString = "Red";
    } else if (match.color == kGreenTarget) {
      colorString = "Green";
    } else if (match.color == kYellowTarget) {
      colorString = "Yellow";
    } else {
      colorString = "Unknown";
    }

    /******************************
     * Driver Controller (_gamepadDrive)
     ******************************/
    /** Gamepad Drive processing */
    forward = _gamepadDrive.getTriggerAxis(Hand.kRight);
    turn = _gamepadDrive.getTriggerAxis(Hand.kLeft);

    if (forward > 0) {
      if ((forward - prevVal) >= 0.07) {
        forward = prevVal + 0.07;
      }
    } else {
      if ((forward - prevVal) <= -0.07) {
        forward = prevVal - 0.07;
      }
    }

    prevVal = forward;

    if (!reverseControls) {
      forward *= -1;
    }

    // adding a small deadzone
    forward = Deadband(forward);
    turn = Deadband(turn);
    // scaling values for sensitivity
    forward = Scale(forward);
    turn = Scale(turn);

    /** Arcade Drive */
    Drive.arcadeDrive(forward, turn);

    if (safety) {
      forward /= speedDiv;
      turn /= speedDiv;
    }

    /** check safety mode */
    if (_gamepadDrive.getStartButtonPressed()) // start button toggles safety
      safety = !safety;

    /** reverse button */
    if (_gamepadDrive.getBackButton()) // reverse as back button is pressed
      reverse = !reverse;

    /** TODO Elevator */
    // R1 = elevator up
    // L1 = elevator set
    //boolean eleUP = _gamepadDrive.getBumper(Hand.kRight);
    //boolean eleDOWN = _gamepadDrive.getBumper(Hand.kLeft);
    // make it so if both are pressed, nothing happens

    /******************************
     * Shooter Controler (_gamepadShoot)
     ******************************/
    /** Shooting */
    aiming = _gamepadShoot.getRawButton(5); // L1 button
    shootSpeed = _gamepadShoot.getRawAxis(2); // L2 analog
    manualAim = _gamepadShoot.getRawAxis(0); // Left stick analog
    toFire = _gamepadShoot.getRawButton(7); // R1 button

    if (targetFound && aiming) {
      Shooter.set(ControlMode.PercentOutput, getShootSpeed(aimDist)); // auto aim and set speed (ideally)
      if (rotationOffset > angleTolerance) {
        Aim.set(ControlMode.PercentOutput, -0.5); // TODO placeholder 50% power, figure out optimal value
      } else if (rotationOffset < -angleTolerance) {
        Aim.set(ControlMode.PercentOutput, 0.5);
      }
    }

    // manual aim and speed set override
    if (Deadband(manualAim) != 0) { // must use both left stick and L2 analog to fire
      Shooter.set(ControlMode.PercentOutput, shootSpeed);
      Aim.set(ControlMode.PercentOutput, manualAim);
    }

    // bring the POWERCELL up to the firing mech
    if (toFire) {
      toShoot.set(ControlMode.PercentOutput, 1.0);
    }

    /** Intake */
    intakeSpeed = _gamepadShoot.getRawAxis(3); // R2 analog stick (TODO maybe important to add reverse?)

    IntakeWheel.set(ControlMode.PercentOutput, intakeSpeed);
    IntakeBelt.set(ControlMode.PercentOutput, intakeSpeed);

    if (_gamepadShoot.getRawButtonPressed(6)) {
      intakeMove = !intakeMove;
      IntakeUpandDown.set(ControlMode.PercentOutput, intakeMove ? 1.0 : -1.0);
    }

    // TODO install limit switches to stop intakeup/down

    /** Control Panel */
    x = _gamepadShoot.getRawButton(1);
    circle = _gamepadShoot.getRawButton(2);
    triangle = _gamepadShoot.getRawButton(4);
    square = _gamepadShoot.getRawButton(3);

    if (x) {

    }

    if (circle) {

    }

    if (triangle) {

    }

    if (square) {

    }

    /** Smart Dashboard */
    SmartDashboard.putNumber("Angle: ", angle);
    SmartDashboard.putNumber("Rate: ", rate);
    SmartDashboard.putBoolean("gyro Connected: ", gyroConnected);

    SmartDashboard.putNumber("Red: ", detectedColor.red);
    SmartDashboard.putNumber("Green: ", detectedColor.green);
    SmartDashboard.putNumber("Blue: ", detectedColor.blue);
    SmartDashboard.putNumber("Confidence: ", match.confidence);
    SmartDashboard.putString("Detected Color: ", colorString);

    SmartDashboard.putBoolean("Safety: ", safety);
    SmartDashboard.putBoolean("Reverse: ", reverseControls);
  }

  /** Deadband 3 percent, used on the gamepad */
  double Deadband(double value) {
    /* Upper deadband */
    if (value >= +0.02)
      return value;

    /* Lower deadband */
    if (value <= -0.02)
      return value;

    /* Outside deadband */
    return 0;
  }

  /**
   * this method scales the joystick output so that the robot moves slower when
   * the joystick is barely moved, but allows for full power
   */
  double Scale(double value) {
    value *= -1;
    if (value >= -0.9 && value <= 0.9) {
      if (value > 0) {
        value = Math.pow(value, 2);
      } else {
        value = Math.pow(value, 2);
        value *= -1;
      }

    }

    return value;
  }

  // TODO find relationship between percent power of shooterspeed and distance
  // Math: v(d)=sqrt((-10d^2)/(1.5-d)), assuming the angle of velocity is 45deg
  // from horizontal
  // Should add more leniency because real world and potential inaccurate distance
  // measure
  double getShootSpeed(double distance) {
    double percent = 0;
    return percent;
  }

}