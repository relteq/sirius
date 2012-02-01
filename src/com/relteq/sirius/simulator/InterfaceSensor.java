/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

public interface InterfaceSensor {
	
	public abstract boolean validate();
	public abstract void reset();
	public abstract void update();
	
	public Double[] getDensityInVeh();
	public double getTotalDensityInVeh();
	public Double[] getFlowInVeh();
	public double getTotalFlowInVeh();
	public double getNormalizedSpeed();
	
}
