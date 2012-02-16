/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import com.relteq.sirius.jaxb.*;

 final class _JaxbObjectFactory extends com.relteq.sirius.jaxb.ObjectFactory {
	
	@Override
	public CapacityProfile createCapacityProfile() {
		return new _CapacityProfile();
	}
	
	@Override
	public ControllerSet createControllerSet() {
		// TODO Auto-generated method stub
		return super.createControllerSet();
	}
	
	@Override
	public DemandProfile createDemandProfile() {
		return new _DemandProfile();
	}

	@Override
	public DemandProfileSet createDemandProfileSet() {
		return new _DemandProfileSet();
	}
	
	@Override
	public Event createEvent() {
		return new _Event();
	}

	@Override
	public FundamentalDiagram createFundamentalDiagram() {
		// TODO Auto-generated method stub
		return super.createFundamentalDiagram();
	}
	
	@Override
	public FundamentalDiagramProfile createFundamentalDiagramProfile() {
		return new _FundamentalDiagramProfile();
	}	
	
	@Override
	public InitialDensityProfile createInitialDensityProfile() {
		return new _InitialDensityProfile();
	}

	@Override
	public com.relteq.sirius.jaxb.Link createLink() {
		return new _Link();
	}

	@Override
	public Network createNetwork() {
		return new _Network();
	}

	@Override
	public Node createNode() {
		return new _Node();
	}

	@Override
	public Scenario createScenario() {
		return new _Scenario();
	}

	@Override
	public ScenarioElement createScenarioElement() {
		// TODO Auto-generated method stub
		return super.createScenarioElement();
	}
	
	@Override
	public SensorList createSensorList() {
		// TODO Auto-generated method stub
		return super.createSensorList();
	}

	@Override
	public Signal createSignal() {
		return new _Signal();
	}

	@Override
	public SplitratioProfile createSplitratioProfile() {
		return new _SplitRatioProfile();
	}

	@Override
	public SplitRatioProfileSet createSplitRatioProfileSet() {
		return new _SplitRatioProfileSet();
	}
	
}
