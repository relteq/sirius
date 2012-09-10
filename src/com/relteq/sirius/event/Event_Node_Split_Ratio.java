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
	public void validate() {
		
		super.validate();
		
		if(targets.size()!=1)
			SiriusErrorLog.addError("Multiple targets assigned to split ratio event id="+this.getId()+".");
		
		// check each target is valid
		if(targets.get(0).getMyType().compareTo(ScenarioElement.Type.node)!=0)
			SiriusErrorLog.addError("Wrong target type for event id="+getId()+".");
		
		if(myNode==null)
			SiriusErrorLog.addWarning("Invalid node id for event id="+getId()+".");
		
		// check split ratio matrix
		if(!resetToNominal){
			if(splitrow==null)
				SiriusErrorLog.addWarning("No split ratio rows for event id="+getId()+".");
			if(myNode!=null){
				if(splitrow.size()!=myNode.getnOut())
					SiriusErrorLog.addWarning("Number of rows does not match number of outgoing links for event id="+getId()+".");
				if(inputindex<0 || inputindex>=myNode.getnIn())
					SiriusErrorLog.addWarning("Invalid input link index for event id="+getId()+".");
			}
			if(vehicletypeindex<0 || vehicletypeindex>=myScenario.getNumVehicleTypes())
				SiriusErrorLog.addWarning("Invalid vehicle type index for event id="+getId()+".");
		}
	}
	
	@Override
	public void activate() throws SiriusException{
		if(myNode==null)
			return;
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
			try {
				Double value = Double.parseDouble(slicesX.nextToken());
				if(value>=0)
					data.add(value);
				else
					data.add(Double.NaN);
			} catch (NumberFormatException e) {
				data.add(Double.NaN);
			}
		}
		return data;
    }
}