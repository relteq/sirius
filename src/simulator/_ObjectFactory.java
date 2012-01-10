package simulator;

import jaxb.Capacity;
import jaxb.Controller;
import jaxb.Demand;
import jaxb.Event;
import jaxb.InitialDensityProfile;
import jaxb.Link;
import jaxb.Network;
import jaxb.Node;
import jaxb.ObjectFactory;
import jaxb.Scenario;
import jaxb.Sensor;
import jaxb.Settings;
import jaxb.Signal;
import jaxb.Splitratios;

public class _ObjectFactory extends ObjectFactory {

	@Override
	public Capacity createCapacity() {
		return new _CapacityProfile();
	}
	
	@Override
	public Controller createController() {
		return new _Controller();
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
	public Link createLink() {
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
