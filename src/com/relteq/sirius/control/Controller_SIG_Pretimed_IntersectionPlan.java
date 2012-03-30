package com.relteq.sirius.control;

import java.util.ArrayList;
import java.util.Collections;

import com.relteq.sirius.jaxb.Stage;
import com.relteq.sirius.simulator.SignalPhase;
import com.relteq.sirius.simulator.SiriusMath;
import com.relteq.sirius.simulator._Scenario;
import com.relteq.sirius.simulator._Signal;
import com.relteq.sirius.simulator._Signal.Command;
import com.relteq.sirius.simulator._Signal.NEMA;

public class Controller_SIG_Pretimed_IntersectionPlan {

	
	// references 
	//private _Node myNode;
	private Controller_SIG_Pretimed_Plan myPlan;
	protected _Signal mySignal;
	
	//SignalManager mySigMan;
	//int myIntersectionID;
	
	// input parameters
	private float offset;	// offset for the intersection
	
	// stage information
	//private float cyclelength;
	
	// list of holds and force-off points 
	protected ArrayList<_Signal.Command> command = new ArrayList<_Signal.Command>();
	int nextcommand;
	float lastcommandtime;
		
	private int numstages;
	private Float [] greentime;
	private Float[] stagelength;
	private _Signal.NEMA [] movA;
	private _Signal.NEMA [] movB;
	
 	public Controller_SIG_Pretimed_IntersectionPlan(Controller_SIG_Pretimed_Plan myPlan){
		this.myPlan = myPlan;
	}
	
	@SuppressWarnings("unchecked")
	public void populate(_Scenario myScenario,com.relteq.sirius.jaxb.Intersection jaxbi) {
								
		if(jaxbi.getOffset()!=null)
			this.offset = jaxbi.getOffset().floatValue();
		else
			this.offset = 0f;

		numstages = jaxbi.getStage().size(); 	// number of stages (length of movA, movB and greentime)
		
		if(numstages<=1)
			return;		
		
		greentime = new Float[numstages];
		movA = new _Signal.NEMA[numstages];
		movB = new _Signal.NEMA[numstages];
	
		for(int i=0;i<numstages;i++){
			Stage stage = jaxbi.getStage().get(i);
			if(stage.getGreentime()!=null)
				greentime[i] = stage.getGreentime().floatValue();
			else
				greentime[i] = null;

			movA[i] = _Signal.String2NEMA(stage.getMovA());
			movB[i] = _Signal.String2NEMA(stage.getMovB());
		}
		
		mySignal = myScenario.getSignalForNodeId(jaxbi.getNetworkId(),jaxbi.getNodeId());

		// Set yellowtimes, redcleartimes, stagelength, totphaselength
		int k;
		SignalPhase pA;
		SignalPhase pB;
		float y,r,yA,yB,rA,rB;
		float totphaselength = 0;
		stagelength = new Float[numstages];
		for(k=0;k<numstages;k++){
	
			pA = mySignal.getPhaseByNEMA(movA[k]);
			pB = mySignal.getPhaseByNEMA(movB[k]);
			
			if(pA==null && pB==null)
				return;
			
			yA = pA==null ? 0f : pA.getYellowtime();
			rA = pA==null ? 0f : pA.getRedcleartime();
			yB = pB==null ? 0f : pB.getYellowtime();
			rB = pB==null ? 0f : pB.getRedcleartime();
			
			y = Math.max(yA,yB);
			r = Math.max(rA,rB);

			if( InNextStage(pA,k) ){
				y = yB;
				r = rB;
			}

			if( InNextStage(pB,k) ){
				y = yA;
				r = rA;
			}
			
			if(pA!=null){
				pA.setActualyellowtime(y);
				pA.setActualredcleartime(r);
			}
				
			if(pB!=null){
				pB.setActualyellowtime(y);
				pB.setActualredcleartime(r);
			}

			stagelength[k] = greentime[k]+y+r;
			totphaselength += greentime[k]+y+r;
		}
		
		// check cycles are long enough .....................................	
		if(!SiriusMath.equals(myPlan._cyclelength,totphaselength))
			return;
		
		// compute hold and forceoff points ............................................
		float stime, etime;
		int nextstage;
		stime=0;
		for(k=0;k<numstages;k++){

			etime = stime + greentime[k];
			stime = stime + stagelength[k];
			
			if(k==numstages-1)
				nextstage = 0;
			else
				nextstage = k+1;
			
			if(stime>=totphaselength)
				stime = 0;
			
			// do something if the phase changes from this stage to the next
			if(movA[k].compareTo(movA[nextstage])!=0){ 
				
				// force off this stage
				if(movA[k].compareTo(NEMA.NULL)!=0){
					pA = mySignal.getPhaseByNEMA(movA[k]);					
					command.add(new Command(_Signal.CommandType.forceoff,movA[k],etime,pA.getActualyellowtime(),pA.getActualredcleartime()));
				}
				
				// hold next stage
				if(movA[nextstage].compareTo(NEMA.NULL)!=0)
					command.add(new Command(_Signal.CommandType.hold,movA[nextstage],stime));
				
			}
			
			// same for ring B
			if(movB[k].compareTo(movB[nextstage])!=0){ 
				if(movB[k].compareTo(NEMA.NULL)!=0){
					pB = mySignal.getPhaseByNEMA(movB[k]);		
					command.add(new Command(_Signal.CommandType.forceoff,movB[k],etime,pB.getActualyellowtime(),pB.getActualredcleartime()));
				}
				if(movB[nextstage].compareTo(NEMA.NULL)!=0)
					command.add(new Command(_Signal.CommandType.hold,movB[nextstage],stime));
			}
			
		}
		
		// Correction: offset is with respect to end of first stage, instead of beginning
		for(_Signal.Command c : command){
			c.time -= greentime[0];
			if(c.time<0)
				c.time += myPlan._cyclelength;
		}
		
		// sort the commands
		Collections.sort(command);
		
		lastcommandtime = command.get(command.size()-1).time;

	}
	
	public boolean validate(double controldt){
		
		// at least two stages
		if(numstages<=1)
			return false;
		
		// check offset
		if(offset<0 || offset>=myPlan._cyclelength)
			return false;
		
		//  greentime, movA, movB
		for(int k=0;k<numstages;k++){
			if(greentime[k]==null || greentime[k]<=0)
				return false;
			if(movA[k]==null && movB[k]==null)
				return false;
		}
		
		// values are integer multiples of controller dt
		for(int k=0;k<numstages;k++){
			if(!SiriusMath.isintegermultipleof((double) greentime[k],controldt))
				return false;
			if(stagelength[k]!=greentime[k])
				if(!SiriusMath.isintegermultipleof((double) stagelength[k]-greentime[k],controldt))
					return false;
		}
		
		// first two commands have zero timestamp
		if(command.get(0).time!=0.0)
			return false;
		
		return true;
	}
		
	public void reset(){
		nextcommand = 0;
	}
	public boolean InNextStage(SignalPhase thisphase,int stageindex)
	{
		int nextstage;
		
		if(stageindex<0 || stageindex>=numstages || thisphase==null)
			return false;

		nextstage = stageindex+1;

		if(nextstage==numstages)
			nextstage=0;

		return thisphase.getMyNEMA().compareTo(movA[nextstage])==0 || thisphase.getMyNEMA().compareTo(movB[nextstage])==0;
		
	}

//	public float getOffset() {
//		return offset;
//	}
	
	protected void getCommandForTime(double itime,ArrayList<_Signal.Command> commandlist){
		
		double reltime = itime - offset;		
		if(reltime<0)
			reltime += myPlan._cyclelength;
		
		if(reltime>lastcommandtime)
			return;
		
		float nexttime = command.get(nextcommand).time;
		
		if(nexttime<=reltime){
			while(nexttime<=reltime){
				commandlist.add(command.get(nextcommand));
				nextcommand += 1;
				if(nextcommand==command.size()){
					nextcommand = 0;
					break;
				}
				nexttime = command.get(nextcommand).time;
			}
		}

	}
	
	
}
