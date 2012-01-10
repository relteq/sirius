package simulator;

import jaxb.*;

public class _Network extends Network implements AuroraComponent {

	/////////////////////////////////////////////////////////////////////
	// _AuroraComponent
	/////////////////////////////////////////////////////////////////////

	@Override
	public void initialize() {
		
		if(getNodeList()!=null)
			for (Node node : getNodeList().getNode())
				((_Node) node).initialize();
		
		if(getLinkList()!=null)
			for (Link link : getLinkList().getLink())
				((_Link) link).initialize();
		
		// free up some memory
		if(Utils.freememory){
			directionsCache = null;
		}
	}

	@Override
	public boolean validate() {

		if(Utils.simdt<=0)
			return false;
		
		// node list
		if(getNodeList()!=null)
			for (Node node : getNodeList().getNode())
				if( !((_Node)node).validate() )
					return false;

		// link list
		if(getLinkList()!=null)
			for (Link link : getLinkList().getLink())
				if( !((_Link)link).validate() )
					return false;

		// sensor list
		if(getSensorList()!=null)
			for (Sensor sensor : getSensorList().getSensor())
				if( !((_Sensor)sensor).validate())
					return false;

		// signal list
		if(getSignalList()!=null)
			for (Signal signal : getSignalList().getSignal())
				if( !((_Signal)signal).validate() )
					return false;
		
		return true;
	}

	@Override
	public void reset() {
		
		// node list
		if(getNodeList()!=null)
			for (Node node : getNodeList().getNode())
				((_Node)node).reset();

		// link list
		if(getLinkList()!=null)
			for (Link link : getLinkList().getLink())
				((_Link)link).reset();

		// sensor list
		if(getSensorList()!=null)
			for (Sensor sensor : getSensorList().getSensor())
				((_Sensor)sensor).reset();

		// signal list
		if(getSignalList()!=null)
			for (Signal signal : getSignalList().getSignal())
				((_Signal)signal).reset();
				
	}

	@Override
	public void update() {
		
        // compute link demand and supply ...............
        for(Link link : getLinkList().getLink()){
        	((_Link)link).updateOutflowDemand();
        	((_Link)link).updateSpaceSupply();
        }
        
        // update sensor readings .......................
        for(Sensor sensor : getSensorList().getSensor())
            ((_Sensor)sensor).update();
        
        // update nodes: compute flows on links .........
        for(Node node : getNodeList().getNode())
            ((_Node)node).update();
        
        // update links: compute densities .............
        for(Link link : getLinkList().getLink())
        	((_Link)link).update();
	}

}
