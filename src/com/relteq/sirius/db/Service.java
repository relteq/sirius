package com.relteq.sirius.db;

import org.apache.torque.Torque;
import org.apache.torque.TorqueException;

import com.relteq.sirius.simulator.SiriusException;

/**
 * DB service initialization and shutdown
 */
public class Service {
	/**
	 * Initializes the DB service.
	 * Connection parameters are read from the environment,
	 * as described in the "Concept of Operations"
	 * @throws SiriusException
	 */
	public static void init() throws SiriusException {
		init(Parameters.fromEnvironment());
	}

	/**
	 * Initializes the DB service for the specified parameters
	 * @param params
	 * @throws SiriusException
	 */
	public static void init(Parameters params) throws SiriusException {
		try {
			Torque.init(params.toConfiguration());
		} catch (TorqueException exc) {
			throw new SiriusException(exc.getMessage(), exc);
		}
	}

	/**
	 * @return true if the DB service is already initialized
	 */
	public static boolean isInit() {
		return Torque.isInit();
	}

	/**
	 * Shuts down the DB service
	 * @throws SiriusException
	 */
	public static void shutdown() {
		try {
			Torque.shutdown();
		} catch (TorqueException exc) {
			exc.printStackTrace();
		}
	}
}
