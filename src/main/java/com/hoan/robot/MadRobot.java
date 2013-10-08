package com.hoan.robot;
import robocode.*;
import java.awt.Color;

public class MadRobot extends Robot
{
	public void run() 
	{

		// Set colors
		setBodyColor   ( Color.black );
		setGunColor	   ( Color.red );
		setRadarColor  ( Color.orange );
		setBulletColor ( Color.black );
		setScanColor   ( Color.black );

		// Robot main loop
		while ( true ) 
		{
			ahead ( 100 ); // Move ahead 100
			back ( 100 ); // Move back 100
		}
	}

	// Fire when we see a robot
	public void onScannedRobot(ScannedRobotEvent e) 
	{
		fire ( 1 );
	}

	//  Turn percendicular when we got hit
	public void onHitByBullet(HitByBulletEvent e) 
	{
		turnLeft ( 90 - e.getBearing() );
	}
}
