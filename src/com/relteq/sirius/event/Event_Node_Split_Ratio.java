package com.relteq.sirius.event;


import java.util.ArrayList;

import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.jaxb.Splitratio;
import com.relteq.sirius.jaxb.SplitratioEvent;
import com.relteq.sirius.simulator.Double1DVector;
import com.relteq.sirius.simulator.Double3DMatrix;
import com.relteq.sirius.simulator.ObjectFactory;
import com.relteq.sirius.simulator._Event;
import com.relteq.sirius.simulator._Node;
import com.relteq.sirius.simulator._Scenario;
import com.relteq.sirius.simulator._ScenarioElement;

/** DESCRIPTION OF THE CLASS
* @author AUTHOR NAME
* @version VERSION NUMBER
*/
public class Event_Node_Split_Ratio extends _Event {

	protected boolean resetToNominal;
	protected Double3DMatrix splitratio;
	protected Integer [] vehicletypeindex; 	// index of vehicle types into global list
	protected _Node myNode;
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Node_Split_Ratio(){
	}
	
	// constructor for event with single node target
	public Event_Node_Split_Ratio(_Scenario myScenario,_Node node,Double3DMatrix splitratio) {
		this.resetToNominal = false;
		this.splitratio = splitratio;
		this.myType = _Event.Type.node_split_ratio;
		this.targets = new ArrayList<_ScenarioElement>();
		this.targets.add(ObjectFactory.createScenarioElement(node));		
	}

	// constructor for event with single node target
	public Event_Node_Split_Ratio(_Scenario myScenario,_Node node) {
		this.resetToNominal = true;
		this.myType = _Event.Type.node_split_ratio;
		this.targets = new ArrayList<_ScenarioElement>();
		this.targets.add(ObjectFactory.createScenarioElement(node));	
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceEvent
	/////////////////////////////////////////////////////////////////////

	@Override
	public void populate(Object jaxbobject) {

		Event jaxbe = (Event) jaxbobject;
		
		if(!jaxbe.isResetToNominal() && jaxbe.getSplitratioEvent()==null)
			return;

		// only accepts single target
		if(targets.size()!=1)
			return;

		this.resetToNominal = jaxbe.isResetToNominal();
		this.myNode = (_Node) targets.get(0).getReference();
		
		if(myNode==null)
			return;
		
		if(resetToNominal)		// nothing else to populate in this case
			return;
		
		SplitratioEvent sre = jaxbe.getSplitratioEvent();
		if(sre!=null)
			vehicletypeindex = myScenario.getVehicleTypeIndices(sre.getVehicleTypeOrder());
		else
			vehicletypeindex = myScenario.getVehicleTypeIndices(null);
				
		splitratio = new Double3DMatrix(myNode.getnIn(),myNode.getnOut(),myScenario.getNumVehicleTypes(),Double.NaN);
		
		int in_index,out_index,k;
		for(Splitratio sr :	sre.getSplitratio()){			
			in_index = myNode.getInputLinkIndex(sr.getLinkIn());
			out_index = myNode.getOutputLinkIndex(sr.getLinkOut());
			if(in_index<0 || out_index<0)
				continue; 
			Double1DVector values = new Double1DVector(sr.getContent(),":");
			for(k=0;k<vehicletypeindex.length;k++)
				splitratio.set(in_index,out_index,vehicletypeindex[k],values.get(k));
		}
		

	}
	
	@Override
	public boolean validate() {
		if(!super.validate())
			return false;
		
		if(targets.size()!=1){
			System.out.println("This event does not work for multiple targets.");
			return false;
		}
		
		// check each target is valid
		if(targets.get(0).getMyType()!=_ScenarioElement.Type.node){
			System.out.println("wrong target type.");
			return false;
		}
		
		if(myNode==null){
			System.out.println("wrong node id.");
			return false;
		}
		
		// check split ratio matrix
		
		
		return true;
	}

	@Override
	public void activate() {
		if(resetToNominal)
			revertNodeEventSplitRatio(myNode);
		else
			setNodeEventSplitRatio(myNode,splitratio);	
	}

}
