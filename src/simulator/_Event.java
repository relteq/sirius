package simulator;

import jaxb.Event;

public class _Event extends Event implements AuroraComponent {

	protected enum TargetType {node,link,network};
	
	protected int timestampstep;
	
	protected Object target = null;
	protected String targetid;
	protected TargetType targettype;
	
	/////////////////////////////////////////////////////////////////////
	// _AuroraComponent
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public void initialize() {

		timestampstep = Utils.round(getTstamp().floatValue()/Utils.simdt);
		
		if(!getLinkId().isEmpty()){
			targettype = TargetType.link;
			targetid = getLinkId();
			target = Utils.getLinkWithId(targetid);
		}
		if(!getNodeId().isEmpty()){
			targettype = TargetType.node;
			targetid = getNodeId();
			target = Utils.getNodeWithId(targetid);
		}
		if(!getNetworkId().isEmpty()){
			targettype = TargetType.network;
			targetid = getNetworkId();
			target = Utils.theScenario.getNetwork();
		}
	}

	@Override
	public boolean validate() {
		// check that the target is valid
		if(target==null){
			System.out.println("Invalid target.");
			return false;
		}
		
		return true;
	}

	@Override
	public void reset() {
	}

	@Override
	public void update() {
	}

}
