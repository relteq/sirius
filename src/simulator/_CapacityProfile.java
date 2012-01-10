package simulator;

import jaxb.Capacity;

public class _CapacityProfile extends Capacity implements AuroraComponent {

	private _Link myLink;
	private float dtinhours;
	private int samplesteps;
	private Float3DMatrix capacity;			// [veh]
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
		capacity = new Float3DMatrix(getContent(),true);	// true=> reshape to vector along k, define length
		
		// normalize capacity
		capacity.multiplyscalar(Utils.simdt);

		if(Utils.freememory)
			setContent("");
	}
	
	@Override
	public boolean validate() {
		
		if(capacity.isempty)
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
		if(!capacity.isvector){
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

	@Override
	public void reset() {
		isdone = false;
	}
	
	@Override
	public void update() {
		if(isdone || capacity.isempty)
			return;
		if(Utils.clock.istimetosample(samplesteps)){
			int step = Utils.floor(Utils.clock.getT()/dtinhours);
			if(step<capacity.length)
				myLink.setCapacity(capacity.get(step));
			if(step==capacity.length-1)
				isdone = true;
		}
	}

}
