package com.hoan.robot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

import robocode.BulletHitEvent;
import robocode.ScannedRobotEvent;
import robocode.TeamRobot;
import robocode.util.Utils;

public class MinionRobot extends TeamRobot{
	static final double MAX_VELOCITY = 8;
    static final double WALL_MARGIN = 25;
    Point2D robotLocation;
    Point2D enemyLocation;
    double enemyDistance;
    double enemyAbsoluteBearing;
    double movementLateralAngle = 0.2;
	
	final static double BULLET_POWER=2.15;//Our bulletpower.
	final static double BULLET_DAMAGE=BULLET_POWER*4;//Formula for bullet damage.
	final static double BULLET_SPEED=20-3*BULLET_POWER;//Formula for bullet speed.
 
	//Variables
	static double dir=1;
	static double oldEnemyHeading;
	static double enemyEnergy;

	int scannedX = Integer.MIN_VALUE;
 	int scannedY = Integer.MIN_VALUE;

    public void run() {
        setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);
		setColors(Color.yellow,Color.orange,Color.white);

        do {
            turnRadarRightRadians(Double.POSITIVE_INFINITY); 
        } while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        robotLocation = new Point2D.Double(getX(), getY());
        enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
        enemyDistance = e.getDistance();
        enemyLocation = vectorToLocation(enemyAbsoluteBearing, enemyDistance, robotLocation);

	move();
	
double angle = Math.toRadians((getHeading() + e.getBearing()) % 360);
	scannedX = (int)(getX() + Math.sin(angle) * e.getDistance());
  	   scannedY = (int)(getY() + Math.cos(angle) * e.getDistance());
	
	Graphics2D g=getGraphics();
	
		if (isTeammate(e.getName())) {
			return;
		}
        setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
		
		double absBearing=e.getBearingRadians()+getHeadingRadians();
 
 
		//Finding the heading and heading change.
		double enemyHeading = e.getHeadingRadians();
		double enemyHeadingChange = enemyHeading - oldEnemyHeading;
		oldEnemyHeading = enemyHeading;
 
		/*This method of targeting is know as circular targeting; you assume your enemy will
		 *keep moving with the same speed and turn rate that he is using at fire time.The 
		 *base code comes from the wiki.
		*/
		double deltaTime = 0;
		double predictedX = getX()+e.getDistance()*Math.sin(absBearing);
		double predictedY = getY()+e.getDistance()*Math.cos(absBearing);
		while((++deltaTime) * BULLET_SPEED <  Point2D.Double.distance(getX(), getY(), predictedX, predictedY)){	
 
			//Add the movement we think our enemy will make to our enemy's current X and Y
			predictedX += Math.sin(enemyHeading) * e.getVelocity();
			predictedY += Math.cos(enemyHeading) * e.getVelocity();
 
 
			//Find our enemy's heading changes.
			enemyHeading += enemyHeadingChange;
 
			//If our predicted coordinates are outside the walls, put them 18 distance units away from the walls as we know 
			//that that is the closest they can get to the wall (Bots are non-rotating 36*36 squares).
			predictedX=Math.max(Math.min(predictedX,getBattleFieldWidth()-18),18);
			predictedY=Math.max(Math.min(predictedY,getBattleFieldHeight()-18),18);
			
 			g.setColor(Color.green);
			g.drawOval((int)predictedX-2,(int)predictedY-2,4,4);
 
		}
		//Find the bearing of our predicted coordinates from us.
		double aim = Utils.normalAbsoluteAngle(Math.atan2(  predictedX - getX(), predictedY - getY()));
 
		//Aim and fire.
		if (getEnergy() < 3) {
			return;
		}
		setTurnGunRightRadians(Utils.normalRelativeAngle(aim - getGunHeadingRadians()));
		setFire(BULLET_POWER);
    }

    void move() {
	considerChangingDirection();
	Point2D robotDestination = null;
	double tries = 0;
	do {
	    robotDestination = vectorToLocation(absoluteBearing(enemyLocation, robotLocation) + movementLateralAngle,
		    enemyDistance * (1.1 - tries / 100.0), enemyLocation);
	    tries++;
	} while (tries < 100 && !fieldRectangle(WALL_MARGIN).contains(robotDestination));
	goTo(robotDestination);
    }

    void considerChangingDirection() {
	
	double flattenerFactor = 0.05;
	if (Math.random() < flattenerFactor) {
	    movementLateralAngle *= -1;
	}
    }

    RoundRectangle2D fieldRectangle(double margin) {
        return new RoundRectangle2D.Double(margin, margin,
	    getBattleFieldWidth() - margin * 2, getBattleFieldHeight() - margin * 2, 75, 75);
    }

    void goTo(Point2D destination) {
        double angle = Utils.normalRelativeAngle(absoluteBearing(robotLocation, destination) - getHeadingRadians());
	double turnAngle = Math.atan(Math.tan(angle));
        setTurnRightRadians(turnAngle);
        setAhead(robotLocation.distance(destination) * (angle == turnAngle ? 1 : -1));
		
	setMaxVelocity(Math.abs(getTurnRemaining()) > 33 ? 0 : MAX_VELOCITY);
    }

    static Point2D vectorToLocation(double angle, double length, Point2D sourceLocation) {
	return vectorToLocation(angle, length, sourceLocation, new Point2D.Double());
    }

    static Point2D vectorToLocation(double angle, double length, Point2D sourceLocation, Point2D targetLocation) {
        targetLocation.setLocation(sourceLocation.getX() + Math.sin(angle) * length,
            sourceLocation.getY() + Math.cos(angle) * length);
	return targetLocation;
    }

    static double absoluteBearing(Point2D source, Point2D target) {
        return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }
    
	public void onBulletHit(BulletHitEvent e){
		Graphics2D g=getGraphics();
		
	 g.setColor(Color.blue);
 
     g.drawLine(scannedX, scannedY, (int)getX(), (int)getY());
	 
 	 g.setColor(Color.white);
     g.fillOval(scannedX - 50, scannedY - 50, 10, 10);
	}
}
