/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

/** Interface implemented by all controllers.
 * 
 * @author Gabriel Gomes (gomes@path.berkeley.edu)
 */
public interface InterfaceController {
		
	/** Register the controller with its targets. 
	 * 
	 * <p> All controllers must register with their targets in order to be allowed to
	 * manipulate them. This is to prevent clashes, in which two or 
	 * more controllers access the same variable. Use 
	 * {@link Controller#registerFlowController} {@link Controller#registerSpeedController} to register. 
	 * The return value of these methods indicates whether the registration was successful.
	 * 
	 * @return <code>true</code> if the controller successfully registered with all of its targets; 
	 * <code>false</code> otherwise.
	 */
	public boolean register();
}
