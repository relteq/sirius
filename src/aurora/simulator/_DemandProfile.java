package aurora.simulator;

public class _DemandProfile extends aurora.jaxb.Demand {

	protected _Link myLink;
	protected float dtinhours;
	protected int samplesteps;
	protected Float3DMatrix demand;			// [veh]
	protected boolean isdone; 
	
	/////////////////////////////////////////////////////////////////////
	// initialize / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void initialize() {
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
	
	protected boolean validate() {
		
		if(demand.isEmpty())
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
		if(demand.getN3()!=Utils.numVehicleTypes){
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

	protected void reset() {
		isdone = false;
	}
	
	protected void update() {
		if(isdone || demand.isEmpty())
			return;
		if(Utils.clock.istimetosample(samplesteps)){
			int n = demand.getN2()-1;
			int step = Math.min(n,Utils.floor(Utils.clock.getT()/dtinhours));	
			if(step<=n)
				myLink.sourcedemand = demand.get(0,step);
			if(step>=n)
				isdone = true;		
		}
	}
	
}
