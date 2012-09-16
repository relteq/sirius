/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

public final class JaxbObjectFactory extends com.relteq.sirius.jaxb.ObjectFactory {
	
	@Override
	public com.relteq.sirius.jaxb.CapacityProfile createCapacityProfile() {
		return new CapacityProfile();
	}
	
	@Override
	public com.relteq.sirius.jaxb.ControllerSet createControllerSet() {
		return new ControllerSet();
	}
	
	@Override
	public com.relteq.sirius.jaxb.DemandProfile createDemandProfile() {
		return new DemandProfile();
	}

	@Override
	public com.relteq.sirius.jaxb.DemandProfileSet createDemandProfileSet() {
		return new DemandProfileSet();
	}
	
	@Override
	public com.relteq.sirius.jaxb.FundamentalDiagram createFundamentalDiagram() {
		return new FundamentalDiagram();
	}
	
	@Override
	public com.relteq.sirius.jaxb.FundamentalDiagramProfile createFundamentalDiagramProfile() {
		return new FundamentalDiagramProfile();
	}	
	
	@Override
	public com.relteq.sirius.jaxb.InitialDensitySet createInitialDensitySet() {
		return new InitialDensitySet();
	}

	@Override
	public com.relteq.sirius.jaxb.Link createLink() {
		return new Link();
	}

	@Override
	public com.relteq.sirius.jaxb.Network createNetwork() {
		return new Network();
	}

	@Override
	public com.relteq.sirius.jaxb.Node createNode() {
		return new Node();
	}

	@Override
	public com.relteq.sirius.jaxb.Scenario createScenario() {
		return new Scenario();
	}

	@Override
	public com.relteq.sirius.jaxb.ScenarioElement createScenarioElement() {
		return new ScenarioElement();
	}
	
	@Override
	public com.relteq.sirius.jaxb.Signal createSignal() {
		return new Signal();
	}

	@Override
	public com.relteq.sirius.jaxb.SplitratioProfile createSplitratioProfile() {
		return new SplitRatioProfile();
	}

	@Override
	public com.relteq.sirius.jaxb.SplitRatioProfileSet createSplitRatioProfileSet() {
		return new SplitRatioProfileSet();
	}

	@Override
	public com.relteq.sirius.jaxb.Sensor createSensor() {
		return new Sensor();
	}

	@Override
	public com.relteq.sirius.jaxb.Parameters createParameters() {
		return new Parameters();
	}

	@Override
	public com.relteq.sirius.jaxb.SplitratioEvent createSplitratioEvent() {
		return new SplitratioEvent();
	}

}
