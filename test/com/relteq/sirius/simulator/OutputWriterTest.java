package com.relteq.sirius.simulator;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;

import javax.xml.XMLConstants;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stax.StAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

@RunWith(Parameterized.class)
public class OutputWriterTest {
	private static String OUT_PREFIX = "output_";
	private static String OUT_SUFFIX = "_0.xml";
	private static String CONF_SUFFIX = ".xml";

	private File conffile;
	private static Schema ischema;
	private static Schema oschema;

	@BeforeClass public static void loadSchemas() throws SAXException {
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		String schemadir = "data" + File.separator + "schema";
		ischema = factory.newSchema(new File(schemadir + File.separator + "sirius.xsd"));
		oschema = factory.newSchema(new File(schemadir + File.separator + "sirius_output.xsd"));
	}

	public OutputWriterTest(File conffile) {
		this.conffile = conffile;
	}

	@Parameters
	public static Vector<Object[]> conffiles() {
		File confdir = new File("data" + File.separator + "config");
		File [] files = confdir.listFiles();
		Vector<Object[]> res = new Vector<Object[]>(files.length);
		for (File file : files) {
			if (file.getName().endsWith(CONF_SUFFIX))
				res.add(new Object[] {file});
		}
		return res;
	}

	@Test
	public void testOutputWriter() throws IOException, SAXException, XMLStreamException, FactoryConfigurationError {
		System.out.println("CONFIG: " + conffile.getPath());
		validate(conffile, ischema);
		String confname = conffile.getName();
		System.out.println("Config " + confname + " validated");

		String out_prefix = OUT_PREFIX + confname.substring(0, confname.length() - CONF_SUFFIX.length()) + "_";
		File outfile = File.createTempFile(out_prefix, OUT_SUFFIX);
		runSirius(conffile.getPath(), outfile.getAbsolutePath());

		validate(outfile, oschema);
		System.out.println("Output validated");

		outfile.delete();
		System.out.println(outfile.getAbsolutePath() + " removed");
	}

	/** Validate an XML file
	 * @throws FactoryConfigurationError
	 * @throws XMLStreamException
	 * @throws IOException
	 * @throws SAXException
	 * */
	protected static void validate(File xmlfile, Schema schema) throws XMLStreamException, FactoryConfigurationError, SAXException, IOException {
		Validator validator = schema.newValidator();
		XMLStreamReader xmlsr = XMLInputFactory.newInstance().createXMLStreamReader(new FileInputStream(xmlfile));
		validator.validate(new StAXSource(xmlsr));
	}

	/** Run Sirius */
	protected void runSirius(String confpath, String outpath) {
		if (!outpath.endsWith(OUT_SUFFIX)) fail("Incorrect output file path: " + outpath);
		String [] args = {confpath, outpath.substring(0, outpath.length() - OUT_SUFFIX.length()), //
				String.format("%d", 0), String.format("%d", 3600), String.format("%d", 600)};
		System.out.print("ARGS:");
		for (int iii = 0; iii < args.length; ++ iii) System.out.print(" " + args[iii]);
		System.out.println();
		Runner.main(args);
		if (SiriusErrorLog.haserror()) {
			System.out.println("==== ERRORS ====");
			SiriusErrorLog.printErrorMessage();
			System.out.println("================");
			SiriusErrorLog.clearErrorMessage();
		}
	}

}
