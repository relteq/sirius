/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;
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
	protected SensorList sensorlist = new SensorList();
	protected SignalList signallist = new SignalList();
	
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
		
		sensorlist.populate(this);
		signallist.populate(myScenario,this);
		
	}

	protected boolean validate() {

		if(myScenario.getSimDtInSeconds()<=0){
			SiriusErrorLog.addErrorMessage("Negative simulation time (" + myScenario.getSimDtInSeconds() +").");
			return false;
		}
		
		// node list
		if(getNodeList()!=null)
			for (com.relteq.sirius.jaxb.Node node : getNodeList().getNode())
				if( !((Node)node).validate() ){
					SiriusErrorLog.addErrorMessage("Node validation failure, node " + node.getId());
					return false;
				}

		// link list
		if(getLinkList()!=null)
			for (com.relteq.sirius.jaxb.Link link : getLinkList().getLink())
				if( !((Link)link).validate() ){
					SiriusErrorLog.addErrorMessage("Link validation failure, link " + link.getId());
					return false;
				}

		// sensor list
		if(!sensorlist.validate())
			return false;


		// signal list
		if(!signallist.validate())
			return false;
		
//		// signal list
//		if(getSignalList()!=null)
//			for (Signal signal : getSignalList().getSignal())
//				if( !((_Signal)signal).validate() ){
//					SiriusErrorLog.addErrorMessage("Signal validation failure.");
//					return false;
//				}

		return true;
	}

	protected void reset(Scenario.ModeType simulationMode) throws SiriusException {

		// link list
		if(getLinkList()!=null)
			for (com.relteq.sirius.jaxb.Link link : getLinkList().getLink()){
				Link _link = (Link) link;
				_link.resetLanes();		
				_link.resetState(simulationMode);
				_link.resetFD();
			}

		// sensor list
		sensorlist.reset();

		
		// signal list
		signallist.reset();
		
//		if(getSignalList()!=null)
//			for (Signal signal : getSignalList().getSignal())
//				((_Signal)signal).reset();
				
	}

	protected void update() throws SiriusException {
		
        // compute link demand and supply ...............
        for(com.relteq.sirius.jaxb.Link link : getLinkList().getLink()){
        	((Link)link).updateOutflowDemand();
        	((Link)link).updateSpaceSupply();
        }
        
        // update sensor readings .......................
        sensorlist.update();
        
        // update signals ...............................
        signallist.update();
//        if(getSignalList()!=null)
//	        for(Signal signal : getSignalList().getSignal())
//	        	((_Signal)signal).update();
        
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
	
	/** Get sensors on a given link.
	 * @param String id of the link.
	 * @return The list of sensors located in the link.
	 */
	public ArrayList<Sensor> getSensorWithLinkId(String linkid){
		ArrayList<Sensor> result = new ArrayList<Sensor>();
		for(Sensor sensor : sensorlist.sensors){
			if(sensor.myLink!=null){
				if(sensor.myLink.getId().equals(linkid)){
					result.add(sensor);
					break;
				}	
			}
		}
		return result;
	}

	/** Get one sensor in the given link.
	 * @param String id of the link.
	 * @return The first sensor found to be contained in the link. 
	 */
	public Sensor getFirstSensorWithLinkId(String linkid){
		for(Sensor sensor : sensorlist.sensors){
			if(sensor.myLink!=null){
				if(sensor.myLink.getId().equals(linkid)){
					return sensor;
				}
			}
		}
		return null;
	}

	/** Get sensor with given id.
	 * @param String id of the sensor.
	 * @return Sensor object.
	 */
	public Sensor getSensorWithId(String id){
		id.replaceAll("\\s","");
		for(Sensor sensor : sensorlist.sensors){
			if(sensor.id.equals(id))
				return sensor;
		}
		return null;
	}

	/** Get signal with given id.
	 * @param String id of the signal.
	 * @return Signal object.
	 */
	public Signal getSignalWithId(String id){
		id.replaceAll("\\s","");
		for(Signal signal : signallist.signals){
			if(signal.getId().equals(id))
				return signal;
		}
		return null;
	}

	/** Get signal on the node with given id.
	 * @param String id of the node.
	 * @return Signal object if there is one. <code>null</code> otherwise. 
	 */
	public Signal getSignalWithNodeId(String node_id){
		id.replaceAll("\\s","");
		for(Signal signal : signallist.signals){
			if(signal.getNodeId().equals(node_id))
				return signal;
		}
		return null;
	}

	/** Get link with given id.
	 * @param String id of the link.
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
	 * @param String id of the node.
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

	/** Get the list of sensors in this network.
	 * @return List of all sensors. 
	 */

	public List<com.relteq.sirius.jaxb.Sensor> getListOfSensors() {
		if(getSensorList()==null)
			return null;
		return getSensorList().getSensor();
	}

	/** Get the list of signals in this network.
	 * @return List of all signals. 
	 */
	public ArrayList<Signal> getListOfSignals() {
		if(signallist==null)
			return null;
		return signallist.signals;
	}

	/** Load sensor data for all sensors in the network.
	 */
	public void loadSensorData() throws SiriusException{
		if(getSensorList()==null)
			return;
		for(com.relteq.sirius.jaxb.Sensor sensor : getSensorList().getSensor()){
			((Sensor)sensor).loadData();
		}
	}
	
}
