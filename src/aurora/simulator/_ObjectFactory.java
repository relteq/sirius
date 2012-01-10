package aurora.simulator;

import aurora.jaxb.*;

public class _ObjectFactory extends ObjectFactory {

	@Override
	public Capacity createCapacity() {
		return new _CapacityProfile();
	}
		
	@Override
	public Demand createDemand() {
		return new _DemandProfile();
	}
	
	@Override
	public Event createEvent() {
		return new _Event();
	}

	@Override
	public InitialDensityProfile createInitialDensityProfile() {
		return new _InitialDensityProfile();
	}

	@Override
	public aurora.jaxb.Link createLink() {
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
	public Sensor createSensor() {
		return new _Sensor();
	}

	@Override
	public Settings createSettings() {
		return new _Settings();
	}

	@Override
	public Signal createSignal() {
		return new _Signal();
	}

	@Override
	public Splitratios createSplitratios() {
		return new _SplitRatiosProfile();
	}

}
