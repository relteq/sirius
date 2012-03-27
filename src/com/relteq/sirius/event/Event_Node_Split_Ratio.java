package com.relteq.sirius.event;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.jaxb.SplitratioEvent;
import com.relteq.sirius.simulator.ObjectFactory;
import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusException;
import com.relteq.sirius.simulator._Event;
import com.relteq.sirius.simulator._Node;
import com.relteq.sirius.simulator._Scenario;
import com.relteq.sirius.simulator._ScenarioElement;

public class Event_Node_Split_Ratio extends _Event {

	protected boolean resetToNominal;			// if true, go back to nominal before applying changes
	protected ArrayList<Double> splitrow;		// if not null, use this one, regardless of resetToNominal
	protected int inputindex;
	protected int vehicletypeindex; 			// index of vehicle type into global list
	protected _Node myNode;
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Node_Split_Ratio(){
	}
	
	// constructor for change event with single node target, single input, single vehicle type
	public Event_Node_Split_Ratio(_Scenario myScenario,_Node node,String inlink,String vehicletype,ArrayList<Double>splits) {
		if(node==null)
			return;
		if(myScenario==null)
			return;
		this.resetToNominal = false;
		this.inputindex = node.getInputLinkIndex(inlink);
		this.vehicletypeindex = myScenario.getVehicleTypeIndex(vehicletype);
		this.splitrow = splits;
		this.targets = new ArrayList<_ScenarioElement>();
		this.targets.add(ObjectFactory.createScenarioElement(node));		
	}

	// constructor for reset event with single node target
	public Event_Node_Split_Ratio(_Scenario myScenario,_Node node) {
		this.resetToNominal = true;
		this.splitrow = null;
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
		if(sre==null)
			return;
		inputindex = myNode.getInputLinkIndex(sre.getLinkIn());
		vehicletypeindex = myScenario.getVehicleTypeIndex(sre.getVehicleTypeName());
		splitrow = readArray(sre.getContent(),",");
	}
	
	@Override
	public boolean validate() {
		if(!super.validate())
			return false;
		
		if(targets.size()!=1){
			SiriusErrorLog.addErrorMessage("This event does not work for multiple targets.");
			return false;
		}
		
		// check each target is valid
		if(targets.get(0).getMyType().compareTo(_ScenarioElement.Type.node)!=0){
			SiriusErrorLog.addErrorMessage("wrong target type.");
			return false;
		}
		
		if(myNode==null){
			SiriusErrorLog.addErrorMessage("wrong node id.");
			return false;
		}
		
		// check split ratio matrix
		if(!resetToNominal){
			if(splitrow==null)
				return false;
			if(splitrow.size()!=myNode.getnOut())
				return false;
			if(inputindex<0 || inputindex>=myNode.getnIn())
				return false;
			if(vehicletypeindex<0 || vehicletypeindex>=myScenario.getNumVehicleTypes())
				return false;
		}
		
		return true;
	}
	
	@Override
	public void activate() throws SiriusException{
		if(resetToNominal)
			revertNodeEventSplitRatio(myNode);
		else
			setNodeEventSplitRatio(myNode,inputindex,vehicletypeindex,splitrow);	
	}

	/////////////////////////////////////////////////////////////////////
	// private
	/////////////////////////////////////////////////////////////////////

    private ArrayList<Double> readArray(String str,String delim) {
      	if ((str.isEmpty()) || (str.equals("\n")) || (str.equals("\r\n"))){
			return null;
    	}
      	ArrayList<Double> data = new ArrayList<Double>();
    	str.replaceAll("\\s","");    	
		StringTokenizer slicesX = new StringTokenizer(str,delim);
		while (slicesX.hasMoreTokens()) {			
			Double value = Double.parseDouble(slicesX.nextToken());
			if(value>=0)
				data.add(value);
			else
				data.add(Double.NaN);
		}
		return data;
    }
}