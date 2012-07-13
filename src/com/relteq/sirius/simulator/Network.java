/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.List;

/** Network in a scenario. 
 * <p>
 * A network is a collection of links, nodes, sensors, and signals that is
 * a) connected and b) limited by terminal nodes on all source and sink links. 
 * All elements within the network can be referred to by element id at the 
 * network level, or by composite (network id,element id) at the scenario level.
 * This class provides access to individual elements (links, nodes,
 * sensors, and signals) and to lists of elements.
* @author Gabriel Gomes
* @version VERSION NUMBER
*/
public final class Network extends com.relteq.sirius.jaxb.Network {

	protected Scenario myScenario;
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(Scenario myScenario) {
		
		this.myScenario = myScenario;
		
		if(getNodeList()!=null)
			for (com.relteq.sirius.jaxb.Node node : getNodeList().getNode())
				((Node) node).populate(this);
		
		if(getLinkList()!=null)
			for (com.relteq.sirius.jaxb.Link link : getLinkList().getLink())
				((Link) link).populate(this);
		
	}
	
	protected boolean validate() {

		if(myScenario.getSimDtInSeconds()<=0){
			SiriusErrorLog.addError("Non-positive simulation step size (" + myScenario.getSimDtInSeconds() +").");
			return false;
		}
		
		// node list
		if(getNodeList()!=null)
			for (com.relteq.sirius.jaxb.Node node : getNodeList().getNode())
				if( !((Node)node).validate() )
					return false;

		// link list
		if(getLinkList()!=null)
			for (com.relteq.sirius.jaxb.Link link : getLinkList().getLink())
				if( !((Link)link).validate() )
					return false;

		return true;
	}

	protected void reset(Scenario.ModeType simulationMode) throws SiriusException {

		// node list
		if(getNodeList()!=null)
			for (com.relteq.sirius.jaxb.Node node : getNodeList().getNode())
				((Node) node).reset();

		// link list
		if(getLinkList()!=null)
			for (com.relteq.sirius.jaxb.Link link : getLinkList().getLink()){
				Link _link = (Link) link;
				_link.resetLanes();		
				_link.resetState(simulationMode);
				_link.resetFD();
			}
	}

	protected void update() throws SiriusException {
		
        // compute link demand and supply ...............
        for(com.relteq.sirius.jaxb.Link link : getLinkList().getLink()){
        	((Link)link).updateOutflowDemand();
        	((Link)link).updateSpaceSupply();
        }
        
        // update nodes: compute flows on links .........
        for(com.relteq.sirius.jaxb.Node node : getNodeList().getNode())
            ((Node)node).update();
        
        // update links: compute densities .............
        for(com.relteq.sirius.jaxb.Link link : getLinkList().getLink())
        	((Link)link).update();
	}

	/////////////////////////////////////////////////////////////////////
	// public API
	/////////////////////////////////////////////////////////////////////

	/** Get link with given id.
	 * @param id String id of the link.
	 * @return Link object.
	 */
	public Link getLinkWithId(String id){
		id.replaceAll("\\s","");
		for(com.relteq.sirius.jaxb.Link link : getLinkList().getLink()){
			if(link.getId().equals(id))
				return (Link) link;
		}
		return null;
	}

	/** Get node with given id.
	 * @param id String id of the node.
	 * @return Node object.
	 */
	public Node getNodeWithId(String id){
		id.replaceAll("\\s","");
		for(com.relteq.sirius.jaxb.Node node : getNodeList().getNode()){
			if(node.getId().equals(id))
				return (Node) node;
		}
		return null;
	}

	/** Get the list of nodes in this network.
	 * @return List of all nodes as jaxb objects. 
	 * Each of these may be cast to a {@link Node}.
	 */
	public List<com.relteq.sirius.jaxb.Node> getListOfNodes() {
		if(getNodeList()==null)
			return null;
		if(getNodeList().getNode()==null)
			return null;
		return getNodeList().getNode();
	}

	/** Get the list of links in this network.
	 * @return List of all links as jaxb objects. 
	 * Each of these may be cast to a {@link Link}.
	 */
	public List<com.relteq.sirius.jaxb.Link> getListOfLinks() {
		if(getLinkList()==null)
			return null;
		if(getLinkList().getLink()==null)
			return null;
		return getLinkList().getLink();	
	}

//	/** Get the list of sensors in this network.
//	 * @return List of all sensors. 
//	 */
//	public List<com.relteq.sirius.jaxb.Sensor> getListOfSensors() {
//		if(getSensorList()==null)
//			return null;
//		return getSensorList().getSensor();
//	}
//
//	/** Get the list of signals in this network.
//	 * @return List of all signals. 
//	 */
//	public List<com.relteq.sirius.jaxb.Signal> getListOfSignals() {
//		if(getSignalList()==null)
//			return null;
//		return getSignalList().getSignal();
//	}
	
}
