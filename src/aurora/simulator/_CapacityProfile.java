package aurora.simulator;

public class _CapacityProfile extends aurora.jaxb.Capacity {

	private _Link myLink;
	private float dtinhours;
	private int samplesteps;
	private Float3DMatrix capacity;			// [veh]
	private boolean isdone;

	/////////////////////////////////////////////////////////////////////
	// initialize / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void initialize() {
		myLink = Utils.getLinkWithId(getLinkId());
		dtinhours = getDt().floatValue()/3600;										// assume given in hours
		samplesteps = Utils.round(dtinhours/Utils.simdt);
		isdone = false;
		capacity = new Float3DMatrix(getContent(),true);	// true=> reshape to vector along k, define length
		
		// normalize capacity
		capacity.multiplyscalar(Utils.simdt);

		if(Utils.freememory)
			setContent("");
	}
	
	protected boolean validate() {
		
		if(capacity.isEmpty())
			return true;
		
		if(myLink==null){
			System.out.println("Bad link id in capacity profile: " + getLinkId());
			return false;
		}
		
		// check dtinhours
		if( dtinhours<=0 ){
			System.out.println("Capacity profile dt should be positive: " + getLinkId());
			return false;	
		}

		if(!Utils.isintegermultipleof(dtinhours,Utils.simdt)){
			System.out.println("Capacity dt should be multiple of sim dt: " + getLinkId());
			return false;	
		}
		
		// check dimensions
		if(!capacity.isVector()){
			System.out.println("Capacity profile must be a vector: " + getLinkId());
			return false;
		}

		// check non-negative
		if(capacity.hasNaN()){
			System.out.println("Capacity profile has illegal values: " + getLinkId());
			return false;
		}

		return true;
	}

	protected void reset() {
		isdone = false;
	}
	
	protected void update() {
		if(isdone || capacity.isEmpty())
			return;
		if(Utils.clock.istimetosample(samplesteps)){
			int n = capacity.getLength()-1;
			int step = Math.min(n,Utils.floor(Utils.clock.getT()/dtinhours));				
			if(step<=n)
				myLink.capacity = capacity.get(step);
			if(step>=n)
				isdone = true;
		}
	}

}
