package com.relteq.sirius.simulator;

/**
 * Output writer interface
 */
public interface OutputWriterIF {
	/**
	 * Opens the output writer
	 * @param run_id the run number
	 * @throws SiriusException
	 */
	void open(int run_id) throws SiriusException;
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
