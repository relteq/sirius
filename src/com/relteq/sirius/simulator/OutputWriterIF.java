package com.relteq.sirius.simulator;

/**
 * Output writer interface
 */
public interface OutputWriterIF {
	void setRunId(int run_id);
	/**
	 * Opens the output writer
	 */
	void open() throws SiriusException;
	/**
	 * Records the simulator state
	 * @param time
	 * @param exportflows
	 * @param outsteps
	 * @throws SiriusException
	 */
	void recordstate(double time, boolean exportflows, int outsteps) throws SiriusException;
	/**
	 * Closes the output writer
	 */
	void close();
}
