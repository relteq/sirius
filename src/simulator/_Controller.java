package simulator;

import jaxb.Controller;

public class _Controller extends Controller implements AuroraComponent {
	
	protected enum TargetType {node,link,network};
	
	protected float dtinhours;
	protected int samplesteps;
	
	protected Object target = null;
	protected String targetid;
	protected TargetType targettype;
	
	@Override
	public void initialize() {
		dtinhours = getDt().floatValue()/3600f;
		samplesteps = Utils.round(dtinhours/Utils.simdt);
		
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
		
		// check that sample dt is an integer multiple of network dt
		if(!Utils.isintegermultipleof(dtinhours,Utils.simdt)){
			System.out.println("Controller sample time must be integer multiple of simulation time step.");
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
