/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

/** DESCRIPTION OF THE CLASS
*
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public interface InterfaceSensor {

	/** DESCRIPTION
	 * 
	 */
	public void populate(com.relteq.sirius.jaxb.Sensor s);
	
	/** DESCRIPTION
	 * 
	 */
	public abstract boolean validate();
	
	/** DESCRIPTION
	 * 
	 */
	public abstract void reset();
	
	/** DESCRIPTION
	 * 
	 */
	public abstract void update();
	
	/** DESCRIPTION
	 * 
	 * @return XXX
	 */
	public Double[] getDensityInVeh();
	
	/** DESCRIPTION
	 * 
	 * @return XXX
	 */
	public double getTotalDensityInVeh();
	
	/** DESCRIPTION
	 * 
	 * @return XXX
	 */
	public Double[] getFlowInVeh();
	
	/** DESCRIPTION
	 * 
	 * @return XXX
	 */
	public double getTotalFlowInVeh();
	
	/** DESCRIPTION
	 * 
	 * @return XXX
	 */
	public double getNormalizedSpeed();
	
}
