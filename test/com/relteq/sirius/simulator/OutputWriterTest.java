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

	@Test
	public void testOutputWriter() throws IOException, SAXException, XMLStreamException, FactoryConfigurationError {
		String prefix = "output";
		String suffix = "_0.xml";
		File outfile = File.createTempFile(prefix, suffix);
		String abspath = outfile.getAbsolutePath();
		if (!abspath.endsWith(suffix)) fail("Incorrect temporary file path: " + abspath);
		System.out.println("Output file: " + abspath);
		String [] args = {"data" + File.separator + "config" + File.separator + "test_event.xml", abspath.substring(0, abspath.length() - suffix.length())};
		System.out.println("args: " + args[0] + " " + args[1]);
		Runner.main(args);
		System.out.println("Output done");

		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = factory.newSchema(new File("data" + File.separator + "schema" + File.separator + "sirius_output.xsd"));
		System.out.println("Schema loaded");
		Validator validator = schema.newValidator();

		XMLStreamReader xmlsr = XMLInputFactory.newInstance().createXMLStreamReader(new FileInputStream(abspath));
		validator.validate(new StAXSource(xmlsr));
		System.out.println("Output validated");
		outfile.delete();
		System.out.println(abspath + " removed");
	}

}
