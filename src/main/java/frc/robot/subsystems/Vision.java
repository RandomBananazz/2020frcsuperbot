package frc.robot.subsystems;

import edu.wpi.first.networktables.*;
import frc.robot.Constants;

public class Vision {
    private static NetworkTableInstance table;
    private static NetworkTable cameraTable;
    private static double currentAngle;
    private static double poseX;
    private static double poseY;
    private static double poseAngle;
    private static boolean targetFound;

    public static void init(){
        table = NetworkTableInstance.getDefault();
        cameraTable = table.getTable("chameleon-vision").getSubTable(Constants.VISION.cameraName);
    }

    public static void periodic(){
        currentAngle = cameraTable.getEntry("yaw").getDouble(0.0);
        poseX = cameraTable.getEntry("targetPose").getDoubleArray(new double[] { 0.0, 0.0, 0.0 })[0];
        poseY = cameraTable.getEntry("targetPose").getDoubleArray(new double[] { 0.0, 0.0, 0.0 })[1];
        poseAngle = cameraTable.getEntry("targetPose").getDoubleArray(new double[] { 0.0, 0.0, 0.0 })[2];
        targetFound = cameraTable.getEntry("isValid").getBoolean(false);
    }

    public static double getX(){
        return poseX;
    }

    public static double getY(){
        return poseY;
    }

    public static boolean targetFound(){
        return targetFound;
    }

    public static double getAngle(){
        return currentAngle;
    }

    public static double getPoseAngle(){
        return poseAngle;
    }
}