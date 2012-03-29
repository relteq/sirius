/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

final class Defaults {
	public static double DURATION 	= 86400f;		// [sec]
	public static double TIME_INIT 	= 0f;			// [sec]
	public static double OUT_DT 	= 300f;			// [sec]
	public static double SIMDT	 	= 10f;			// [sec]
	
	public static String vehicleType = "vehicle";
	
	// fundamental diagram
	public static Double vf					= 60.0;		// [mile/hr]
	public static Double w					= 20.0;		// [mile/hr]
	public static Double densityJam			= 160.0;	// [veh/mile/lane]
	public static Double capacityDrop 		= 0.0;		// [veh/hr/lane]
	public static Double capacity 			= 2400.0;	// [veh/hr/lane]
}
