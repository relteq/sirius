package com.relteq.sirius.event;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.relteq.sirius.simulator.ObjectFactory;
import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusException;
import com.relteq.sirius.simulator.Event;
import com.relteq.sirius.simulator.Node;
import com.relteq.sirius.simulator.Scenario;
import com.relteq.sirius.simulator.ScenarioElement;

public class Event_Node_Split_Ratio extends Event {

	protected boolean resetToNominal;			// if true, go back to nominal before applying changes
	protected ArrayList<Double> splitrow;		// if not null, use this one, regardless of resetToNominal
	protected int inputindex;
	protected int vehicletypeindex; 			// index of vehicle type into global list
	protected Node myNode;
	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public Event_Node_Split_Ratio(){
	}
	
	// constructor for change event with single node target, single input, single vehicle type
	public Event_Node_Split_Ratio(Scenario myScenario,Node node,String inlink,String vehicletype,ArrayList<Double>splits) {
		if(node==null)
			return;
		if(myScenario==null)
			return;
		this.resetToNominal = false;
		this.inputindex = node.getInputLinkIndex(inlink);
		this.vehicletypeindex = myScenario.getVehicleTypeIndex(vehicletype);
		this.splitrow = splits;
		this.targets = new ArrayList<ScenarioElement>();
		this.targets.add(ObjectFactory.createScenarioElement(node));		
	}

	// constructor for reset event with single node target
	public Event_Node_Split_Ratio(Scenario myScenario,Node node) {
		this.resetToNominal = true;
		this.splitrow = null;
		this.myType = Event.Type.node_split_ratio;
		this.targets = new ArrayList<ScenarioElement>();
		this.targets.add(ObjectFactory.createScenarioElement(node));	
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceEvent
	/////////////////////////////////////////////////////////////////////

	@Override
	public void populate(Object jaxbobject) {

		com.relteq.sirius.jaxb.Event jaxbe = (com.relteq.sirius.jaxb.Event) jaxbobject;
		
		if(!jaxbe.isResetToNominal() && jaxbe.getSplitratioEvent()==null)
			return;

		// only accepts single target
		if(targets.size()!=1)
			return;

		this.resetToNominal = jaxbe.isResetToNominal();
		this.myNode = (Node) targets.get(0).getReference();
		
		if(myNode==null)
			return;
		
		if(resetToNominal)		// nothing else to populate in this case
			return;
		
		com.relteq.sirius.jaxb.SplitratioEvent sre = jaxbe.getSplitratioEvent();
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
		if(targets.get(0).getMyType().compareTo(ScenarioElement.Type.node)!=0){
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