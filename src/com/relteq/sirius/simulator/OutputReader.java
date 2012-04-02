package com.relteq.sirius.simulator;

import java.io.*;
import java.util.Vector;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import javax.xml.stream.*;

import com.relteq.sirius.jaxb.Link;
import com.relteq.sirius.jaxb.Network;

class Output {
	public _Scenario scenario;
	public Vector<Double> t; // time
	public Vector<Double> d; // density
	public Vector<Double> f; // flow
	public Vector<Link> getLinks() {
		Vector<Link> res = new Vector<Link>();
		if (null != scenario){
			for (Network network : scenario.getNetworkList().getNetwork())
				for (Link link : network.getLinkList().getLink())
					res.add(link);
		}
		return res;
	}
}

class OutputReader {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.print("Please specify an output file name\n");
			return;
		}
		try {
			long time = System.currentTimeMillis();
			Read(args[0]);
			System.out.print(System.currentTimeMillis() - time + " ms\n");
		} catch (FileNotFoundException exc) {
			exc.printStackTrace();
		}
	}
	
	public static Output Read(String filename) throws FileNotFoundException {
		return Read(new FileInputStream(filename));
	}
	
	public static Output Read(InputStream is) {
		Output res = new Output();
		try {
			XMLStreamReader xmlsr = XMLInputFactory.newInstance().createXMLStreamReader(is);
			while (xmlsr.hasNext()) {
				if (XMLStreamConstants.START_ELEMENT == xmlsr.getEventType()) {
					String localname = xmlsr.getName().getLocalPart();
					if ("scenario" == localname) {
						JAXBContext jaxbc;
						try {
							jaxbc = JAXBContext.newInstance(com.relteq.sirius.jaxb.ObjectFactory.class);
							Unmarshaller unmrsh = jaxbc.createUnmarshaller();
							unmrsh.setProperty("com.sun.xml.internal.bind.ObjectFactory", new _JaxbObjectFactory());
							res.scenario = (_Scenario) unmrsh.unmarshal(xmlsr);
						} catch (JAXBException exc) {
							exc.printStackTrace();
						}
					}else if ("data" == localname) {
						res.t = new Vector<Double>();
						res.d = new Vector<Double>();
						res.f = new Vector<Double>();
						xmlsr.next();
						while (xmlsr.hasNext()) {
							if (XMLStreamConstants.END_ELEMENT == xmlsr.getEventType() && "data" == xmlsr.getName().getLocalPart()) break;
							else if (XMLStreamConstants.START_ELEMENT == xmlsr.getEventType()) {
								if ("ts" == xmlsr.getName().getLocalPart()) {
									res.t.add(Double.valueOf(xmlsr.getAttributeValue(null, "sec")));
								}else if ("net" == xmlsr.getName().getLocalPart()) {
									String dt_attr = xmlsr.getAttributeValue(null, "dt");
									double dt = null == dt_attr ? 1.0f : Double.valueOf(dt_attr);
									xmlsr.next();
									while (xmlsr.hasNext()) {
										if (XMLStreamConstants.END_ELEMENT == xmlsr.getEventType() && "net" == xmlsr.getName().getLocalPart()) break;
										else if (XMLStreamConstants.START_ELEMENT == xmlsr.getEventType() && "l" == xmlsr.getName().getLocalPart()) {
											res.d.add(Double.valueOf(xmlsr.getAttributeValue(null, "d")));
											String f_attr = xmlsr.getAttributeValue(null, "f");
											if (null != f_attr) res.f.add(Double.valueOf(f_attr) / dt);
										}
										xmlsr.next();
									}
								}
							}
							xmlsr.next();
						}
					}
				}
				xmlsr.next();
			}
		} catch (XMLStreamException exc) {
			exc.printStackTrace();
		} catch (FactoryConfigurationError exc) {
			exc.printStackTrace();
		}
		return res;
	}
}
