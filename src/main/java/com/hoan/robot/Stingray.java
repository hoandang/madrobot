package com.hoan.robot;
import robocode.*;
import java.awt.Color;

public class Stingray extends TeamRobot {

    private Enemy target;                 
    private int direction = 1;             
    private double firePower;              

    public void run() {
        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForRobotTurn(true);
        target = new Enemy();
        target.distance = 100000;               // initialise the distance so that we can select a target
        setColors(Color.black, Color.yellow, Color.yellow); // (robot, gun, radar)
        setMaxVelocity(8);
        setAhead(10000 * direction);
        // turns of the robot, gun and radar are independent
        turnRadarRightRadians(2 * Math.PI);          //turns the radar right around to get a view of the field
        while(true) {

            doMovement();                 
            doFirePower();             
            doScanner();                 
            doGun();
            doFire();
            execute();                    
        }
    }

    void doFire() {
        // if the gun is cool and we're pointed at the target, shoot!
        if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 20 && target.distance < 600)
            setFire(firePower);
    }

    void doFirePower() {
        firePower = 400 / target.distance; // selects a power based on our distance away from the target
        if (firePower < 1.5) {
            firePower = 1.5; // firepower of at least 1 does extra damage
        }
    }

    void doMovement() {
        // set direcion to move in
        if (target.distance < 200) {
            setTurnRightRadians(target.bearing + (Math.PI/2) + (direction * 1)); // move away from target if too close
        } else if (target.distance > 400) {
            setTurnRightRadians(target.bearing + (Math.PI/2) - (direction * 1)); // move in on target if too far away
        } else {
            setTurnRightRadians(target.bearing + (Math.PI/2)); // distance ok, circle strafe the enemy
        }

        // normal movement: switch directions if we've stopped
        if (getVelocity() == 0) {
            direction *= -1;
            setTurnRightRadians(Math.PI /2);
            setAhead(Double.POSITIVE_INFINITY);
        }

    }

    void doScanner() {
        double radarOffset;
        if (getTime() - target.ctime > 4) {     // if we haven't seen anybody for a bit....
            radarOffset = 360;              // rotate the radar to find a target
        } else {
            // next is the amount we need to rotate the radar by to scan where the target is now
            radarOffset = getRadarHeadingRadians() - absbearing(getX(), getY() ,target.x, target.y);

            //this moves the radar slightly so the target isnt lost
            if (radarOffset < 0) {
                radarOffset -= Math.PI / 8;
            } else {
                radarOffset += Math.PI / 8;
            }
        }
        // turn the radar
        setTurnRadarLeftRadians(normaliseBearing(radarOffset));
    }

    void doGun() {
        // works out how long it would take a bullet to travel to where the enemy is *now*
        // this is the best estimation we have
        long time = getTime() + (int)(target.distance / (20 - (3 * firePower)));

        //offsets the gun by the angle to the next shot based on linear targeting provided by the enemy class
        double gunOffset = getGunHeadingRadians() - absbearing(getX(), getY(), target.guessX(time), target.guessY(time));
        setTurnGunLeftRadians(normaliseBearing(gunOffset));
    }

    public void onScannedRobot(ScannedRobotEvent e) {


        if (isTeammate(e.getName())) {//check if the scanned bot is friendly
            direction *= -1;//prevent bot radar from locking onto an ally
            setAhead(Double.POSITIVE_INFINITY);
            turnRadarRightRadians(Math.PI/2);
            return;//don't fire if ally
        }

        if ((e.getDistance() < target.distance) || (target.name == e.getName())) {
            // the next line gets the absolute bearing to the point where the bot is
            double absbearing_rad = (getHeadingRadians() + e.getBearingRadians()) % (2 * Math.PI);
            // this section sets all the information about our target
            target.name = e.getName();
            target.x = getX() + Math.sin(absbearing_rad) * e.getDistance(); // works out the x coordinate of where the target is
            target.y = getY() + Math.cos(absbearing_rad) * e.getDistance(); // works out the y coordinate of where the target is
            target.bearing = e.getBearingRadians();
            target.head = e.getHeadingRadians();
            target.ctime = getTime();               // game time at which this scan was produced
            target.speed = e.getVelocity();
            target.distance = e.getDistance();
            target.energy = e.getEnergy();
        }
    }

    public void onRobotDeath(RobotDeathEvent e) {
        if (e.getName() == target.name) {
            target.distance = 10000; // look for new enemy
        }
    }

    public void onHitRobot(HitRobotEvent e) {
        setTurnRightRadians(Math.PI /2);
        setAhead(Double.POSITIVE_INFINITY);
    }

    public void onHitWall(HitWallEvent e) {
        direction *= -1;
        setTurnRightRadians(Math.PI /2);
        setAhead(Double.POSITIVE_INFINITY);
    }

    public void onHitByBullet(HitByBulletEvent e) {
        direction *= -1;
    }

    //Helper stuff
    class Enemy {
        String name;
        public double bearing;
        public double head;
        public long ctime; // game time that the robot was scanned
        public double speed;
        public double x,y;
        public double distance;
        public double energy;
        public double guessX(long when) {
            long diff = when - ctime;
            return x + Math.sin(head) * speed * diff;
        }
        public double guessY(long when) {
            long diff = when - ctime;
            return y + Math.cos(head) * speed * diff;
        }
    }

    // if a bearing is not within the -Math.PI to Math.PI range, alters it to provide the shortest angle
    private double normaliseBearing(double ang) {
        if (ang > Math.PI) {
            ang -= 2 * Math.PI;
        }
        if (ang < -Math.PI) {
            ang += 2 * Math.PI;
        }
        return ang;
    }

    // if a heading is not within the 0 to 2Math.PI range, alters it to provide the shortest angle


    // returns the distance between two x,y coordinates
    public double getRange(double x1, double y1, double x2,double y2) {
        double xo = x2 - x1;
        double yo = y2 - y1;
        double h = Math.sqrt( xo * xo + yo * yo );
        return h;
    }

    // gets the absolute bearing between two x,y coordinates
    public double absbearing(double x1, double y1, double x2,double y2) {
        double xo = x2 - x1;
        double yo = y2 - y1;
        double h = getRange(x1, y1, x2, y2);
        if (xo > 0 && yo > 0) {
            return Math.asin( xo / h );
        }
        if (xo > 0 && yo < 0) {
            return Math.PI - Math.asin( xo / h );
        }
        if (xo < 0 && yo < 0) {
            return Math.PI + Math.asin(-xo / h);
        }
        if(xo < 0 && yo > 0) {
            return 2.0 * Math.PI - Math.asin(-xo / h);
        }
        return 0;
    }
}


