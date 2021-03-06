package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import edu.wpi.first.wpilibj.Timer;
import frc.robot.Constants;

public class Motors{
    public static VictorSPX intakeBelt;
    public static CANSparkMax turretFly;
    public static VictorSPX turretAim;
    public static CANSparkMax turretLoad;
    public static TalonSRX elevator;
    public static Timer motorTimer;
    public static final double angleTolerance = 5; // Deadzone for turret aim
    public static final double aimSpeed = 0.15; // Default auto aim speed

    public static void init(){
        intakeBelt = new VictorSPX(Constants.CAN.intakeBelt);
        turretFly = new CANSparkMax(Constants.CAN.turretFly, MotorType.kBrushless);
        turretAim = new VictorSPX(Constants.CAN.turretAim);
        turretLoad = new CANSparkMax(Constants.CAN.turretLoad, MotorType.kBrushed);
        elevator = new TalonSRX(Constants.CAN.elevator);
    }

    public static boolean aimTurret() {
        double angle = Vision.getCurrentAngle();
        if (Vision.getTargetFound() && angle <= -angleTolerance){
            turretAim.set(ControlMode.PercentOutput, aimSpeed);
            return false;
        } else if (Vision.getTargetFound() && angle >= angleTolerance){
            turretAim.set(ControlMode.PercentOutput, -aimSpeed);
            return false;
        } else if (Vision.getTargetFound()) {
            turretFly.set(1);
            return true;
        }
        return false;
    }

    public static void loadTurret(boolean on) {
        if (on) {
            intakeBelt.set(ControlMode.PercentOutput, 1);
            turretLoad.set(1);
        } else {
            intakeBelt.set(ControlMode.PercentOutput, 0);
            turretLoad.set(0);
        }
    }

    public static void aimOff() {
        turretFly.set(0);
        turretAim.set(ControlMode.PercentOutput, 0);
    }

    public static void liftElevator(boolean RB, boolean LB, boolean upperLimit, boolean lowerLimit) {
        if (RB && !LB && lowerLimit) {
            elevator.set(ControlMode.PercentOutput, -1); // RB to drop
        } else if (LB && !RB && upperLimit) {
            elevator.set(ControlMode.PercentOutput, 1); // LB to lift
        } else {
            elevator.set(ControlMode.PercentOutput, 0);
        }
    }

    public static void manualAim(double speed) {
        speed = Constants.deadband(speed);
        if (speed == 0) {
            turretAim.set(ControlMode.PercentOutput, 0);
        } else {
            turretAim.set(ControlMode.PercentOutput, (speed < 0) ? -aimSpeed : aimSpeed);
        }
    }

    public static void manualIntake(double speed) {
        intakeBelt.set(ControlMode.PercentOutput, speed);
    }

    public static void manualFly(double speed) {
        turretFly.set(speed);
    }

    public static void manualLoad(double speed) {
        turretLoad.set(speed);
    }
}