package com.relteq.sirius.simulator;

@SuppressWarnings("serial")
public class SiriusException extends Exception {

	public SiriusException(String string){
		super(string);
	}

	public SiriusException(String message, Throwable cause) {
		super(message, cause);
	}

	public SiriusException(Throwable exc) {
		super(exc);
	}
}
