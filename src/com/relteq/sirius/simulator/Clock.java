/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

public class Clock {
	protected double t;					// [sec]
	protected double to;					// [sec]
	protected double dt;					// [sec]
	protected double maxt;				// [sec]
	protected int currentstep;			// [sec]
	
	public Clock(double to,double tf,double dt){
		this.t = to;
		this.to = to;
		this.dt = dt;
		this.maxt = tf;
		this.currentstep = 0;
	}
	
	public void reset(){
		t = to;
		currentstep = 0;
	}

	public double getT() {
		return t;
	}

	public int getCurrentstep() {
		return currentstep;
	}
	
	protected void advance(){
		currentstep++;
		t = to + currentstep*dt;
	}
	
	public boolean expired(){
		return t>maxt;
	}

	public boolean istimetosample(int samplesteps,int stepinitial){	
		if(currentstep<=1)
			return true;
		if(currentstep<stepinitial)
			return false;
		return (currentstep-stepinitial) % samplesteps == 0;
	}
	
}
