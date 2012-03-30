/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

import com.relteq.sirius.jaxb.Link;
import com.relteq.sirius.jaxb.Node;

final class _Network extends com.relteq.sirius.jaxb.Network {

	protected _Scenario myScenario;
	protected _SensorList _sensorlist = new _SensorList();
	protected _SignalList _signallist = new _SignalList();
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(_Scenario myScenario) {
		
		this.myScenario = myScenario;
		
		if(getNodeList()!=null)
			for (Node node : getNodeList().getNode())
				((_Node) node).populate(this);
		
		if(getLinkList()!=null)
			for (Link link : getLinkList().getLink())
				((_Link) link).populate(this);
		
		_sensorlist.populate(this);
		_signallist.populate(myScenario,this);
		
	}

	protected boolean validate() {

		if(myScenario.getSimDtInSeconds()<=0){
			SiriusErrorLog.addErrorMessage("Negative simulation time (" + myScenario.getSimDtInSeconds() +").");
			return false;
		}
		
		// node list
		if(getNodeList()!=null)
			for (Node node : getNodeList().getNode())
				if( !((_Node)node).validate() ){
					SiriusErrorLog.addErrorMessage("Node validation failure, node " + node.getId());
					return false;
				}

		// link list
		if(getLinkList()!=null)
			for (Link link : getLinkList().getLink())
				if( !((_Link)link).validate() ){
					SiriusErrorLog.addErrorMessage("Link validation failure, link " + link.getId());
					return false;
				}

		// sensor list
		if(!_sensorlist.validate())
			return false;


		// signal list
		if(!_signallist.validate())
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

	protected void reset() throws SiriusException {
		
//		// node list
//		if(getNodeList()!=null)
//			for (Node node : getNodeList().getNode())
//				((_Node)node).reset();

		// link list
		if(getLinkList()!=null)
			for (Link link : getLinkList().getLink()){
				_Link _link = (_Link) link;
				_link.resetLanes();				
				_link.resetState();
				_link.resetFD();
			}

		// sensor list
		_sensorlist.reset();

		
		// signal list
		_signallist.reset();
		
//		if(getSignalList()!=null)
//			for (Signal signal : getSignalList().getSignal())
//				((_Signal)signal).reset();
				
	}

	protected void update() throws SiriusException {
		
        // compute link demand and supply ...............
        for(Link link : getLinkList().getLink()){
        	((_Link)link).updateOutflowDemand();
        	((_Link)link).updateSpaceSupply();
        }
        
        // update sensor readings .......................
        _sensorlist.update();
        
        // update signals ...............................
        _signallist.update();
//        if(getSignalList()!=null)
//	        for(Signal signal : getSignalList().getSignal())
//	        	((_Signal)signal).update();
        
        // update nodes: compute flows on links .........
        for(Node node : getNodeList().getNode())
            ((_Node)node).update();
        
        // update links: compute densities .............
        for(Link link : getLinkList().getLink())
        	((_Link)link).update();
	}

	/////////////////////////////////////////////////////////////////////
	// public API
	/////////////////////////////////////////////////////////////////////
	
	public ArrayList<_Sensor> getSensorWithLinkId(String linkid){
		ArrayList<_Sensor> result = new ArrayList<_Sensor>();
		for(_Sensor sensor : _sensorlist._sensors){
			if(sensor.myLink!=null){
				if(sensor.myLink.getId().equals(linkid)){
					result.add(sensor);
					break;
				}	
			}
		}
		return result;
	}
	
	public _Sensor getFirstSensorWithLinkId(String linkid){
		for(_Sensor sensor : _sensorlist._sensors){
			if(sensor.myLink!=null){
				if(sensor.myLink.getId().equals(linkid)){
					return sensor;
				}
			}
		}
		return null;
	}

	public _Sensor getSensorWithId(String id){
		id.replaceAll("\\s","");
		for(_Sensor sensor : _sensorlist._sensors){
			if(sensor.id.equals(id))
				return sensor;
		}
		return null;
	}
	
	public _Signal getSignalWithId(String id){
		id.replaceAll("\\s","");
		for(_Signal signal : _signallist._signals){
			if(signal.getId().equals(id))
				return signal;
		}
		return null;
	}
	
	public _Signal getSignalWithNodeId(String node_id){
		id.replaceAll("\\s","");
		for(_Signal signal : _signallist._signals){
			if(signal.getNodeId().equals(node_id))
				return signal;
		}
		return null;
	}
	
	public _Link getLinkWithId(String id){
		id.replaceAll("\\s","");
		for(Link link : getLinkList().getLink()){
			if(link.getId().equals(id))
				return (_Link) link;
		}
		return null;
	}

	public _Node getNodeWithId(String id){
		id.replaceAll("\\s","");
		for(Node node : getNodeList().getNode()){
			if(node.getId().equals(id))
				return (_Node) node;
		}
		return null;
	}
	
}
