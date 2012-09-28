package com.relteq.sirius.simulator;

public class Parameters extends com.relteq.sirius.jaxb.Parameters {
	/**
	 * Tests whether a parameter with the given name exists
	 * @param name
	 * @return true, if such a parameter exists; false, otherwise
	 */
	public boolean has(String name) {
		for (com.relteq.sirius.jaxb.Parameter param : getParameter()) {
			if (name.equals(param.getName())) return true;
		}
		return false;
	}

	/**
	 * Retrieves a value of a parameter with the given name
	 * @param name
	 * @return null, if such a parameter does not exist
	 */
	public String get(String name) {
		java.util.ListIterator<com.relteq.sirius.jaxb.Parameter> iter = getParameter().listIterator(getParameter().size());
		while (iter.hasPrevious()) {
			com.relteq.sirius.jaxb.Parameter param = iter.previous();
			if (name.equals(param.getName())) return param.getValue();
		}
		return null;
	}
}
