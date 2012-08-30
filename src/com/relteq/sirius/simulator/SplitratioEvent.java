package com.relteq.sirius.simulator;

import com.relteq.sirius.jaxb.Splitratio;
import com.relteq.sirius.jaxb.VehicleType;

public class SplitratioEvent extends com.relteq.sirius.jaxb.SplitratioEvent {

	public VehicleType getVehicleType() {
		for (Object obj : getContent()) {
			if (obj instanceof VehicleType) return (VehicleType) obj;
		}
		return null;
	}

	public Splitratio getSplitratio() {
		for (Object obj : getContent()) {
			if (obj instanceof Splitratio) return (Splitratio) obj;
		}
		return null;
	}
}
