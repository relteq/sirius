package com.relteq.sirius.event;


import com.relteq.sirius.jaxb.Event;
import com.relteq.sirius.jaxb.Splitratio;
import com.relteq.sirius.jaxb.SplitratioEvent;
import com.relteq.sirius.simulator.Double1DVector;
import com.relteq.sirius.simulator.Double3DMatrix;
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
	public Event_Node_Split_Ratio(_Scenario myScenario,boolean isreset,_Node node,Double3DMatrix splitratio) {
		this.resetToNominal = isreset;
		this.splitratio = splitratio;
		this.myType = _Event.Type.node_split_ratio;
		this.myNode = node;
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceEvent
	/////////////////////////////////////////////////////////////////////

	@Override
	public void populate(Event e) {

		if(!e.isResetToNominal() && e.getSplitratioEvent()==null)
			return;

		// only accepts single target
		if(targets.size()!=1)
			return;

		this.resetToNominal = e.isResetToNominal();
		this.myNode = (_Node) targets.get(0).getReference();
		
		if(myNode==null)
			return;
		
		if(resetToNominal)		// nothing else to populate in this case
			return;
		
		// use <VehicleTypesOrder> if it is there, otherwise assume order given in <settings>
		int i,numTypes;
		SplitratioEvent sre = e.getSplitratioEvent();
		if(sre!=null && sre.getVehicleTypeOrder()!=null){
			numTypes = sre.getVehicleTypeOrder().getVehicleType().size();
			vehicletypeindex = new Integer[numTypes];
			for(i=0;i<numTypes;i++)
				vehicletypeindex[i] = myScenario.getVehicleTypeIndex(sre.getVehicleTypeOrder().getVehicleType().get(i).getName());
		}
		else{
			numTypes = myScenario.getNumVehicleTypes();
			vehicletypeindex = new Integer[numTypes];
			for(i=0;i<numTypes;i++)
				vehicletypeindex[i] = i;
		}
				
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
		
		// check each target is valid
		for(_ScenarioElement s : targets){
			if(s.getMyType()!=_ScenarioElement.Type.node){
				System.out.println("wrong target type.");
				return false;
			}
		}
		
		// check split ratio matrix
		
		
		return true;
	}

	@Override
	public void activate() {
		if(resetToNominal){
			for(_ScenarioElement s : targets){
				_Node targetnode = (_Node) s.getReference();
				revertNodeEventSplitRatio(targetnode);
			}
		}
		else{
			for(_ScenarioElement s : targets){
				_Node targetnode = (_Node) s.getReference();
				setNodeEventSplitRatio(targetnode,splitratio);
			}
		}		
	}

}
