package com.relteq.sirius.db;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Vector;

import javax.xml.bind.JAXBException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

import com.relteq.sirius.simulator.SiriusErrorLog;
import com.relteq.sirius.simulator.SiriusException;

@RunWith(Parameterized.class)
public class ImportExportTest {
	/** scenario file */
	private File conffile;
	/** DB connection parameters */
	private static com.relteq.sirius.db.Parameters params;

	/**
	 * Creates a temporary database and initializes the DB service
	 * @throws SQLException
	 * @throws IOException
	 * @throws SiriusException
	 * @throws ClassNotFoundException
	 */
	@BeforeClass
	public static void createDatabase() throws SQLException, IOException, SiriusException, ClassNotFoundException {
		params = new com.relteq.sirius.db.Parameters();
		params.setDriver("derby");
		params.setDBName("sirius-" + com.relteq.sirius.db.util.UUID.generate());
		System.out.println("Initializing the database");
		com.relteq.sirius.db.Admin.init(params);
		System.out.println("Created a temporary database '" + params.getDBName() + "'");
		params.setCreate(false);
		com.relteq.sirius.db.Service.init(params);
		clearErrors();
	}

	/**
	 * Shuts down the DB service and removes the temporary database
	 */
	@AfterClass
	public static void removeDatabase() {
		com.relteq.sirius.db.Service.shutdown();
		com.relteq.sirius.db.Admin.drop(params);
		clearErrors();
	}

	private static void clearErrors() {
		if (SiriusErrorLog.haserror()) {
			System.out.println("==== ERRORS ====");
			SiriusErrorLog.printErrorMessage();
			System.out.println("================");
			SiriusErrorLog.clearErrorMessage();
		}
	}

	/**
	 * Initializes the testing environment
	 * @param conffile File the configuration file
	 */
	public ImportExportTest(File conffile) {
		this.conffile = conffile;
	}

	/**
	 * Retrieves a list of scenario files
	 * @return
	 */
	@Parameters
	public static Vector<Object[]> conffiles() {
		return com.relteq.sirius.simulator.XMLOutputWriterTest.conffiles();
	}

	/**
	 * Imports and exports a scenario
	 * @throws SiriusException
	 * @throws IOException
	 * @throws JAXBException
	 * @throws SAXException
	 */
	@Test
	public void test() throws SiriusException, IOException, JAXBException, SAXException {
		System.out.println("Importing " + conffile.getPath());
		com.relteq.sirius.om.Scenarios db_scenario = com.relteq.sirius.db.importer.ScenarioLoader.load(conffile.getPath());

		File outfile = File.createTempFile("scenario_", ".xml");
		System.out.println("Exporting " + db_scenario.getId() + " to " + outfile.getPath());
		com.relteq.sirius.db.exporter.ScenarioRestorer.export(db_scenario.getId(), outfile.getPath());
		outfile.delete();

		clearErrors();
	}

}
