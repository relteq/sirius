package com.relteq.sirius.simulator;

abstract class OutputWriterBase implements OutputWriterIF{
	protected Scenario scenario;

	OutputWriterBase(Scenario scenario) {
		this.scenario = scenario;
	}
	/**
	 * @return the scenario
	 */
	public Scenario getScenario() {
		return scenario;
	}

}
