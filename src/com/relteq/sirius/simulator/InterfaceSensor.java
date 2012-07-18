/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

/** Interface implemented by all sensors.
*
* @author Gabriel Gomes (gomes@path.berkeley.edu)
*/
public interface InterfaceSensor {

	/** Measured density per vehicle type in veh/mile. 
	 * 
	 * <p> The output array contains measured densities.
	 * The array is organized by vehicle type in the order in which they appear in the 
	 * <code>settings</code> block of the configuration file (see {@link Scenario#getVehicleTypeNames}).
	 * 
	 * @return Array of densities.
	 */
	public Double[] getDensityInVPM(int ensemble);
	
	/** Measured total density in veh/mile. 
	 * 
	 * <p> Returns the total density measured by the sensor.
	 * Must equal the sum of values in {@link InterfaceSensor#getDensityInVPM}.
	 * 
	 * @return A double with the total measured density in veh/mile.	 
	 */
	public double getTotalDensityInVPM(int ensemble);

	/** Measured total density in veh/link. 
	 * 
	 * <p> Returns the total density measured by the sensor averaged over the links it's in.
	 * 
	 * @return A double with the total measured density in veh/link.	 
	 */
	public double getTotalDensityInVeh(int ensemble);
	
	/** Measured total occupancy in a number between 0 and 100. 
	 * 
	 * <p> Returns the occupancy  measured by the sensor.	 * 
	 * 
	 * @return A double with the total occupancy, with values between 0 and 100.	 
	 */
	public double getOccupancy(int ensemble);
	
	
	/** Measured flow per vehicle type in veh/hr. 
	 * 
	 * <p> The output array contains measured flows.
	 * The array is organized by vehicle type in the order in which they appear in the 
	 * <code>settings</code> block of the configuration file (see {@link Scenario#getVehicleTypeNames}).
	 * 
	 * @return Array of flows.
	 */	
	
	public Double[] getFlowInVPH(int ensemble);
	
	/** Measured total flow in veh/hr. 
	 * 
	 * <p> Returns the total flow measured by the sensor.
	 * Must equal the sum of values in {@link InterfaceSensor#getFlowInVPH}.
	 * 
	 * @return A double with the total measured flow in veh/hr.	 
	 */
	public double getTotalFlowInVPH(int ensemble);
	
	/** Measured speed in mile/hr. 
	 * 
	 * <p> Returns the speed measured by the sensor.
	 * 
	 * @return A double with the measured speed in mile/hr.	 
	 */
	public double getSpeedInMPH(int ensemble);
	
}
