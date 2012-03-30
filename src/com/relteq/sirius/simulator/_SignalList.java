/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.util.ArrayList;

public class _SignalList extends com.relteq.sirius.jaxb.SignalList {

	protected ArrayList<_Signal> _signals = new ArrayList<_Signal>();
	
	/////////////////////////////////////////////////////////////////////
	// populate / reset / validate / update
	/////////////////////////////////////////////////////////////////////
	
	protected void populate(_Scenario myScenario,_Network myNetwork) {
		if(myNetwork.getSignalList()!=null){
			for(com.relteq.sirius.jaxb.Signal signal : myNetwork.getSignalList().getSignal()){
				((_Signal) signal).populate(myScenario,myNetwork);
				_signals.add((_Signal) signal);
			}
		}
	}

	protected boolean validate() {
		for(_Signal signal : _signals)
			if(!signal.validate()){
				SiriusErrorLog.addErrorMessage("Signal validation failure, signal " + signal.getId());
				return false;
			}
		return true;
	}

	protected void reset() throws SiriusException {
		for(_Signal signal : _signals)
			signal.reset();
	}

	protected void update() throws SiriusException {
    	for(_Signal signal : _signals)
    		signal.update();
	}
	
}
