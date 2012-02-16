/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.Random;

final class _FundamentalDiagram extends com.relteq.sirius.jaxb.FundamentalDiagram{

	//protected _Scenario myScenario;
	protected _Link myLink;
	protected double lanes;
	protected Double _densityJam;     	// [veh] 
	protected Double _capacity_nominal;   // [veh] 
	protected Double _capacity_actual;   	// [veh] 
	protected Double _capacityDrop;     	// [veh] 
	protected Double _vf;                	// [-]
	protected Double _w;                	// [-]
	protected Double std_dev_capacity;	// [veh]
	protected Double density_critical;	// [veh]

	/////////////////////////////////////////////////////////////////////
	// construction 
	/////////////////////////////////////////////////////////////////////

	public _FundamentalDiagram(_Link myLink){
		if(myLink==null)
			return;
		this.myLink = myLink;
		this.lanes = myLink._lanes;
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
	// protected interface
	/////////////////////////////////////////////////////////////////////

	// we do not have to worry about getters returning NaN:
	// they are only called for fundamental diagrams belonging
	// to links, these are initialized with default values, and 
	// copyfrom only replaces with non-nan values.

	protected Double _getDensityJamInVeh() {
		return _densityJam;
	}

	protected Double _getCapacityInVeh() {
		return _capacity_actual;
	}

	protected Double _getCapacityDropInVeh() {
		return _capacityDrop;
	}

	protected double getVfNormalized() {
		return _vf;
	}

	protected double getWNormalized() {
		return _w;
	}

	protected Double getDensityCriticalInVeh() {
		return density_critical;
	}
	
	/////////////////////////////////////////////////////////////////////
	// protected interface
	/////////////////////////////////////////////////////////////////////

	// used by CapacityProfile
	protected void setCapacityFromVeh(double c) {
		_capacity_actual = c*lanes*myLink.myNetwork.myScenario.getSimDtInHours();
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
		double simDtInHours = myLink.myNetwork.myScenario.getSimDtInHours();
		double lengthInMiles = myLink.getLengthInMiles();
		_densityJam 	  = Defaults.densityJam		*lanes*myLink.getLengthInMiles();
		_capacity_nominal = Defaults.capacity		*lanes*simDtInHours;
		_capacity_actual  = Defaults.capacity		*lanes*simDtInHours;
		_capacityDrop 	  = Defaults.capacityDrop	*lanes*simDtInHours;
		_vf = Defaults.vf * simDtInHours / lengthInMiles;
        _w  = Defaults.w  * simDtInHours / lengthInMiles;
        density_critical =  _capacity_actual / _vf;
	}

 	// copy per lane parameters from jaxb and normalize
	protected void copyfrom(com.relteq.sirius.jaxb.FundamentalDiagram fd){

		if(fd==null)
			return;
		if(myLink==null)
			return;
		
		double value;
		double simDtInHours = myLink.myNetwork.myScenario.getSimDtInHours();

		if(fd.getDensityJam()!=null){
			value = fd.getDensityJam().doubleValue();			// [veh/mile/lane]
			_densityJam = value *lanes*myLink.getLengthInMiles();
		} 

		if(fd.getCapacity()!=null){
			value = fd.getCapacity().doubleValue();			// [veh/hr/lane]
			_capacity_nominal = value * lanes*simDtInHours;
			_capacity_actual = _capacity_nominal;
		} 
		
		if(fd.getStdDevCapacity()!=null){
			value = fd.getStdDevCapacity().doubleValue();	// [veh/hr/lane]
			std_dev_capacity = value * lanes*simDtInHours;
		}
		
		if(fd.getCapacityDrop()!=null){
			value = fd.getCapacityDrop().doubleValue();		// [veh/hr/lane]
			_capacityDrop = value * lanes*simDtInHours;
		} 
		
		if(fd.getFreeflowSpeed()!=null){
			value = fd.getFreeflowSpeed().doubleValue();		// [mile/hr]
			_vf = value * simDtInHours / myLink.getLengthInMiles();
		}

		if(fd.getCongestionSpeed()!=null){
			value = fd.getCongestionSpeed().doubleValue();		// [mile/hr]
			_w = value * simDtInHours / myLink.getLengthInMiles();
		}

		density_critical =  _capacity_actual / _vf;
        
	}
	
	protected void reset(_Scenario.UncertaintyType uncertaintyModel){
	
		Random random = myLink.myNetwork.myScenario.random;
			
		// set lanes back to original value
		setLanes(myLink.get_Lanes());
		
		// sample the capacity distribution
		_capacity_actual = _capacity_nominal;
		if(!std_dev_capacity.isNaN()){
			switch(uncertaintyModel){
			case uniform:
				_capacity_actual += std_dev_capacity*Math.sqrt(3)*(2*random.nextDouble()-1);
				break;

			case gaussian:
				_capacity_actual += std_dev_capacity*random.nextGaussian();
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
			
		if(SiriusMath.greaterthan(density_critical,dens_crit_congestion)){
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
