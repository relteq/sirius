package aurora.control;

import aurora.simulator.Utils;
import aurora.simulator._Controller;
import aurora.simulator._Link;
import aurora.simulator._Node;

public class ControllerAlinea extends _Controller {

	private float targetvehicles;		// [veh]
	private float gain;					// [-]
	private _Link mainlinelink;
	private float maxrate;				// [veh]
	private float minrate;				// [veh]

	/////////////////////////////////////////////////////////////////////
	// Construction
	/////////////////////////////////////////////////////////////////////
	
	public ControllerAlinea(aurora.jaxb.Controller c,aurora.simulator.Types.Controller myType) {
		super(c,myType);
		
		
		// initialize mainlinelink to downstrean mainline
		_Node endnode = ((_Link) target).getEnd_node();
		mainlinelink = null;
		for(_Link link : endnode.getOutput_link()){
			if(link.getType().equals("ML") || link.getType().equals("FW")){
				mainlinelink = link;
				break;
			}
		}
		
		if(mainlinelink!=null)
			gain = 50f * Utils.simdt / mainlinelink.getLinkLength() ;
		else
			gain = 0f;
		
		minrate = 0f;
		maxrate = 900f * Utils.simdt;
		
		targetvehicles = 0f;
		if(mainlinelink!=null)
			targetvehicles = mainlinelink.getDensityCritical();
		
	}
	
	/////////////////////////////////////////////////////////////////////
	// _AuroraComponent
	/////////////////////////////////////////////////////////////////////

	@Override
	public boolean validate() {
		if(!super.validate())
			return false;
		if(gain<=0f)
			return false;
		if(mainlinelink==null)
			return false;
		if(targetvehicles<=0f)
			return false;
		return true;
	}

	@Override
	public void reset() {
		super.reset();
		System.out.println("RESET ALINEA");
	}

	@Override
	public void update() {
		super.update();
		float desiredvehrate = ((_Link) target).getTotalOutflowInVeh() + gain*(targetvehicles-mainlinelink.getTotalDensityInVeh());
		desiredvehrate = Math.max(desiredvehrate, minrate);
		desiredvehrate = Math.min(desiredvehrate, maxrate);
		((_Link)target).control_maxflow = desiredvehrate;
	}

}
