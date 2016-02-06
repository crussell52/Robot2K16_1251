
package org.usfirst.frc.team1251.robot;

import edu.wpi.first.wpilibj.CounterBase.EncodingType;
import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**Code for prototype testing */

public class Robot extends IterativeRobot {
	
	/**The Following code is used to switch values depending on the controller used
	 X-BOX: cLeft=1, cRight=5, cShoot=3, cIntake=5 |
	 LOGITECH: cLeft=1, cRight=3, cShoot=1, cIntake=5 | 
	**/
	
	//Left and Right stick on the controller
	int cLeft = 1;
	int cRight = 3;
	
	//Button inputs for shooting and disabling the drive rev   
	int cShoot = 1;
	int cSpeed = 6;
	
	//Multiplier for max speed and acceleration rate
	double sMulti = 1.0;
	double accel = 0.05;
	
	//variables for drive acceleration
	double lLimiter, rLimiter, lSpeed, rSpeed;
	
	//variables for testing if the controller is negative
	int lNegative, rNegative;
	
	//variables for physical parts
	RobotDrive mainDrive;
	Joystick controller, cStick;
	Victor shooter;
	DoubleSolenoid solenoid;
	Compressor compressor;
	Encoder eWheel;
	
	//pid variables
	double P = 0.00003;
	double I = 0.0026;
	double D = 0.0002;
	PIDController pid;
	
	AnalogGyro gyro;
	
	boolean rotatingRobot = false;
	
	double desiredAngle;
	
	
    public void robotInit() {
    	//initialize the drive base
    	compressor = new Compressor();
    	mainDrive = new RobotDrive(1, 0);
    	
    	//initialize the controller for the robot
    	controller = new Joystick(0);
    	cStick = new Joystick(1);
    	
    	//initialize the victor
    	shooter = new Victor (2);
    	solenoid = new DoubleSolenoid (0,1);
    	
    	//base values for the drive acceleration 
    	lLimiter = 0.0;
    	rLimiter = 0.0;
    	
    	//initialize the encoder and pid
    	eWheel = new  Encoder(1, 2, true, EncodingType.k4X);
    	eWheel.setPIDSourceType(PIDSourceType.kRate);
    	eWheel.setDistancePerPulse(400);
    	pid = new PIDController(P, I, D, eWheel, shooter);
    	
    	//invert the shooter
    	shooter.setInverted(true);
    	gyro = new AnalogGyro(0);
    	
    	gyro.calibrate();
    	
    	
    	
    	
    }
    
    public void autonomousInit() { 
    	desiredAngle = rotateRobot(10);
    }
    
    public void autonomousPeriodic() { //empty
    	rotateDrivetrain(desiredAngle, 10);
    }
    
    public void teleopInit(){
    	//enable pid and set it's speed
    	
    	pid.enable();
    	pid.setSetpoint(1000);
    	compressor.setClosedLoopControl(true);
    
    	}

    public void teleopPeriodic() {
    	
       	//check if the joysticks are negative
    	if (isNegative(controller.getRawAxis(cLeft)))
    		lNegative = -1;
    	else
    		lNegative = 1;
    	if (isNegative(controller.getRawAxis(cRight)))
    		rNegative = -1;
    	else
    		rNegative = 1;
    	
    	//set the limiter amount 
    	if (controller.getRawAxis(cLeft) > lLimiter)
    		lLimiter = lLimiter + accel;		
    	else 
    		lLimiter = lLimiter - accel;
    	
    	if (controller.getRawAxis(cRight) > rLimiter)
    		rLimiter = rLimiter + accel;		
    	else
    		rLimiter = rLimiter - accel;
    	
    	//if a button is pressed then the rev-up is deactivated
    	if(controller.getRawButton(cSpeed)){
        	lSpeed = controller.getRawAxis(cLeft)* sMulti;
        	rSpeed = controller.getRawAxis(cRight)* sMulti;
        	mainDrive.tankDrive(-lSpeed, -rSpeed);
    	}
    	else {
    		//else this formula is used for drivebase rev-up
        	lSpeed = lNegative*(Math.pow(controller.getRawAxis(cLeft), 2))* sMulti * Math.abs(lLimiter);
        	rSpeed = rNegative*(Math.pow(controller.getRawAxis(cRight), 2))* sMulti * Math.abs(rLimiter);
    		mainDrive.tankDrive(-lSpeed, -rSpeed);
    	}
    	
    	//enable or disable pid if the button is or isn't pressed
    	if (controller.getRawButton(cShoot))
    		pid.enable();
    		
        	
    	else
        	pid.disable();
    	
    	
    	
    	//smart dashboard information
    	SmartDashboard.putNumber("Wheel rpm: ", eWheel.getRate()/120);
    	SmartDashboard.putNumber("shooter: ", shooter.get());
    	SmartDashboard.putData("PID: ", pid);
    	SmartDashboard.putNumber("Gyro", gyro.getRate());
    	
    	if (controller.getRawButton(2)){
    		solenoid.set(Value.kReverse);
    	}else{
    		solenoid.set(Value.kForward);
    	}
    }
    
    public void testPeriodic() { //empty
    	//Temporarily unused
    }
    
    public void disabledInit(){
    	pid.disable();
    	compressor.setClosedLoopControl(false);
    
    	}
    
    
    public boolean isNegative(double d) {
        return Double.compare(d, 0.0) < 0;
   }

	public double rotateRobot(double changeInDegrees){
		double currentAngle = gyro.getAngle();
		double desiredAngle;
		
		if(currentAngle + changeInDegrees > 360){
			desiredAngle = currentAngle + changeInDegrees - 360;
		}else if (currentAngle + changeInDegrees < 0){
			desiredAngle = currentAngle + changeInDegrees + 360;
		}
		else{
			desiredAngle = currentAngle + changeInDegrees;
		}
		rotatingRobot = true;
		return desiredAngle;
	}
	
	public void rotateDrivetrain(double desiredAngle, double changeInDegrees){
		if (!(gyro.getAngle() > desiredAngle + 1 || gyro.getAngle() < desiredAngle - 1)){
			mainDrive.drive(.5, 1);
		}else if (!(gyro.getAngle() < desiredAngle - 1 || gyro.getAngle() > desiredAngle + 1)){
			mainDrive.drive(-.5, -1);
		}else {
			rotatingRobot = false;
		}
	}
    
}


