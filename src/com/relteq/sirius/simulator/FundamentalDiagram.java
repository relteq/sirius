/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

final class FundamentalDiagram extends com.relteq.sirius.jaxb.FundamentalDiagram{

	protected Link myLink;
	protected double lanes;
	protected Double _densityJam;     		// [veh] 
	protected Double _capacity;   			// [veh] 
	protected Double _capacityDrop;     	// [veh] 
	protected Double _vf;                	// [-]
	protected Double _w;                	// [-]
	protected Double std_dev_capacity;		// [veh]
	protected Double density_critical;		// [veh]

	/////////////////////////////////////////////////////////////////////
	// construction 
	/////////////////////////////////////////////////////////////////////

	public FundamentalDiagram(){};
	
	public FundamentalDiagram(Link myLink){
		this.myLink       = myLink;
		this.lanes 		  = myLink==null ? Double.NaN : myLink._lanes;
		_densityJam 	  = Double.NaN;  
	    _capacity  		  = Double.NaN;
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
		return _capacity;
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

	protected void setLanes(double newlanes){
		if(newlanes<=0)
			return;
		if(SiriusMath.equals(newlanes,lanes))
			return;
		double alpha = newlanes/lanes;
		_densityJam 	  *= alpha; 
		_capacity  *= alpha; 
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
		_capacity  = Defaults.capacity		*lanes*simDtInHours;
		_capacityDrop 	  = Defaults.capacityDrop	*lanes*simDtInHours;
		_vf = Defaults.vf * simDtInHours / lengthInMiles;
        _w  = Defaults.w  * simDtInHours / lengthInMiles;
        density_critical =  _capacity / _vf;
	}

 	// copy per lane parameters from _FundamentalDiagram 
	protected void copyfrom(FundamentalDiagram that){

		if(that==null)
			return;
		if(myLink==null)
			return;
		
		if(that._densityJam!=null)
			this._densityJam = that._densityJam;

		if(that._capacity!=null)
			this._capacity = that._capacity;

		if(that.std_dev_capacity!=null)
			this.std_dev_capacity = that.std_dev_capacity;

		if(that._capacityDrop!=null)
			this._capacityDrop = that._capacityDrop;

		if(that._vf!=null)
			this._vf = that._vf;

		if(that._w!=null)
			this._w = that._w;

		this.density_critical =  this._capacity / this._vf;
        
	}
	
 	// copy per lane parameters from jaxb and normalize
	protected void copyfrom(com.relteq.sirius.jaxb.FundamentalDiagram fd){

		if(fd==null)
			return;
		if(myLink==null)
			return;
		
		double value;
		double simDtInHours = myLink.myNetwork.myScenario.getSimDtInHours();

		if(fd.getJamDensity()!=null){
			value = fd.getJamDensity().doubleValue();		// [veh/mile/lane]
			_densityJam = value *lanes*myLink.getLengthInMiles();
		} 

		if(fd.getCapacity()!=null){
			value = fd.getCapacity().doubleValue();			// [veh/hr/lane]
			_capacity = value * lanes*simDtInHours;
		} 
		
		if(fd.getStdDevCapacity()!=null){
			value = fd.getStdDevCapacity().doubleValue();	// [veh/hr/lane]
			std_dev_capacity = value * lanes*simDtInHours;
		}
		
		if(fd.getCapacityDrop()!=null){
			value = fd.getCapacityDrop().doubleValue();		// [veh/hr/lane]
			_capacityDrop = value * lanes*simDtInHours;
		} 
		
		if(fd.getFreeFlowSpeed()!=null){
			value = fd.getFreeFlowSpeed().doubleValue();		// [mile/hr]
			_vf = value * simDtInHours / myLink.getLengthInMiles();
		}

		if(fd.getCongestionSpeed()!=null){
			value = fd.getCongestionSpeed().doubleValue();		// [mile/hr]
			_w = value * simDtInHours / myLink.getLengthInMiles();
		}

		density_critical =  _capacity / _vf;
        
	}
	
	protected void reset(Scenario.UncertaintyType uncertaintyModel){
		if(myLink==null)
			return;
		// set lanes back to original value
		setLanes(myLink.get_Lanes());
	}
	
	// produce a sample fundamental diagram with this one as expected value.
	protected FundamentalDiagram perturb(){
		if(myLink==null)
			return null;
		// make a copy of this fundamental diagram
		FundamentalDiagram samp = new FundamentalDiagram(myLink);
		samp.copyfrom(this);
		
		// perturb it
		if(!std_dev_capacity.isNaN()){
			switch(myLink.myNetwork.myScenario.uncertaintyModel){
			case uniform:
				samp._capacity += SiriusMath.sampleZeroMeanUniform(std_dev_capacity);
				break;

			case gaussian:
				samp._capacity += SiriusMath.sampleZeroMeanUniform(std_dev_capacity);
				break;
			}			
		}
		
		// adjustments to sampled fd:
		
		// non-negativity
		samp._capacity = Math.max(samp._capacity,0.0);
		
		// density_critical no greater than dens_crit_congestion
		double dens_crit_congestion = samp._densityJam-samp._capacity/samp._w;	// [veh]
		if(SiriusMath.greaterthan(samp.density_critical,dens_crit_congestion)){
			samp.density_critical = dens_crit_congestion;
			samp._capacity = samp._vf * samp.density_critical;
		}

		return samp;
	}
	
	protected void validate(){
		
		String myLinkId;
		
		myLinkId = myLink==null ? "[invalid link id]" : myLink.getId();
		
		if(_vf<0 || _w<0 || _densityJam<0 || _capacity<0 || _capacityDrop<0)
			SiriusErrorLog.addError("Negative fundamental diagram parameters for link id=" + myLinkId);

		double dens_crit_congestion = _densityJam-_capacity/_w;	// [veh]
			
		if(SiriusMath.greaterthan(density_critical,dens_crit_congestion))
			SiriusErrorLog.addError("Minimum allowable critical density for link " + myLinkId + " is " + dens_crit_congestion);
		
		if(_vf>1)
			SiriusErrorLog.addError("CFL condition violated, FD for link " + myLinkId + " has vf=" + _vf);

		if(_w>1)
			SiriusErrorLog.addError("CFL condition violated, FD for link " + myLinkId + " has w=" + _w);
		
		if(myLink!=null)
			for(int e=0;e<myLink.myNetwork.myScenario.numEnsemble;e++)
				if(myLink.getTotalDensityInVeh(e)>_densityJam)
					SiriusErrorLog.addError("Initial density=" + myLink.getTotalDensityInVeh(e) + " of link id=" + myLinkId + " exceeds jam density=" + _densityJam);
	}

}
