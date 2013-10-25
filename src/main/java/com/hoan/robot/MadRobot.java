package com.hoan.robot;
import robocode.*;
import java.awt.Color;

public class MadRobot extends Robot
{
	public void run () 
	{

		// Set colors
		setBodyColor   ( Color.black );
		setGunColor	   ( Color.red );
		setRadarColor  ( Color.orange );
		setBulletColor ( Color.red );
		setScanColor   ( Color.black );

		// Robot main loop
		while ( true ) 
		{
			ahead (100); // Move ahead 100
            // if (getX() == 800 || getY() == 600)
            // {
                // System.out.println ("Current X: " + getX());
                // System.out.println ("Current Y: " + getY());
                // back (100);
            // }
            // turnGunRight (360);
			// back (100); // Move back 100
            // turnGunRight (360);
            // turnGunRight (10);
            // System.out.println ("heading "+ getHeading());
            // System.out.println ("gun heading" + getGunHeading());
            // System.out.println ("radar heading " + getRadarHeading());
		}
	}

	// Fire when we see a robot
	public void onScannedRobot (ScannedRobotEvent e) 
	{
		fire (1);
	}

    @Override
    public void onHitWall (HitWallEvent event)
    {
        back (100);
    }

    @Override
	//  Turn percendicular when we got hit
    public void onHitByBullet (HitByBulletEvent event)
    {
		turnLeft (90 - e.getBearing());
    }
}
