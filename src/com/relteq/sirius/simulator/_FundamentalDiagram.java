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
    private Double _capacity;          	// [veh] 
	private Double _capacityDrop;     	// [veh] 
    private double _vf;                	// [-]
    private double _w;                	// [-]

	/////////////////////////////////////////////////////////////////////
	// construction 
	/////////////////////////////////////////////////////////////////////

	public _FundamentalDiagram(_Link myLink){
		if(myLink==null)
			return;
		this.myLink = myLink;
		this.lanes = myLink.get_Lanes();
		_densityCritical = Double.NaN;         
		_densityJam 	 = Double.NaN;  
	    _capacity 		 = Double.NaN; 
		_capacityDrop 	 = Double.NaN; 
	    _vf 			 = Double.NaN; 
	    _w 				 = Double.NaN; 
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
		return _capacity;
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

	protected void setCapacityFromVeh(double c) {
		_capacity = c*lanes*Utils.simdtinhours;
	}

	protected void setLanes(double newlanes){
		if(newlanes<=0)
			return;
		double alpha = newlanes/lanes;
		_densityCritical *= alpha;          
		_densityJam 	 *= alpha; 
	    _capacity 		 *= alpha; 
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
		_capacity 		 = Defaults.capacity		*lanes*Utils.simdtinhours;
		_capacityDrop 	 = Defaults.capacityDrop	*lanes*Utils.simdtinhours;
		_vf = computeVf();
        _w  = computeW();
	}
	
 	// copy per lane parameters from jaxb and normalize
	protected void copyfrom(com.relteq.sirius.jaxb.FundamentalDiagram fd){

		if(fd==null)
			return;
		
		double value;
		try {
			value = Double.parseDouble(fd.getDensityCritical());	// [veh/mile/lane]
			_densityCritical = value * lanes*myLink.getLengthInMiles();
		} catch (NumberFormatException e) {
			// keep current value
		}

		try {
			value = Double.parseDouble(fd.getDensityJam());			// [veh/mile/lane]
			_densityJam = value *lanes*myLink.getLengthInMiles();
		} catch (NumberFormatException e) {
			// keep current value
		}

		try {
			value = Double.parseDouble(fd.getCapacity());			// [veh/hr/lane]
			_capacity = value * lanes*Utils.simdtinhours;
		} catch (NumberFormatException e) {
			// keep current value
		}

		try {
			value =  Double.parseDouble(fd.getCapacityDrop());		// [veh/hr/lane]
			_capacityDrop = value * lanes*Utils.simdtinhours;
		} catch (NumberFormatException e) {
			// keep current value
		}
		
		// compute vf and w
        _vf = computeVf();		// [0,1]
        _w  = computeW();		// [0,1]
        
	}
	
//	protected void reset(com.relteq.sirius.jaxb.FundamentalDiagram fd){
//		// assume the link has been reset, so the lanes are original
//		lanes = myLink.get_Lanes();
//		_densityCritical = Double.NaN;         
//		_densityJam 	 = Double.NaN;  
//	    _capacity 		 = Double.NaN; 
//		_capacityDrop 	 = Double.NaN; 
//	    _vf 			 = Double.NaN; 
//	    _w 				 = Double.NaN; 
//	    copyfrom(fd);
//	}
	
	protected boolean validate(){
		if(_densityCritical<0 || _densityJam<0 || _capacity<0 || _capacityDrop<0){
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
		return _capacity / _densityCritical;
	}
	
	private Double computeW(){
		if(_densityJam<=_densityCritical)
			return Double.POSITIVE_INFINITY;		
		return _capacity / (_densityJam-_densityCritical);
	}
	
}
