package com.relteq.sirius.simulator;

import static org.junit.Assert.*;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stax.StAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

public class OutputWriterTest {
	private static String OUT_PREFIX = "output_";
	private static String OUT_SUFFIX = "_0.xml";
	private static String CONF_SUFFIX = ".xml";

	@Test
	public void testOutputWriter() throws IOException, SAXException, XMLStreamException, FactoryConfigurationError {
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		String schemadir = "data" + File.separator + "schema";
		Schema ischema = factory.newSchema(new File(schemadir + File.separator + "sirius_output.xsd"));
		Schema oschema = factory.newSchema(new File(schemadir + File.separator + "sirius_output.xsd"));

		File confdir = new File("data" + File.separator + "config");
		File [] conffile = confdir.listFiles();
		for (int iii = 0; iii < conffile.length; ++iii) {
			String confname = conffile[iii].getName();
			if (confname.endsWith(CONF_SUFFIX)) {
				System.out.println("CONFIG: " + conffile[iii].getPath());
				validate(conffile[iii], ischema);
				System.out.println("Config " + confname + " validated");

				String out_prefix = OUT_PREFIX + confname.substring(0, confname.length() - CONF_SUFFIX.length()) + "_";
				File outfile = File.createTempFile(out_prefix, OUT_SUFFIX);
				runSirius(conffile[iii].getPath(), outfile.getAbsolutePath());

				validate(outfile, oschema);
				System.out.println("Output validated");

				outfile.delete();
				System.out.println(outfile.getAbsolutePath() + " removed");
			}
		}
	}

	/** Validate an XML file
	 * @throws FactoryConfigurationError
	 * @throws XMLStreamException
	 * @throws IOException
	 * @throws SAXException
	 * */
	protected void validate(File xmlfile, Schema schema) throws XMLStreamException, FactoryConfigurationError, SAXException, IOException {
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
	}

}
