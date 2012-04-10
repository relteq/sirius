/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

final class Network extends com.relteq.sirius.jaxb.Network {

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
		
//		// node list
//		if(getNodeList()!=null)
//			for (Node node : getNodeList().getNode())
//				((_Node)node).reset();

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

	public Sensor getSensorWithId(String id){
		id.replaceAll("\\s","");
		for(Sensor sensor : sensorlist.sensors){
			if(sensor.id.equals(id))
				return sensor;
		}
		return null;
	}
	
	public Signal getSignalWithId(String id){
		id.replaceAll("\\s","");
		for(Signal signal : signallist.signals){
			if(signal.getId().equals(id))
				return signal;
		}
		return null;
	}
	
	public Signal getSignalWithNodeId(String node_id){
		id.replaceAll("\\s","");
		for(Signal signal : signallist.signals){
			if(signal.getNodeId().equals(node_id))
				return signal;
		}
		return null;
	}
	
	public Link getLinkWithId(String id){
		id.replaceAll("\\s","");
		for(com.relteq.sirius.jaxb.Link link : getLinkList().getLink()){
			if(link.getId().equals(id))
				return (Link) link;
		}
		return null;
	}

	public Node getNodeWithId(String id){
		id.replaceAll("\\s","");
		for(com.relteq.sirius.jaxb.Node node : getNodeList().getNode()){
			if(node.getId().equals(id))
				return (Node) node;
		}
		return null;
	}
	
}
