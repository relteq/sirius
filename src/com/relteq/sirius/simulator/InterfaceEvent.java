/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

/** Interface implemented by all events.
* 
* @author Gabriel Gomes (gomes@path.berkeley.edu)
*/
public interface InterfaceEvent {
	
	/** Activate the event.
	 * 
	 * <p> Called once by {@link Scenario#run} at the event time stamp.
	 * Executes all changes to the scenario caused by the event.
	 * @throws SiriusException
	 */
	public void activate() throws SiriusException;
}
