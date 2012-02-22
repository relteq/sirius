/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

import com.relteq.sirius.jaxb.Link;
import com.relteq.sirius.jaxb.Node;
import com.relteq.sirius.jaxb.Signal;

final class _Network extends com.relteq.sirius.jaxb.Network {

	protected _Scenario myScenario;
	protected _SensorList _sensorlist = new _SensorList();
	
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
		
	}

	protected boolean validate() {

		if(myScenario.getSimDtInSeconds()<=0)
			return false;
		
		// node list
		if(getNodeList()!=null)
			for (Node node : getNodeList().getNode())
				if( !((_Node)node).validate() )
					return false;

		// link list
		if(getLinkList()!=null)
			for (Link link : getLinkList().getLink())
				if( !((_Link)link).validate() )
					return false;

		// sensor list
		if(!_sensorlist.validate())
			return false;

		// signal list
		if(getSignalList()!=null)
			for (Signal signal : getSignalList().getSignal())
				if( !((_Signal)signal).validate() )
					return false;

		return true;
	}

	protected void reset() {
		
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
		if(getSignalList()!=null)
			for (Signal signal : getSignalList().getSignal())
				((_Signal)signal).reset();
				
	}

	protected void update() {
		
        // compute link demand and supply ...............
        for(Link link : getLinkList().getLink()){
        	((_Link)link).updateOutflowDemand();
        	((_Link)link).updateSpaceSupply();
        }
        
        // update sensor readings .......................
        _sensorlist.update();
        
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
			if(sensor.getMyLink()!=null){
				if(sensor.getMyLink().getId().equals(linkid)){
					result.add(sensor);
					break;
				}	
			}
		}
		return result;
	}
	
	public _Sensor getFirstSensorWithLinkId(String linkid){
		for(_Sensor sensor : _sensorlist._sensors){
			if(sensor.getMyLink()!=null){
				if(sensor.getMyLink().getId().equals(linkid)){
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
