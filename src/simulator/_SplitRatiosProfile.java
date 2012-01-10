package simulator;

import java.util.ArrayList;

import jaxb.Splitratios;
import jaxb.Srm;

public class _SplitRatiosProfile extends Splitratios implements AuroraComponent {

	private _Node myNode;
	private float dtinhours;
	private int samplesteps;
	private ArrayList<Float3DMatrix> splitratio;
	private boolean isdone; 

	/////////////////////////////////////////////////////////////////////
	// _AuroraComponent
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public void initialize() {
		myNode = Utils.getNodeWithId(getNodeId());
		dtinhours = getDt().floatValue()/3600;										// assume given in hours
		samplesteps = Utils.round(dtinhours/Utils.simdt);
		isdone = false;
		splitratio = new ArrayList<Float3DMatrix>();
		for(Srm srm : getSrm())
			splitratio.add(new Float3DMatrix(srm.getContent(),false));
		
		for(Float3DMatrix srm : splitratio)
			srm.normalizeSplitRatioMatrix();
		
		// inform the node
		myNode.setHasSRprofile(true);
		
		if(Utils.freememory)
			getSrm().clear();
	}

	@Override
	public void reset() {
	}

	@Override
	public boolean validate() {
		
		if(splitratio.isEmpty())
			return true;
		
		if(myNode.isterminal)
			return true;
		
		if(myNode==null){
			System.out.println("Bad node id in split ratio profile: " + getNodeId());
			return false;
		}

		// check dtinhours
		if( dtinhours<=0 ){
			System.out.println("Split ratio profile dt should be positive: " + getNodeId());
			return false;	
		}

		if(!Utils.isintegermultipleof(dtinhours,Utils.simdt)){
			System.out.println("Split ratio dt should be multiple of sim dt: " + getNodeId());
			return false;	
		}
		
		// check split ratio dimensions and values
		int i,j,k;
		Float value;
		for(Float3DMatrix X : splitratio){

			// dimension
			if(X.n1!=myNode.nIn || X.n2!=myNode.nOut || X.n3!=Utils.numVehicleTypes){
				System.out.println("Split ratio for node " + getNodeId() + " has incorrect dimension");
				return false;
			}
			
			// range
			for(i=0;i<X.n1;i++){
				for(j=0;j<X.n2;j++){
					for(k=0;k<X.n3;k++){
						value = X.get(i,j,k);
						if( !value.isNaN() && (value>1 || value<0) ){
							System.out.println("Split ratio values must be in [0,1]");
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	@Override
	public void update() {

		if(isdone || splitratio.isEmpty())
			return;
		if(Utils.clock.istimetosample(samplesteps)){
			int step = Utils.floor(Utils.clock.getT()/dtinhours);			
			if(step<splitratio.size())
				myNode.setSampledSRProfile(splitratio.get(step));
			if(step==splitratio.size()-1)
				isdone = true;
		}
	}

}
