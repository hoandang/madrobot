//REFERENCES//
//http://robowiki.net/wiki/Wall_Smoothing
//http://robowiki.net/wiki/Stop_And_Go
//http://robowiki.net/wiki/Linear_Targeting
//http://robowiki.net/wiki/Anti-Gravity_Tutorial

package com.hoan.robot;

import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;
import java.awt.Color;

public class Viper extends TeamRobot
{
	final double PI = Math.PI;
	double robotDirection = 1; //direction to move around the field 1 clockwise -1 anti-clockwise
	byte scanDirection = 1;
	static double previousEnergy = 100;	//enemy energy level to detect incoming fire
	
	public void run() {
		setColors(Color.white,Color.red,Color.white); // body, gun, radar color
	    setAdjustRadarForGunTurn(true);
	    setAdjustGunForRobotTurn(true);
	    setAdjustRadarForRobotTurn(true);

		do {
			turnRadarRightRadians(Double.POSITIVE_INFINITY);
		} while(true);
		
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		scanDirection *= -1;
		turnRadarRightRadians(PI/4 * scanDirection);	//on scanning an enemy, spin the radar back and scan in that 45 degree area
		
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();								//bearing relative to our robot
		double lateralVelocity = e.getVelocity() * Math.sin(e.getHeadingRadians() - absoluteBearing);		//velocity perpendicular to our robot
		double bulletSpeed = 11.0;																			//bullet speed at 3 fire power
		
		//movement	
		if((previousEnergy - e.getEnergy()) > 0 && getDistanceRemaining() == 0) {	//check if the enemy bot has fired and if our bot is still
			setAhead(Double.POSITIVE_INFINITY);										//if it has force move and change direction
			robotDirection *= -1;
		}
		previousEnergy = e.getEnergy();
		
		move(e);
		wallSmoothing(absoluteBearing);
		
		if (isTeammate(e.getName())) {				//check if the scanned bot is friendly
			robotDirection *= -1;					//prevent bot radar from locking onto an ally
			setAhead(Double.POSITIVE_INFINITY);
			turnRadarRightRadians(PI/2);
			return;									//don't fire if ally
		}
		
		setTurnGunRightRadians(Utils.normalRelativeAngle((absoluteBearing - getGunHeadingRadians()) + lateralVelocity / bulletSpeed));		//simple linear targeting
		setFire(3.0);
	}
	
	public void move(ScannedRobotEvent e) {
		double rand = Math.random();
		if(rand > 0.5)					//random movement
		{
			setAhead(rand * getBattleFieldHeight()/4);
			setTurnRightRadians(e.getBearingRadians() + PI/2);
		} else {
			setAhead(rand * getBattleFieldHeight()/4);
			setTurnLeftRadians(e.getBearingRadians() + PI/2);
		}
	}
	
	public void wallSmoothing(double absoluteBearing) {								//basic wall smoothing method
		double wallMargin = 20;														//closest distance the bot can get to the wall
		Rectangle2D fieldRect = new Rectangle2D.Double(wallMargin, wallMargin, getBattleFieldWidth()-wallMargin*2,
		    getBattleFieldHeight()-wallMargin*2);
		double goalBearing = absoluteBearing-PI/2*robotDirection;					//the goal - clockwise or counter clockwise depending on robotDirection
		double angle;																//the angle that will be turned to avoid the wall
		final double stickLength = 135;												//the stick length to test if the bot needs to turn
		
		while (!fieldRect.contains(getX()+Math.sin(goalBearing)*stickLength, getY()+Math.cos(goalBearing)*stickLength)) {		//adjust the heading until the stickLength doesn't touch the wall
			goalBearing += robotDirection * 0.01;
		}
		
		angle = Utils.normalRelativeAngle(goalBearing-getHeadingRadians());		//the angle relative to the robot that where the stick no longer toughes the wall
		
		if (Math.abs(angle) < PI/2) {											//if the difference in the goal and current direction is less than 90 degrees continue moving
			setAhead(Double.POSITIVE_INFINITY);
		}
		else {
			angle = Utils.normalRelativeAngle(angle + PI);
			setBack(Double.POSITIVE_INFINITY);
		}
		setTurnRightRadians(angle);
	}
	
	public void onHitByBullet(HitByBulletEvent e) {
		robotDirection *= -1;							//on hit, start traveling the opposite direction
		setAhead(Double.POSITIVE_INFINITY);
	}

	public void onHitWall(HitWallEvent e) {

	}
	
	 public void onHitRobot(HitRobotEvent e) {
		robotDirection *= -1;
        setTurnRightRadians(PI);
		setAhead(Double.POSITIVE_INFINITY);
    }
}
