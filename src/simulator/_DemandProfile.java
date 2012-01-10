package simulator;

import jaxb.Demand;

public class _DemandProfile extends Demand implements AuroraComponent {

	private _Link myLink;
	private float dtinhours;
	private int samplesteps;
	private Float3DMatrix demand;			// [veh]
	private boolean isdone; 

	/////////////////////////////////////////////////////////////////////
	// _AuroraComponent
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public void initialize() {
		myLink = Utils.getLinkWithId(getLinkId());
		dtinhours = getDt().floatValue()/3600;										// assume given in hours
		samplesteps = Utils.round(dtinhours/Utils.simdt);
		isdone = false;
		demand = new Float3DMatrix(getContent(),false);
		
		// normalize demand and apply knob
		demand.multiplyscalar(Utils.simdt * getKnob().floatValue());
		
		if(Utils.freememory)
			setContent("");
		
	}
	
	@Override
	public boolean validate() {
		
		if(demand.isempty)
			return true;
		
		if(myLink==null){
			System.out.println("Bad link id in demand profile: " + getLinkId());
			return false;
		}
		
		// check dtinhours
		if( dtinhours<=0 ){
			System.out.println("Demand profile dt should be positive: " + getLinkId());
			return false;	
		}
		
		if(!Utils.isintegermultipleof(dtinhours,Utils.simdt)){
			System.out.println("Demand dt should be multiple of sim dt: " + getLinkId());
			return false;	
		}
		
		// check dimensions
		if(demand.n3!=Utils.numVehicleTypes){
			System.out.println("Incorrect dimensions for demand on link " + getLinkId());
			return false;
		}
		
		// check non-negative
		if(demand.hasNaN()){
			System.out.println("Illegal values in demand profile for link " + getLinkId());
			return false;
		}

		return true;
	}

	@Override
	public void reset() {
		isdone = false;
	}
	
	@Override
	public void update() {
		if(isdone || demand.isempty)
			return;
		if(Utils.clock.istimetosample(samplesteps)){
			
			int step = Utils.round(Utils.clock.getT()/dtinhours);
			
			if(step<demand.n2)
				myLink.setSourceDemand(demand.get(0,step));
			if(step==demand.n2-1)
				isdone = true;
			
if(this.myLink.getId().equals("-289")){
	System.out.println(Utils.clock.getCurrentstep() + "\t" + Utils.clock.getT() + "\t" + myLink.sourcedemand[0]);
}			
			
		}
	}
	
}
