/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

final class _FundamentalDiagram extends com.relteq.sirius.jaxb.FundamentalDiagram{

	private _Link myLink;
	private double lanes;
	private Double _densityJam;     	// [veh] 
    private Double _capacity_nominal;   // [veh] 
    private Double _capacity_actual;   	// [veh] 
	private Double _capacityDrop;     	// [veh] 
    private Double _vf;                	// [-]
    private Double _w;                	// [-]
    private Double std_dev_capacity;	// [veh]
    
    private Double density_critical;	// [veh]

	/////////////////////////////////////////////////////////////////////
	// construction 
	/////////////////////////////////////////////////////////////////////

	public _FundamentalDiagram(_Link myLink){
		if(myLink==null)
			return;
		this.myLink = myLink;
		this.lanes = myLink.get_Lanes();
		_densityJam 	  = Double.NaN;  
	    _capacity_nominal = Double.NaN; 
	    _capacity_actual  = Double.NaN;
		_capacityDrop 	  = Double.NaN; 
	    _vf 			  = Double.NaN; 
	    _w 				  = Double.NaN; 
	    std_dev_capacity  = Double.NaN;
	    density_critical  = Double.NaN;
	}
	
	/////////////////////////////////////////////////////////////////////
	// public interface
	/////////////////////////////////////////////////////////////////////

	// we do not have to worry about getters returning NaN:
	// they are only called for fundamental diagrams belonging
	// to links, these are initialized with default values, and 
	// copyfrom only replaces with non-nan values.

	public Double _getDensityJamInVeh() {
		return _densityJam;
	}

	public Double _getCapacityInVeh() {
		return _capacity_actual;
	}

	public Double _getCapacityDropInVeh() {
		return _capacityDrop;
	}

	public double getVfNormalized() {
		return _vf;
	}

	public double getWNormalized() {
		return _w;
	}

	public Double getDensityCriticalInVeh() {
		return density_critical;
	}
	
	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////

	// used by CapacityProfile
	protected void setCapacityFromVeh(double c) {
		_capacity_actual = c*lanes*Utils.simdtinhours;
		_capacity_nominal = _capacity_actual;
	}

	protected void setLanes(double newlanes){
		if(newlanes<=0)
			return;
		double alpha = newlanes/lanes;
		_densityJam 	  *= alpha; 
		_capacity_actual  *= alpha; 
		_capacity_nominal *= alpha; 
		_capacityDrop 	  *= alpha; 
		density_critical  *= alpha;
		lanes = newlanes;
	}
	
	/////////////////////////////////////////////////////////////////////
	// reset / validate
	/////////////////////////////////////////////////////////////////////

	// assign default values parameters and normalize
 	protected void settoDefault(){
		if(myLink==null)
			return;
		_densityJam 	  = Defaults.densityJam		*lanes*myLink.getLengthInMiles();
		_capacity_nominal = Defaults.capacity		*lanes*Utils.simdtinhours;
		_capacity_actual  = Defaults.capacity		*lanes*Utils.simdtinhours;
		_capacityDrop 	  = Defaults.capacityDrop	*lanes*Utils.simdtinhours;
		_vf = Defaults.vf * Utils.simdtinhours / myLink.getLengthInMiles();
        _w  = Defaults.w  * Utils.simdtinhours / myLink.getLengthInMiles();
        density_critical =  _capacity_actual / _vf;
	}

 	// copy per lane parameters from jaxb and normalize
	protected void copyfrom(com.relteq.sirius.jaxb.FundamentalDiagram fd){

		if(fd==null)
			return;

		if(myLink==null)
			return;
		
		double value;

		if(fd.getDensityJam()!=null){
			value = fd.getDensityJam().doubleValue();			// [veh/mile/lane]
			_densityJam = value *lanes*myLink.getLengthInMiles();
		} 

		if(fd.getCapacity()!=null){
			value = fd.getCapacity().doubleValue();			// [veh/hr/lane]
			_capacity_nominal = value * lanes*Utils.simdtinhours;
			_capacity_actual = _capacity_nominal;
		} 
		
		if(fd.getStdDevCapacity()!=null){
			value = fd.getStdDevCapacity().doubleValue();	// [veh/hr/lane]
			std_dev_capacity = value * lanes*Utils.simdtinhours;
		}
		
		if(fd.getCapacityDrop()!=null){
			value = fd.getCapacityDrop().doubleValue();		// [veh/hr/lane]
			_capacityDrop = value * lanes*Utils.simdtinhours;
		} 
		
		if(fd.getFreeflowSpeed()!=null){
			value = fd.getFreeflowSpeed().doubleValue();		// [mile/hr]
			_vf = value * Utils.simdtinhours / myLink.getLengthInMiles();
		}

		if(fd.getCongestionSpeed()!=null){
			value = fd.getCongestionSpeed().doubleValue();		// [mile/hr]
			_w = value * Utils.simdtinhours / myLink.getLengthInMiles();
		}

		density_critical =  _capacity_actual / _vf;
        
	}
	
	protected void reset(){
	
		// set lanes back to original value
		setLanes(myLink.get_Lanes());
		
		// sample the capacity distribution
		_capacity_actual = _capacity_nominal;
		if(!std_dev_capacity.isNaN()){
			switch(Utils.uncertaintyModel){
			case uniform:
				_capacity_actual += std_dev_capacity*Math.sqrt(3)*(2*Utils.random.nextDouble()-1);
				break;

			case gaussian:
				_capacity_actual += std_dev_capacity*Utils.random.nextGaussian();
				break;
			}			
		}
		
		// non-negativity
		_capacity_actual = Math.max(_capacity_actual,0.0);

	}
	
	protected boolean validate(){
		if(_vf<0 || _w<0 || _densityJam<0 || _capacity_nominal<0 || _capacity_actual<0 || _capacityDrop<0){
			System.out.println("Fundamental diagram parameters must be non-negative.");
			return false;
		}
		
		double dens_crit_congestion = _densityJam-_capacity_nominal/_w;	// [veh]
			
		if(Utils.greaterthan(density_critical,dens_crit_congestion)){
			System.out.println("Invalid fundamental diagram.");
			return false;
		}
        	
		if(_vf>1 || _w>1){
			System.out.println("CFL condition violated");
			return false;
		}
		return true;
	}

}
