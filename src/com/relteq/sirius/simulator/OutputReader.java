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
	public Vector<Double> mf; // capacity
	public Vector<Double> fv; // free flow speed
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
						if (null != res.scenario) {
							int nvehtypes = res.scenario.getNumVehicleTypes();
							if (nvehtypes <= 0) nvehtypes = 1;
							int nlinks = 0;
							for (Network network : res.scenario.getNetworkList().getNetwork())
								nlinks += network.getLinkList().getLink().size();
							int t_incr = 30;
							int d_incr = t_incr * nvehtypes * nlinks;
							res.t = new Vector<Double>(t_incr, t_incr);
							res.d = new Vector<Double>(d_incr, d_incr);
							res.f = new Vector<Double>(d_incr, d_incr);
							res.mf = new Vector<Double>(d_incr, d_incr);
							res.fv = new Vector<Double>(d_incr, d_incr);
						} else {
							res.t = new Vector<Double>();
							res.d = new Vector<Double>();
							res.f = new Vector<Double>();
							res.mf = new Vector<Double>();
							res.fv = new Vector<Double>();
						}
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
											res.d.addAll(unformat(xmlsr.getAttributeValue(null, "d"), ":"));

											Vector<Double> vect_f = unformat(xmlsr.getAttributeValue(null, "f"), ":");
											for (int iii = 0; iii < vect_f.size(); ++iii) vect_f.set(iii, vect_f.get(iii) * dt);
											res.f.addAll(vect_f);

											res.mf.addAll(unformat(xmlsr.getAttributeValue(null, "mf"), ":"));
											res.fv.addAll(unformat(xmlsr.getAttributeValue(null, "fv"), ":"));
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

	private static Vector<Double> unformat(String str, String delim) {
		if (null == str || 0 == str.length()) return new Vector<Double>(0);
		else {
			String [] parts = str.split(delim);
			Vector<Double> res = new Vector<Double>(parts.length);
			for (int iii = 0; iii < parts.length; ++iii) res.add(Double.valueOf(parts[iii]));
			return res;
		}
	}
}
