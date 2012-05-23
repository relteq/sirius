package com.relteq.sirius;

import javax.xml.XMLConstants;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 */
public class Version {
	String schemaVersion;
	String engineVersion;

	private Version() {}

	/**
	 * @return the schemaVersion
	 */
	public String getSchemaVersion() {
		return schemaVersion;
	}

	/**
	 * @param schemaVersion the schemaVersion to set
	 */
	public void setSchemaVersion(String schemaVersion) {
		this.schemaVersion = schemaVersion;
	}

	/**
	 * @return the engineVersion
	 */
	public String getEngineVersion() {
		return engineVersion;
	}

	/**
	 * @param engineVersion the engineVersion to set
	 */
	public void setEngineVersion(String engineVersion) {
		this.engineVersion = engineVersion;
	}

	public static Version get() {
		Version version = new Version();
		ClassLoader classLoader = Version.class.getClassLoader();
		// schema version
		try {
			XMLStreamReader xmlsr = XMLInputFactory.newInstance().createXMLStreamReader(classLoader.getResourceAsStream("sirius.xsd"));
			while (xmlsr.hasNext()) {
				if (XMLStreamConstants.START_ELEMENT == xmlsr.getEventType()) {
					javax.xml.namespace.QName qname = xmlsr.getName();
					if ("schema" == qname.getLocalPart() && XMLConstants.W3C_XML_SCHEMA_NS_URI == qname.getNamespaceURI()) {
						version.setSchemaVersion(xmlsr.getAttributeValue(null, "version"));
						break;
					}
				}
				xmlsr.next();
			}
			xmlsr.close();
		} catch (XMLStreamException exc) {
			exc.printStackTrace();
		} catch (FactoryConfigurationError exc) {
			exc.printStackTrace();
		}

		return version;
	}
}
