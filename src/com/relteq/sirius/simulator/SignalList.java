/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

public class SignalList extends com.relteq.sirius.jaxb.SignalList {

	protected ArrayList<Signal> signals = new ArrayList<Signal>();
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(Scenario myScenario,Network myNetwork) {
		if(myNetwork.getSignalList()!=null){
			for(com.relteq.sirius.jaxb.Signal signal : myNetwork.getSignalList().getSignal()){
				((Signal) signal).populate(myScenario,myNetwork);
				signals.add((Signal) signal);
			}
		}
	}

	protected boolean validate() {
		for(Signal signal : signals)
			if(!signal.validate()){
				SiriusErrorLog.addErrorMessage("Signal validation failure, signal " + signal.getId());
				return false;
			}
		return true;
	}

	protected void reset() throws SiriusException {
		for(Signal signal : signals)
			signal.reset();
	}

	protected void update() throws SiriusException {
    	for(Signal signal : signals)
    		signal.update();
	}
	
}
