package com.relteq.sirius.control;

import java.util.Vector;

import com.relteq.sirius.simulator.SiriusMath;
import com.relteq.sirius.simulator.Controller;
import com.relteq.sirius.simulator.Scenario;
import com.relteq.sirius.simulator.ScenarioElement;

public class Controller_SIG_Pretimed extends Controller {

	// input parameters
	private int [] plansequence;		  // Ordered list of plans to implement 
	private float [] planstarttime;		  // [sec] Implementation times (first should be 0, should be increasing)
	private float transdelay;					   // transition time between plans.
	private int numplans;						   // total number of defined plans
	private Controller_SIG_Pretimed_Plan [] plan;  // array of plans
	
	// state
	//private int cplan;							  // current plan id
	private int cperiod;						  // current index to planstarttime and plansequence
	
	// coordination
	//private ControllerCoordinated coordcont;
	private boolean coordmode = false;					  // true if this is used for coordination (softforceoff only)

	
	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////

	public Controller_SIG_Pretimed() {
		// TODO Auto-generated constructor stub
	}
	
	public Controller_SIG_Pretimed(Scenario myScenario) {
		// TODO Auto-generated constructor stub
	}

	/////////////////////////////////////////////////////////////////////
	// InterfaceController
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public void populate(Object jaxbobject) {

		com.relteq.sirius.jaxb.Controller jaxbc = (com.relteq.sirius.jaxb.Controller) jaxbobject;

		// must have these
		if(jaxbc.getTargetElements()==null)
			return;
		if(jaxbc.getTargetElements().getScenarioElement()==null)
			return;
		if(jaxbc.getPlanList()==null)
			return;
		if(jaxbc.getPlanList().getPlan()==null)
			return;
		if(jaxbc.getPlanList().getPlan().isEmpty())
			return;

		// plan list
		numplans = jaxbc.getPlanList().getPlan().size();
		Vector<String> planId2Index = new Vector<String>();
		plan = new Controller_SIG_Pretimed_Plan[numplans];
		for(int i=0;i<numplans;i++){
			plan[i] = new Controller_SIG_Pretimed_Plan();
			plan[i].populate(this,myScenario,jaxbc.getPlanList().getPlan().get(i));
			planId2Index.add(plan[i].getId());
		}
		
		// plan sequence
		if(jaxbc.getPlanSequence()==null){	// if no plan sequence, assume 0,0
			transdelay = 0;
			plansequence = new int[1];
			plansequence[0] = 0;
			planstarttime = new float[1];
			planstarttime[0] = 0f;
		}
		else{
			if(jaxbc.getPlanSequence().getTransitionDelay()!=null)
				transdelay = jaxbc.getPlanSequence().getTransitionDelay().floatValue();
			else
				transdelay = 0f;
			
			if(jaxbc.getPlanSequence().getPlanReference()!=null){
				
				int numPlanReference = jaxbc.getPlanSequence().getPlanReference().size();

				plansequence = new int[numPlanReference];
				planstarttime = new float[numPlanReference];
				
				for(int i=0;i<numPlanReference;i++){
					com.relteq.sirius.jaxb.PlanReference ref = jaxbc.getPlanSequence().getPlanReference().get(i);
					plansequence[i] = planId2Index.indexOf(ref.getPlanId());
					planstarttime[i] = ref.getStartTime().floatValue();
				}
			}
			
		}

	}

	@Override
	public void update() {

		double simtime = myScenario.getTimeInSeconds();

		// time to switch plans .....................................
		if( cperiod < planstarttime.length-1 ){
			if( SiriusMath.greaterorequalthan( simtime , planstarttime[cperiod+1] + transdelay ) ){
				cperiod++;
				if(plansequence[cperiod]==0){
					// GCG asc.ResetSignals();  GG FIX THIS
				}
//				if(coordmode)
//					coordcont.SetSyncPoints();
					
			}
		}

//		if( plansequence[cperiod]==0 )
//			ImplementASC();
//		else
			plan[plansequence[cperiod]].implementPlan(simtime,coordmode);		
		
	}
	
	@Override
	public boolean validate() {
		if(!super.validate())
			return false;
		
		int i;
		
		// transdelay>=0
		if(transdelay<0)
			return false;
		
		// first planstarttime=0
		if(planstarttime[0]!=0)
			return false;
		
		// planstarttime is increasing
		for(i=1;i<planstarttime.length;i++)
			if(planstarttime[i]<=planstarttime[i-1])
				return false;
		
		// all plansequence ids found
		for(i=0;i<plansequence.length;i++)
			if(plansequence[i]<0)
				return false;

		// all targets are signals
		for(ScenarioElement se: targets){
			if(se.getMyType().compareTo(ScenarioElement.Type.signal)!=0){
				return false;
			}
		}
		
		for(i=0;i<plan.length;i++)
			if(!plan[i].validate())
				return false;
		
		return true;
	}

	@Override
	public void reset() {
		super.reset();
		cperiod = 0;

		for(int i=0;i<plan.length;i++)
			plan[i].reset();
	}

	@Override
	public boolean register() {
		return true; // signal controllers don't have to register, because the signal does this for them.
	}

	@Override
	public boolean deregister() {
		// TODO Auto-generated method stub
		return false;
	}
	
}
