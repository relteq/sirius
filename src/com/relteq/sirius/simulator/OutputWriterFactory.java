package com.relteq.sirius.simulator;

import java.util.Properties;

/**
 *
 */
public class OutputWriterFactory {
	/**
	 * Constructs an output writer of a given type
	 * @param scenario
	 * @param props output writer properties (type, prefix)
	 * @return an output writer
	 * @throws SiriusException
	 */
	public static OutputWriterIF getWriter(Scenario scenario, Properties props) throws SiriusException {
		final String type = props.getProperty("type");
		if (type.equals("xml")) return new XMLOutputWriter(scenario, props);
		else if (type.equals("db")) return new DBOutputWriter(scenario);
		else if (type.equals("text") || type.equals("plaintext")) return new TextOutputWriter(scenario, props);
		else throw new SiriusException("Unknown output writer type '" + type + "'");
	}
}
