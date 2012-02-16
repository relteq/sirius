/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

final class _ScenarioElement {
	
	protected static enum Type {NULL, link, 
									  node,
									  controller,
									  sensor,
									  event,
									  signal };
		    
	protected _ScenarioElement.Type myType;
	protected String network_id = "";
	protected String id;
	protected Object reference;
	
	public _ScenarioElement(com.relteq.sirius.jaxb.ScenarioElement s){
		this.id = s.getId();
		this.network_id = s.getNetworkId();
		if(id.equalsIgnoreCase("link")){
			this.myType = _ScenarioElement.Type.link;
			this.reference = API.getLinkWithCompositeId(network_id,id);
		}
		if(id.equalsIgnoreCase("node")){
			this.myType = _ScenarioElement.Type.node;
			this.reference = API.getNodeWithCompositeId(network_id,id);
		}
	}
	
}
