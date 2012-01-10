package simulator;

import jaxb.Density;

public class _InitialDensityProfile extends jaxb.InitialDensityProfile implements AuroraComponent {

	/////////////////////////////////////////////////////////////////////
	// getters and helpers
	/////////////////////////////////////////////////////////////////////
	
	public Float [] getDensityForLinkId(String linkid){
		for(Density density : getDensity()){
			if(density.getLinkId().equals(linkid)){
				Float3DMatrix d = new Float3DMatrix(density.getContent(),true);
				if(d.length!=Utils.numVehicleTypes)
					System.out.println("ERROR: Wrong number of elements in initial density profile.");				
				return d.getvector();
			}
		}
		// not found, default to zero
		Float [] zero = new Float[Utils.numVehicleTypes];
		for(int i=0;i<zero.length;i++)
			zero[i] = 0f;
		return zero;
	}
	
	/////////////////////////////////////////////////////////////////////
	// _AuroraComponent
	/////////////////////////////////////////////////////////////////////
	
	@Override
	public void initialize() {
	}

	@Override
	public boolean validate() {
		
		for(Density density : getDensity()){
			String linkid = density.getLinkId();
			_Link mylink = Utils.getLinkWithId(linkid);

			if(density.getContent().isEmpty())
				continue;
			
			// check validity of link id
			if(mylink==null){
				System.out.println("Bad link id in split ratio profile: " + linkid);
				return false;
			}

			Float3DMatrix d = new Float3DMatrix(density.getContent(),true);
			
			// check number of values equals number of vehicle types
			if(d.n1!=Utils.numVehicleTypes){
				System.out.println("Number of initial densities in " + linkid + " does not equal the number of vehicle types.");
				return false;
			}
			
			// check that values are between 0 and jam density
			float sum = 0;
			for(Float x : d.getvector()){
				if(x<0 || x.isNaN()){
					System.out.println("Invalid initial density.");
					return false;
				}
				sum += x;
			}
			if(sum>mylink.densityJam){
				System.out.println("Initial density exceeds jam density.");
				return false;
			}
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
