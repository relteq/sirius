package aurora.simulator;

import java.util.ArrayList;

import aurora.jaxb.Splitratios;
import aurora.jaxb.Srm;

public class _SplitRatiosProfile extends Splitratios {

	protected _Node myNode;
	protected float dtinhours;
	protected int samplesteps;
	protected ArrayList<Float3DMatrix> splitratio;
	protected boolean isdone; 

	/////////////////////////////////////////////////////////////////////
	// initialize / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void initialize() {
		myNode = Utils.getNodeWithId(getNodeId());
		dtinhours = getDt().floatValue()/3600f;										// assume given in hours
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

	protected void reset() {
	}

	protected boolean validate() {
		
		if(splitratio.isEmpty())
			return true;
		
		if(myNode.getMyType()==Types.Node.T)
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
			if(X.getN1()!=myNode.getnIn() || X.getN2()!=myNode.getnOut() || X.getN3()!=Utils.numVehicleTypes){
				System.out.println("Split ratio for node " + getNodeId() + " has incorrect dimension");
				return false;
			}
			
			// range
			for(i=0;i<X.getN1();i++){
				for(j=0;j<X.getN2();j++){
					for(k=0;k<X.getN3();k++){
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

	protected void update() {
		if(isdone || splitratio.isEmpty())
			return;
		if(Utils.clock.istimetosample(samplesteps)){
			int n = splitratio.size()-1;
			int step = Math.min(n,Utils.floor(Utils.clock.getT()/dtinhours));			
			if(step<=n){
				myNode.setSampledSRProfile(splitratio.get(step));
			}
			if(step>=n)
				isdone = true;
		}
	}

}
