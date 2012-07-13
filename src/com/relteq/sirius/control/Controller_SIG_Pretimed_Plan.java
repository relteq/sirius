package com.relteq.sirius.control;

import java.util.ArrayList;

import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusMath;
import com.relteq.sirius.simulator.Scenario;
import com.relteq.sirius.simulator.ScenarioElement;
import com.relteq.sirius.simulator.Signal;

public class Controller_SIG_Pretimed_Plan extends com.relteq.sirius.jaxb.Plan {
	
	protected Controller_SIG_Pretimed myController;
	
	// input parameters
	protected Controller_SIG_Pretimed_IntersectionPlan [] intersplan;
	protected boolean [] havesignaltarget;	// true if this intersection is in the target list
	protected double _cyclelength;	

	ArrayList<Signal.Command> commandlist = new ArrayList<Signal.Command>();

	public void populate(Controller_SIG_Pretimed myController,Scenario myScenario,com.relteq.sirius.jaxb.Plan jaxbplan) {
		
		this.myController = myController;
		
		this.setId(jaxbplan.getId());
		
		if(jaxbplan.getCyclelength()!=null)
			_cyclelength = jaxbplan.getCyclelength().doubleValue();
					
		if(jaxbplan.getIntersection()!=null){
			int numintersection = jaxbplan.getIntersection().size();
			havesignaltarget = new boolean[numintersection];
			intersplan = new Controller_SIG_Pretimed_IntersectionPlan[numintersection];
			for(int i=0;i<intersplan.length;i++){

				// check whether the signal is in the target list
				com.relteq.sirius.jaxb.Intersection jaxbi = jaxbplan.getIntersection().get(i);				
				Signal mySignal = myScenario.getSignalWithCompositeNodeId(null,jaxbi.getNodeId());
				if(mySignal==null)
					continue;
				boolean haveit = false;
				for(ScenarioElement se : myController.getTargets()){
					if( se.getMyType().compareTo(ScenarioElement.Type.signal)==0 &&
						//se.getNetworkId().compareTo(jaxbi.getNetworkId())==0 && 
						se.getId().compareTo(mySignal.getId())==0 ){
						haveit=true;
					}
				}
				if(!haveit)
					continue;				
				intersplan[i] = new Controller_SIG_Pretimed_IntersectionPlan(this);
				intersplan[i].populate(myScenario,jaxbi);
			}
		}
		
	}
	
	public void validate(){
		
		if(myController==null)
			SiriusErrorLog.addError("Invalid controller for pretimed signal plan id=" + getId() + ".");
		
		// positive cycle
		if(_cyclelength<=0)
			SiriusErrorLog.addError("Non-positive cycle length in pretimed signal controller id=" + getId() + ".");
		
		// cycle length should be a multiple of controller dt
		if(myController!=null)
			if(!SiriusMath.isintegermultipleof(_cyclelength,myController.getDtinseconds()))
				SiriusErrorLog.addError("Cycle length is not an integer multiple of controller rate in pretimed signal controller id=" + getId()+ ".");
		
		// plan includes all targets
		boolean foundit;
		if(myController!=null)
			for(ScenarioElement se : myController.getTargets()){
				foundit = false;
				for(int i=0;i<intersplan.length;i++){
					if(se.getId().equals(intersplan[i].mySignal.getId())){
						foundit=true;
						break;
					}
				}
				if(!foundit)
					SiriusErrorLog.addError("Controller target (id="+se.getId()+") not found in pretimed signal plan id="+getId());
			}
		
		// intersection plans
		for(int i=0;i<intersplan.length;i++)
			intersplan[i].validate(myController.getDtinseconds());
		
	}

	public void reset() {
		for(int i=0;i<intersplan.length;i++)
			intersplan[i].reset();		
	}
	
	protected void implementPlan(double simtime,boolean coordmode){

		int i;
		double itime;

		// Master clock .............................
		itime =  simtime % _cyclelength;
		
		// Loop through intersections ...............
		for(i=0;i<intersplan.length;i++){

			commandlist.clear();
			
			// get commands for this intersection
			intersplan[i].getCommandForTime(itime,commandlist);
			
			// send command to the signal
			intersplan[i].mySignal.requestCommand(commandlist);

//			if( !coordmode ){
//				for(j=0;j<intplan.holdpoint.length;j++)
//					if( reltime==intplan.holdpoint[j] )
//						intplan.mySignal.IssueHold(j);
//
//				for(j=0;j<intplan.holdpoint.length;j++)
//					if( reltime==intplan.forceoffpoint[j] )
//						intplan.mySignal.IssueForceOff(j,intplan.mySignal.phase[j].actualyellowtime,intplan.mySignal.phase[j].actualredcleartime);
//			}

			
			// Used for coordinated actuated.
//			if( coordmode ){

//			for( j=0;j<8;j++ ){
//
//					
//					if( !intplan.mySignal.Phase(j).Protected() )
//						continue;
//
//					issyncphase = j==intplan.movA[0] || j==intplan.movB[0];
//
//					// Non-persisting forceoff request at forceoffpoint
//					if( reltime==intplan.forceoffpoint[j] )
//						c.setRequestforceoff(i, j, true);
//
//					// Hold request for sync phase if
//					// currently both sync phases are active
//					// and not reached syncpoint
//					if( issyncphase && 
//						c.PhaseA(i)!=null && c.PhaseA(i).MyNEMA()==intplan.movA.get(0) && 
//						c.PhaseB(i)!=null && c.PhaseB(i).MyNEMA() == intplan.movB.get(0) &&
//						reltime!= c.Syncpoint(i) )
//						c.setRequesthold(i, j, true);
//				}
//			}
		}
	}

	
	
}
