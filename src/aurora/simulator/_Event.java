package aurora.simulator;

public class _Event extends aurora.jaxb.Event {

	protected Types.Event myType;
	protected Types.Element myTargetType;
	
	protected int timestampstep;
	
	protected Object target = null;
	protected String targetid;
	
	/////////////////////////////////////////////////////////////////////
	// interface
	/////////////////////////////////////////////////////////////////////
	
	public Types.Event getMyType() {
		return myType;
	}
	
	public Types.Element getMyTargetType() {
		return myTargetType;
	}

	/////////////////////////////////////////////////////////////////////
	// initialize / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void initialize() {

		// assign type
    	try {
			myType = Types.Event.valueOf(getType());
		} catch (IllegalArgumentException e) {
			myType = null;
			return;
		}
		
		timestampstep = Utils.round(getTstamp().floatValue()/Utils.simdt);
		
		if(!getLinkId().isEmpty()){
			myTargetType = Types.Element.LINK;
			targetid = getLinkId();
			target = Utils.getLinkWithId(targetid);
		}
		if(!getNodeId().isEmpty()){
			myTargetType = Types.Element.NODE;
			targetid = getNodeId();
			target = Utils.getNodeWithId(targetid);
		}
		if(!getNetworkId().isEmpty()){
			myTargetType = Types.Element.NETWORK;
			targetid = getNetworkId();
			target = Utils.theScenario.getNetwork();
		}
	}

	protected boolean validate() {
		// check that the target is valid
		if(target==null){
			System.out.println("Invalid target.");
			return false;
		}
		
		return true;
	}

	protected void reset() {
	}

	protected void update() {
	}

}
