package com.relteq.sirius.event;

import java.util.ArrayList;

import com.relteq.sirius.simulator.ObjectFactory;
import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusException;
import com.relteq.sirius.simulator.Event;
import com.relteq.sirius.simulator.Node;
import com.relteq.sirius.simulator.Scenario;
import com.relteq.sirius.simulator.ScenarioElement;

public class Event_Node_Split_Ratio extends Event {

	protected boolean resetToNominal;			// if true, go back to nominal before applying changes
	protected Node myNode;
	protected java.util.List<SplitRatio> splitratios;
	
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
		splitratios = new ArrayList<SplitRatio>(splits.size());
		int input_index = node.getInputLinkIndex(inlink);
		int vt_index = myScenario.getVehicleTypeIndex(vehicletype);
		int output_index = 0;
		for (Double split : splits)
			splitratios.add(new SplitRatio(input_index, output_index++, vt_index, split));
		this.targets = new ArrayList<ScenarioElement>();
		this.targets.add(ObjectFactory.createScenarioElement(node));		
	}

	// constructor for reset event with single node target
	public Event_Node_Split_Ratio(Scenario myScenario,Node node) {
		this.resetToNominal = true;
		this.splitratios = null;
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
		com.relteq.sirius.simulator.Parameters params = (com.relteq.sirius.simulator.Parameters) jaxbe.getParameters();

		// reset_to_nominal
		boolean reset_to_nominal = false;
		if (null != params && params.has("reset_to_nominal"))
			reset_to_nominal = params.get("reset_to_nominal").equalsIgnoreCase("true");

		if(!reset_to_nominal && jaxbe.getSplitratioEvent()==null)
			return;

		// only accepts single target
		if(targets.size()!=1)
			return;

		this.resetToNominal = reset_to_nominal;
		this.myNode = (Node) targets.get(0).getReference();
		
		if(myNode==null)
			return;
		
		if(resetToNominal)		// nothing else to populate in this case
			return;
		
		com.relteq.sirius.jaxb.SplitratioEvent srevent = jaxbe.getSplitratioEvent();
		if (srevent != null) {
			int[] vt_index = null;
			if (null == srevent.getVehicleTypeOrder()) {
				vt_index = new int[myScenario.getNumVehicleTypes()];
				for (int i = 0; i < vt_index.length; ++i)
					vt_index[i] = i;
			} else {
				vt_index = new int[srevent.getVehicleTypeOrder().getVehicleType().size()];
				int i = 0;
				for (com.relteq.sirius.jaxb.VehicleType vt : srevent.getVehicleTypeOrder().getVehicleType())
					vt_index[i++] = myScenario.getVehicleTypeIndex(vt.getName());
			}
			splitratios = new ArrayList<SplitRatio>(vt_index.length * srevent.getSplitratio().size());
			for (com.relteq.sirius.jaxb.Splitratio sr : srevent.getSplitratio()) {
				com.relteq.sirius.simulator.Double1DVector vector = new com.relteq.sirius.simulator.Double1DVector(sr.getContent(), ":");
				if (!vector.isEmpty()) {
					int input_index = myNode.getInputLinkIndex(sr.getLinkIn());
					int output_index = myNode.getOutputLinkIndex(sr.getLinkOut());
					for (int i = 0; i < vector.getLength().intValue(); ++i)
						splitratios.add(new SplitRatio(input_index, output_index, vt_index[i], vector.get(i)));
				}
			}
		}

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
			for (SplitRatio sr : splitratios) {
				if (sr.getInputIndex() < 0 || sr.getInputIndex() >= myNode.getnIn())
					SiriusErrorLog.addWarning("Invalid input link index for event id="+getId()+".");
				if (sr.getOutputIndex() < 0 || sr.getOutputIndex() >= myNode.getnOut())
					SiriusErrorLog.addWarning("Invalid output link index for event id="+getId()+".");
				if (sr.getVehicleTypeIndex() < 0 || sr.getVehicleTypeIndex() >= myScenario.getNumVehicleTypes())
					SiriusErrorLog.addWarning("Invalid vehicle type index for event id="+getId()+".");
			}
		}
	}
	
	@Override
	public void activate() throws SiriusException{
		if(myNode==null)
			return;
		if(resetToNominal)
			revertNodeEventSplitRatio(myNode);
		else
			setNodeEventSplitRatio(myNode, splitratios);
	}

}