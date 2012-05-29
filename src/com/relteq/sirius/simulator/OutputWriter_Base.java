package com.relteq.sirius.simulator;

import java.io.FileNotFoundException;

abstract class OutputWriter_Base {

	protected Scenario myScenario;

	protected abstract boolean open(String prefix,String suffix) throws FileNotFoundException;
	protected abstract void recordstate(double time,boolean exportflows,int outsteps) throws SiriusException;
	protected abstract void close();
	protected abstract String format(Double [] V,String delim);
	

	protected static enum Type	{ xml , tabdelim };

}
