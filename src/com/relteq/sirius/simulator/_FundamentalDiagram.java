/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

public class _FundamentalDiagram extends com.relteq.sirius.jaxb.FundamentalDiagram{

	private _Link myLink;
	private double lanes;
	private Double _densityCritical; 	// [veh]           
	private Double _densityJam;     	// [veh] 
    private Double _capacity_nominal;   // [veh] 
    private Double _capacity_actual;   	// [veh] 
	private Double _capacityDrop;     	// [veh] 
    private double _vf;                	// [-]
    private double _w;                	// [-]
    private double std_dev_capacity;	// [veh]

	/////////////////////////////////////////////////////////////////////
	// construction 
	/////////////////////////////////////////////////////////////////////

	public _FundamentalDiagram(_Link myLink){
		if(myLink==null)
			return;
		this.myLink = myLink;
		this.lanes = myLink.get_Lanes();
		_densityCritical  = Double.NaN;         
		_densityJam 	  = Double.NaN;  
	    _capacity_nominal = Double.NaN; 
	    _capacity_actual  = Double.NaN;
		_capacityDrop 	  = Double.NaN; 
	    _vf 			  = Double.NaN; 
	    _w 				  = Double.NaN; 
	    std_dev_capacity  = Double.NaN;
	}
	
	/////////////////////////////////////////////////////////////////////
	// public interface
	/////////////////////////////////////////////////////////////////////

	// we do not have to worry about getters returning NaN:
	// they are only called for fundamental diagrams belonging
	// to links, these are initialized with default values, and 
	// copyfrom only replaces with non-nan values.
	
	public Double _getDensityCritical() {
		return _densityCritical;
	}

	public Double _getDensityJam() {
		return _densityJam;
	}

	public Double _getCapacity() {
		return _capacity_actual;
	}

	public Double _getCapacityDrop() {
		return _capacityDrop;
	}

	public double getVf() {
		return _vf;
	}

	public double getW() {
		return _w;
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
		_densityCritical *= alpha;          
		_densityJam 	 *= alpha; 
		_capacity_actual *= alpha; 
		_capacity_nominal *= alpha; 
		_capacityDrop 	 *= alpha; 
		lanes = newlanes;
	}
	
	/////////////////////////////////////////////////////////////////////
	// reset / validate
	/////////////////////////////////////////////////////////////////////

	// assign default values parameters and normalize
 	protected void settoDefault(){
		if(myLink==null)
			return;
		_densityCritical = Defaults.densityCritical	*lanes*myLink.getLengthInMiles();
		_densityJam 	 = Defaults.densityJam		*lanes*myLink.getLengthInMiles();
		_capacity_nominal= Defaults.capacity		*lanes*Utils.simdtinhours;
		_capacity_actual = Defaults.capacity		*lanes*Utils.simdtinhours;
		_capacityDrop 	 = Defaults.capacityDrop	*lanes*Utils.simdtinhours;
		_vf = computeVf();
        _w  = computeW();
	}

 	// copy per lane parameters from jaxb and normalize
	protected void copyfrom(com.relteq.sirius.jaxb.FundamentalDiagram fd){

		if(fd==null)
			return;
		
		double value;
		
		if(fd.getDensityCritical()!=null){
			value = fd.getDensityCritical().doubleValue();	// [veh/mile/lane]
			_densityCritical = value * lanes*myLink.getLengthInMiles();	
		}

		if(fd.getDensityJam()!=null){
			value = fd.getDensityJam().doubleValue();			// [veh/mile/lane]
			_densityJam = value *lanes*myLink.getLengthInMiles();
		} 

		if(fd.getCapacity()!=null){
			value = fd.getCapacity().doubleValue();			// [veh/hr/lane]
			_capacity_nominal = value * lanes*Utils.simdtinhours;
		} 
		
		if(fd.getStdDevCapacity()!=null){
			value = fd.getStdDevCapacity().doubleValue();			// [veh/hr/lane]
			std_dev_capacity = value * lanes*Utils.simdtinhours;
		}
		
		if(fd.getCapacityDrop()!=null){
			value = fd.getCapacityDrop().doubleValue();		// [veh/hr/lane]
			_capacityDrop = value * lanes*Utils.simdtinhours;
		} 
		
		// compute vf and w
        _vf = computeVf();		// [0,1]
        _w  = computeW();		// [0,1]
        
	}
	
	protected void reset(){
	
		// set lanes back to original value
		setLanes(myLink.get_Lanes());
		
		// sample the capacity distribution
		_capacity_actual = _capacity_nominal;
		if(std_dev_capacity!=Double.NaN){
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
		if(_densityCritical<0 || _densityJam<0 || _capacity_nominal<0 || _capacity_actual<0 || _capacityDrop<0){
			System.out.println("Fundamental diagram parameters must be non-negative.");
			return false;
		}
		
		if(_densityCritical>=_densityJam){
			System.out.println("Critical density must be smaller than jam density.");
			return false;
		}

        Double vf = computeVf();
        Double w  = computeW();
        
        if(vf.isInfinite() || w.isInfinite()){
        	System.out.println("Infinite freeflow or congestion wave speed.");
        	return false;
        }

        if(vf.isNaN() || w.isNaN()){
        	System.out.println("Infinite freeflow or congestion wave speed.");
        	return false;
        }
        	
		if(vf>1 || w>1){
			System.out.println("CFL condition violated");
			return false;
		}
		return true;
	}
	
	/////////////////////////////////////////////////////////////////////
	// private methods
	/////////////////////////////////////////////////////////////////////

	private Double computeVf(){	
		if(_densityCritical==0)
			return Double.POSITIVE_INFINITY;
		return _capacity_actual / _densityCritical;
	}
	
	private Double computeW(){
		if(_densityJam<=_densityCritical)
			return Double.POSITIVE_INFINITY;		
		return _capacity_actual / (_densityJam-_densityCritical);
	}
	
}
