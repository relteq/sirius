/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

public class _ScenarioElement {
	public Types.ScenarioElement myType;
	public String network_id = "";
	public String id;
	public Object reference;
	
	public _ScenarioElement(com.relteq.sirius.jaxb.ScenarioElement s){
		this.id = s.getId();
		this.network_id = s.getNetworkId();
		if(id.equalsIgnoreCase("link")){
			this.myType = Types.ScenarioElement.link;
			this.reference = Utils.getLinkWithCompositeId(network_id,id);
		}
		if(id.equalsIgnoreCase("node")){
			this.myType = Types.ScenarioElement.node;
			this.reference = Utils.getNodeWithCompositeId(network_id,id);
		}
	}
	
}
