package com.hoan.robot;

import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;     // for Point2D's
import java.lang.*;         // for Double and Integer objects
import java.util.ArrayList; // for collection of waves
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * This is Leader Robot that aims to targeting the opponent in short distance by using circular targeting strategy 
 * In case of long distance the leader will switch to using wave surfing movement to dodge enemy's bullet and won't fire
 * @author Guangbo Chen, Hoan Dang
 */
public class WaveSufferingRobot extends TeamRobot 
{
    public static int BINS = 47;
    public static double _surfStats[] = new double[BINS]; // we'll use 47 bins
    public Point2D.Double _myLocation;     // our bot's location
    public Point2D.Double _enemyLocation;  // enemy bot's location
 
    public ArrayList _enemyWaves;
    public ArrayList _surfDirections;
    public ArrayList _surfAbsBearings;

	final static double BULLET_POWER=3;//Our bulletpower.
	final static double BULLET_DAMAGE=BULLET_POWER*4;//Formula for bullet damage.
	final static double BULLET_SPEED=20-3*BULLET_POWER;//Formula for bullet speed.
	
	// Variables
	static double dir=1;
	static double oldEnemyHeading;
	static double enemyEnergy;
 
    // EnergyDrop indicating a bullet is fired
    public static double _oppEnergy = 100.0;
 
    // This is a rectangle that represents an 800x600 battle field,
    public static Rectangle2D.Double _fieldRect = new java.awt.geom.Rectangle2D.Double(18, 18, 764, 564);
    // the wall stick indicates the amount of space we try to always have on either end of the tank
    // (extending straight out the front or back) before touching a wall.
    public static double WALL_STICK = 160;
 
    public void run() 
    {
        setColors();
        _enemyWaves = new ArrayList();
        _surfDirections = new ArrayList();
        _surfAbsBearings = new ArrayList();
 
        // Make radar and gun turn independently
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while (true)
            // Quickly twist radar around
            turnRadarRightRadians(Double.POSITIVE_INFINITY);
    }
 
	/**
	 * onScannedRobot: response to do what we do when see another robot
	 */
    public void onScannedRobot(ScannedRobotEvent e) 
    {
        //if is team mate hold the fire
		if (isTeammate(e.getName()))
			return;

        // Using wave suffering to dodge enemy's bullet in a long distance
        waveSuffering (e);

        // if enemy distance is closed less than 500
        // then apply circular targeting strategy
        if (e.getDistance() < 500)
            circularTargeting (e, BULLET_POWER);
    }

    /**
     * 
     */
    public void waveSuffering(ScannedRobotEvent e)
    {
        _myLocation = new Point2D.Double(getX(), getY());
 
        double lateralVelocity = getVelocity()*Math.sin(e.getBearingRadians());
        double absBearing = e.getBearingRadians() + getHeadingRadians();
 
        setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians()) * 2);
 
        _surfDirections.add(0,
            new Integer((lateralVelocity >= 0) ? 1 : -1));
        _surfAbsBearings.add(0, new Double(absBearing + Math.PI));
 
        double bulletPower = _oppEnergy - e.getEnergy();
        if (bulletPower < 3.01 && bulletPower > 0.09
            && _surfDirections.size() > 2) {
            EnemyWave ew = new EnemyWave();
            ew.fireTime = getTime() - 1;
            ew.bulletVelocity = bulletVelocity(bulletPower);
            ew.distanceTraveled = bulletVelocity(bulletPower);
            ew.direction = ((Integer)_surfDirections.get(2)).intValue();
            ew.directAngle = ((Double)_surfAbsBearings.get(2)).doubleValue();
            ew.fireLocation = (Point2D.Double)_enemyLocation.clone(); // last tick
 
            _enemyWaves.add(ew);
        }
 
        _oppEnergy = e.getEnergy();
 
        // update after EnemyWave detection, because that needs the previous
        // enemy location as the source of the wave
        _enemyLocation = project(_myLocation, absBearing, e.getDistance());
 
        updateWaves();
        doSurfing();
    }

    public void circularTargeting(ScannedRobotEvent e, double bulletPower)
    {
		Graphics2D g = getGraphics();

		double absBearing = e.getBearingRadians()+getHeadingRadians();
 
		//Finding the heading and heading change.
		double enemyHeading = e.getHeadingRadians();
		double enemyHeadingChange = enemyHeading - oldEnemyHeading;
		oldEnemyHeading = enemyHeading;
 
		/* This method of targeting is know as circular targeting; you assume your enemy will
		 * keep moving with the same speed and turn rate that he is using at fire time 
		*/
		double deltaTime = 0;
		double predictedX = getX()+e.getDistance()*Math.sin(absBearing);
		double predictedY = getY()+e.getDistance()*Math.cos(absBearing);

		while((++deltaTime) * BULLET_SPEED <  Point2D.Double.distance(getX(), getY(), predictedX, predictedY)) 
        {
 
			//Add the movement we think our enemy will make to our enemy's current X and Y
			predictedX += Math.sin(enemyHeading) * e.getVelocity();
			predictedY += Math.cos(enemyHeading) * e.getVelocity();
 
 
			//Find our enemy's heading changes.
			enemyHeading += enemyHeadingChange;
			
			g.setColor(Color.red);
			g.drawRect((int)predictedX-2,(int)predictedY-2,4,4);
 
			//If our predicted coordinates are outside the walls, put them 18 distance units away from the walls as we know 
			//that that is the closest they can get to the wall (Bots are non-rotating 36*36 squares).
			predictedX=Math.max(Math.min(predictedX,getBattleFieldWidth()-18),18);
			predictedY=Math.max(Math.min(predictedY,getBattleFieldHeight()-18),18);
 
		}

		//Find the bearing of our predicted coordinates from us.
		double aim = Utils.normalAbsoluteAngle(Math.atan2(  predictedX - getX(), predictedY - getY()));
 
		//Aim and fire.
		setTurnGunRightRadians(Utils.normalRelativeAngle(aim - getGunHeadingRadians()));
		setFire(bulletPower);
		setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing-getRadarHeadingRadians())*2);
    }
 
    public void updateWaves() 
    {
        for (int x = 0; x < _enemyWaves.size(); x++) {
            EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
 
            ew.distanceTraveled = (getTime() - ew.fireTime) * ew.bulletVelocity;
            if (ew.distanceTraveled >
                _myLocation.distance(ew.fireLocation) + 50) {
                _enemyWaves.remove(x);
                x--;
            }
        }
    }
 
    public EnemyWave getClosestSurfableWave() 
    {
        double closestDistance = 50000; // I juse use some very big number here
        EnemyWave surfWave = null;
 
        for (int x = 0; x < _enemyWaves.size(); x++) 
        {
            EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
            double distance = _myLocation.distance(ew.fireLocation)
                - ew.distanceTraveled;
 
            if (distance > ew.bulletVelocity && distance < closestDistance) 
            {
                surfWave = ew;
                closestDistance = distance;
            }
        }
 
        return surfWave;
    }
 
    // Given the EnemyWave that the bullet was on, and the point where we
    // were hit, calculate the index into our stat array for that factor.
    public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
        double offsetAngle = (absoluteBearing(ew.fireLocation, targetLocation)
            - ew.directAngle);
        double factor = Utils.normalRelativeAngle(offsetAngle)
            / maxEscapeAngle(ew.bulletVelocity) * ew.direction;
 
        return (int)limit(0,
            (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
            BINS - 1);
    }
 
    // Given the EnemyWave that the bullet was on, and the point where we
    // were hit, update our stat array to reflect the danger in that area.
    public void logHit(EnemyWave ew, Point2D.Double targetLocation) {
        int index = getFactorIndex(ew, targetLocation);
 
        for (int x = 0; x < BINS; x++) {
            // for the spot bin that we were hit on, add 1;
            // for the bins next to it, add 1 / 2;
            // the next one, add 1 / 5; and so on...
            _surfStats[x] += 1.0 / (Math.pow(index - x, 2) + 1);
        }
    }
 
    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        // If the _enemyWaves collection is empty, we must have missed the
        // detection of this wave somehow.
        if (!_enemyWaves.isEmpty()) {
            Point2D.Double hitBulletLocation = new Point2D.Double(
                e.getBullet().getX(), e.getBullet().getY());
            EnemyWave hitWave = null;
 
            // look through the EnemyWaves, and find one that could've hit us.
            for (int x = 0; x < _enemyWaves.size(); x++) {
                EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
 
                if (Math.abs(ew.distanceTraveled -
                    _myLocation.distance(ew.fireLocation)) < 50
                    && Math.abs(bulletVelocity(e.getBullet().getPower()) 
                        - ew.bulletVelocity) < 0.001) {
                    hitWave = ew;
                    break;
                }
            }
 
            if (hitWave != null) {
                logHit(hitWave, hitBulletLocation);
 
                // We can remove this wave now, of course.
                _enemyWaves.remove(_enemyWaves.lastIndexOf(hitWave));
            }
        }
    }

    public Point2D.Double predictPosition(EnemyWave surfWave, int direction) {
    	Point2D.Double predictedPosition = (Point2D.Double)_myLocation.clone();
    	double predictedVelocity = getVelocity();
    	double predictedHeading = getHeadingRadians();
    	double maxTurning, moveAngle, moveDir;
 
        int counter = 0; // number of ticks in the future
        boolean intercepted = false;
 
    	do {
    		moveAngle =
                wallSmoothing(predictedPosition, absoluteBearing(surfWave.fireLocation,
                predictedPosition) + (direction * (Math.PI/2)), direction)
                - predictedHeading;
    		moveDir = 1;
 
    		if(Math.cos(moveAngle) < 0) {
    			moveAngle += Math.PI;
    			moveDir = -1;
    		}
 
    		moveAngle = Utils.normalRelativeAngle(moveAngle);
 
    		// maxTurning is built in like this, you can't turn more then this in one tick
    		maxTurning = Math.PI/720d*(40d - 3d*Math.abs(predictedVelocity));
    		predictedHeading = Utils.normalRelativeAngle(predictedHeading
                + limit(-maxTurning, moveAngle, maxTurning));
 
    		// this one is nice ;). if predictedVelocity and moveDir have
            // different signs you want to breack down
    		// otherwise you want to accelerate (look at the factor "2")
    		predictedVelocity += (predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
    		predictedVelocity = limit(-8, predictedVelocity, 8);
 
    		// calculate the new predicted position
    		predictedPosition = project(predictedPosition, predictedHeading, predictedVelocity);
 
            counter++;
 
            if (predictedPosition.distance(surfWave.fireLocation) <
                surfWave.distanceTraveled + (counter * surfWave.bulletVelocity)
                + surfWave.bulletVelocity) {
                intercepted = true;
            }
    	} while(!intercepted && counter < 500);
 
    	return predictedPosition;
    }
 
    public double checkDanger(EnemyWave surfWave, int direction) {
        int index = getFactorIndex(surfWave,
            predictPosition(surfWave, direction));
 
        return _surfStats[index];
    }
 
    public void doSurfing() {
        EnemyWave surfWave = getClosestSurfableWave();
 
        if (surfWave == null) { return; }
 
        double dangerLeft = checkDanger(surfWave, -1);
        double dangerRight = checkDanger(surfWave, 1);
 
        double goAngle = absoluteBearing(surfWave.fireLocation, _myLocation);
        if (dangerLeft < dangerRight) {
            goAngle = wallSmoothing(_myLocation, goAngle - (Math.PI/2), -1);
        } else {
            goAngle = wallSmoothing(_myLocation, goAngle + (Math.PI/2), 1);
        }
 
        setBackAsFront(this, goAngle);
    }
 
    // This can be defined as an inner class if you want.
    class EnemyWave {
        Point2D.Double fireLocation;
        long fireTime;
        double bulletVelocity, directAngle, distanceTraveled;
        int direction;
 
        public EnemyWave() { }
    }
 
    //  return absolute angle to move at after account for WallSmoothing
    public double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) 
    {
        while (!_fieldRect.contains(project(botLocation, angle, 160)))
            angle += orientation*0.05;
        return angle;
    }
 
    //   - returns point length away from sourceLocation, at angle
    public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length) 
    {
        return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
            sourceLocation.y + Math.cos(angle) * length);
    }
 
    //  - returns the absolute angle (in radians) from source to target points
    public static double absoluteBearing(Point2D.Double source, Point2D.Double target) 
    {
        return Math.atan2(target.x - source.x, target.y - source.y);
    }
 
    public static double limit(double min, double value, double max) 
    {
        return Math.max(min, Math.min(value, max));
    }
 
    public static double bulletVelocity(double power) 
    {
        return (20D - (3D*power));
    }
 
    public static double maxEscapeAngle(double velocity) 
    {
        return Math.asin(8.0/velocity);
    }
 
    public static void setBackAsFront(AdvancedRobot robot, double goAngle) {
        double angle =
            Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
        if (Math.abs(angle) > (Math.PI/2)) {
            if (angle < 0) {
                robot.setTurnRightRadians(Math.PI + angle);
            } else {
                robot.setTurnLeftRadians(Math.PI - angle);
            }
            robot.setBack(100);
        } else {
            if (angle < 0) {
                robot.setTurnLeftRadians(-1*angle);
           } else {
                robot.setTurnRightRadians(angle);
           }
            robot.setAhead(100);
        }
    }

    private void setColors() 
    {
		setBodyColor(Color.yellow);
		setGunColor(Color.black);
		setRadarColor(Color.blue);
		setBulletColor(Color.yellow);
		setScanColor(Color.green);
    }
}

