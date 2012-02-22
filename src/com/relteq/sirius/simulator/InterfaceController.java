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
public interface InterfaceController {
	
	/** DESCRIPTION
	 * 
	 */
	public void populate(com.relteq.sirius.jaxb.Controller c);
	
	/** DESCRIPTION
	 * 
	 */
	public boolean validate();
	
	/** DESCRIPTION
	 * 
	 */
	public void reset();
	
	/** DESCRIPTION
	 * 
	 */
	public void update();
	
	/** DESCRIPTION
	 * 
	 */
	public boolean register();
}
