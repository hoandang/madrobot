package com.hoan.robot;
import java.awt.geom.Point2D;

class Enemy {
	/*
	 * ok, we should really be using accessors and mutators here,
	 * (i.e getName() and setName()) but life's too short.
	 */
	String name;
	public double bearing,heading,speed,x,y,distance,changehead;
	public long ctime; 		//game time that the scan was produced
	public boolean live; 	//is the enemy alive?
	public Point2D.Double guessPosition(long when) {
		double diff = when - ctime;
		double newY = y + Math.cos(heading) * speed * diff;
		double newX = x + Math.sin(heading) * speed * diff;
		
		return new Point2D.Double(newX, newY);
	}
}
