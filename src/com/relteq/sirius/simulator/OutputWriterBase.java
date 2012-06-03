package com.relteq.sirius.simulator;

abstract class OutputWriterBase implements OutputWriterIF{
	protected Scenario scenario;
	protected int run_id;

	OutputWriterBase(Scenario scenario) {
		this.scenario = scenario;
	}
	/**
	 * @return the scenario
	 */
	public Scenario getScenario() {
		return scenario;
	}
	/**
	 * @return the run id
	 */
	public int getRunId() {
		return run_id;
	}
	/**
	 * @param run_id the run id to set
	 */
	@Override
	public void setRunId(int run_id) {
		this.run_id = run_id;
	}

}
