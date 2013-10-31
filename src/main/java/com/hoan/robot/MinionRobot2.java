package com.hoan.robot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Enumeration;
import java.util.Hashtable;

import robocode.HitByBulletEvent;
import robocode.HitWallEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.TeamRobot;
import robocode.util.Utils;

/**
 * This is Minion Robot that aims to targeting the opponent in long distance if its original birth place has no 
 * enemy nearby(using anti-gravity movement to dodge the bullet and fire weapon with minimum energy), however if it is surround by 
 * the enemies when it starts, it would use another strategy called circular movement and circular targeting, to be simplified, 
 * it will moves around the enemy with a particular circle and even ram it if the distance is really short
 * @author Guangbo Chen, Hoan Dang
 */
public class MinionRobot2 extends WaveSufferingRobot {

	//declare constant variable for circular targeting
	private static double dir=1;
	private static double oldEnemyHeading;
	private final static double BULLET_DAMAGE=BULLET_POWER*4; //formula for bullet damage.
	private final static double BULLET_SPEED=20-3*BULLET_POWER; //formula for bullet speed.
	
	//declare variables for Anti-gravity Movement
	private final static double bulletPower = 1;	//bullet power for anti-gra
	private final double PI = Math.PI;
	private Hashtable targets;	//Hashtable stores all enemies
	private Enemy target;
	private double midpointstrength = 0;	//The strength of the gravity point in the middle of the field
	private int midpointcount = 0;	//Number of turns since that strength was changed.
	
	private static boolean leaderBotIsDead;
	
	public void run() 
    {
		
		//initialize variables
		leaderBotIsDead = false;
		targets = new Hashtable();
		target = new Enemy();
		//best guess for begin distance
		target.distance = 300;
		
		//set my robot colors
		setColors();
		
		//Sets the gun and radar to turn independent from the robot's turn.
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		
        while (true) {
			turnRadarRightRadians(Double.POSITIVE_INFINITY);
        }
    }
	
	/**
	 * onScannedRobot: response to do what we do when see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		
		//if is team mate hold the fire
		if (isTeammate(e.getName())) return;

        // If Leader is dead then switch minion bot to leader
        if (leaderBotIsDead)
        {
            // Using wave suffering to dodge enemy's bullet in a long distance
            waveSuffering (e);

            // if enemy distance is closed less than 500
            // then apply circular targeting strategy
            if (e.getDistance() < 500)
                circularTargeting (e, BULLET_POWER);
        }
        else
        {
            double distance = e.getDistance();
            if(distance > 500)
            {
                antiGraScanning(e); //scanning and add new enemy to the hash table
                antiGravMove();		//move the robot by anti-gra
                circularTargeting(e, 1);
                execute();			//execute all commands
            }
            else
            {
                //if the distance between enemy is less than 400
                //is not survived, using circular targeting strategy
                setTurnRight(90+e.getBearing()-(25*dir));
                if(Math.random() < 0.05) dir=-dir;
                setAhead((e.getDistance()/1.75)*dir);
                circularTargeting(e, 3);
            }
        }
	}
	
	private void antiGraScanning(ScannedRobotEvent e)
	{
			Enemy en;
			if (targets.containsKey(e.getName())) {
				en = (Enemy)targets.get(e.getName());
			} else {
				en = new Enemy();
				targets.put(e.getName(),en);
			}
			//gets the absolute bearing to the point where the robot is
			double absbearing_rad = (getHeadingRadians()+e.getBearingRadians())%(2*PI);
			//this section sets all the information about our target
			en.name = e.getName();
			double h = normaliseBearing(e.getHeadingRadians() - en.heading);
			h = h/(getTime() - en.ctime);
			en.changehead = h;
			en.x = getX()+Math.sin(absbearing_rad)*e.getDistance(); //works out the x coordinate of where the target is
			en.y = getY()+Math.cos(absbearing_rad)*e.getDistance(); //works out the y coordinate of where the target is
			en.bearing = e.getBearingRadians();
			en.heading = e.getHeadingRadians();
			en.ctime = getTime();				//game time at which this scan was produced
			en.speed = e.getVelocity();
			en.distance = e.getDistance();	
			en.live = true;
			if ((en.distance < target.distance)||(target.live == false)) {
				target = en;
			}
	}
	
	
	/**
	 * this method detects whether the robot hits the wall, particularly using by circular targeting
	 */
    public void onHitWall(HitWallEvent e)
    {
    	//if hits the wall turn the direction opponent
		dir=-dir;
    }
    
    
    /**
     * methods for anti-gravity movement
     */
	void antiGravMove() {
   		double xforce = 0, yforce = 0, force, ang;
	    GravPoint p;
		Enemy en;
    	Enumeration e = targets.elements();
	    //cycle through all the enemies.  If they are alive, they are repulsive.  Calculate the force on us
		while (e.hasMoreElements()) {
    	    en = (Enemy)e.nextElement();
			if (en.live) {
				p = new GravPoint(en.x,en.y, -1000);
		        force = p.power/Math.pow(getDistance(getX(),getY(),p.x,p.y),2);
		        //find the bearing from the point to us
		        ang = normaliseBearing(Math.PI/2 - Math.atan2(getY() - p.y, getX() - p.x)); 
		        //add the components of this force to the total force in their respective directions
		        xforce += Math.sin(ang) * force;
		        yforce += Math.cos(ang) * force;
			}
	    }
	    
		/**The next section adds a middle point with a random (positive or negative) strength.
		The strength changes every 5 turns, and goes between -1000 and 1000.  This gives a better
		overall movement.**/
		midpointcount++;
		if (midpointcount > 5) {
			midpointcount = 0;
			midpointstrength = (Math.random() * 2000) - 1000;
		}
		p = new GravPoint(getBattleFieldWidth()/2, getBattleFieldHeight()/2, midpointstrength);
		force = p.power/Math.pow(getDistance(getX(),getY(),p.x,p.y),1.5);
	    ang = normaliseBearing(Math.PI/2 - Math.atan2(getY() - p.y, getX() - p.x)); 
	    xforce += Math.sin(ang) * force;
	    yforce += Math.cos(ang) * force;
	   
	    /**The following four lines add wall avoidance.  They will only affect us if the robot is close 
	    to the walls due to the force from the walls decreasing at a power 3.**/
	    xforce += 5000/Math.pow(getDistance(getX(), getY(), getBattleFieldWidth(), getY()), 3);
	    xforce -= 5000/Math.pow(getDistance(getX(), getY(), 0, getY()), 3);
	    yforce += 5000/Math.pow(getDistance(getX(), getY(), getX(), getBattleFieldHeight()), 3);
	    yforce -= 5000/Math.pow(getDistance(getX(), getY(), getX(), 0), 3);
	    
	    //Move in the direction of our resolved force.
	    goTo(getX()-xforce,getY()-yforce);
	}
	
	/**Move towards an x and y coordinate**/
	void goTo(double x, double y) {
	    double dist = 20; 
	    double angle = Math.toDegrees(absbearing(getX(),getY(),x,y));
	    double r = turnTo(angle);
	    setAhead(dist * r);
	}


	/**
	 * Turns the robot heading to shortest angle and move the robot to the direction
	 */
	private int turnTo(double angle) {
	    double ang;
    	int dir;
	    ang = normaliseBearing(getHeading() - angle);
	    if (ang > 90) {
	        ang -= 180;
	        dir = -1;
	    }
	    else if (ang < -90) {
	        ang += 180;
	        dir = -1;
	    }
	    else {
	        dir = 1;
	    }
	    setTurnLeft(ang);
	    return dir;
	}
	
	/**
	 * if a bearing is within the PI and -PI range, return the origin value
	 */
	private double normaliseBearing(double angle) {
		if (angle > PI) angle -= 2*PI;
		if (angle < -PI) angle += 2*PI;
		return angle;
	}
	
	/**
	 * gets the absolute bearing between two x and y coordinates
	 */
	private double absbearing( double x1,double y1, double x2,double y2 )
	{
		double xo = x2-x1;
		double yo = y2-y1;
		double h = getDistance( x1,y1, x2,y2 );
		if( xo > 0 && yo > 0 ) return Math.asin( xo / h );
		if( xo > 0 && yo < 0 ) return Math.PI - Math.asin( xo / h );
		if( xo < 0 && yo < 0 ) return Math.PI + Math.asin( -xo / h );
		if( xo < 0 && yo > 0 ) return 2.0*Math.PI - Math.asin( -xo / h );
		return 0;
	}
	
	/**
	 * returns the distance between two x and y coordinates
	 */
	private double getDistance( double x1,double y1, double x2,double y2 )
	{
		double xo = x2-x1;
		double yo = y2-y1;
		double distance = Math.sqrt( xo*xo + yo*yo );
		return distance;
	}
	
	/**
	 * this method detects whether robot is dead or not
	 */
	public void onRobotDeath(RobotDeathEvent e) {
		
		//update enemy life status
		if (isTeammate(e.getName()))
			leaderBotIsDead = true;
		else
		{
			//update enemy life status
			Enemy en = (Enemy)targets.get(e.getName());
			en.live = false;		
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
