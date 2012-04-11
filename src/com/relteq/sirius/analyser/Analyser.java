package com.relteq.sirius.analyser;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import javax.xml.namespace.QName;
import javax.xml.stream.*;

import com.relteq.sirius.simulator.*;

public class Analyser {
	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Arguments: input_file output_file");
			return;
		}
		Analyser analyser = null;
		try {
			analyser = new Analyser(args[0], args[1]);
		} catch (FileNotFoundException exc) {
			exc.printStackTrace();
		}
		if (null != analyser) {
			analyser.Run();
		}
	}
	private _Scenario scenario;
	private XMLStreamReader xmlsr;
	private XMLStreamWriter xmlsw;
	public Analyser(String ifnam, String ofnam) throws FileNotFoundException {
		this(new FileInputStream(ifnam), new FileOutputStream(ofnam));
	}
	public Analyser(InputStream is, OutputStream os) {
		try {
			xmlsr = XMLInputFactory.newInstance().createXMLStreamReader(is);
			xmlsw = XMLOutputFactory.newInstance().createXMLStreamWriter(os, "utf-8");
			xmlsw.writeStartDocument("utf-8", "1.0");
		} catch (XMLStreamException exc) {
			exc.printStackTrace();
		} catch (FactoryConfigurationError exc) {
			exc.printStackTrace();
		}
	}
	public void Run() {
		try {
			xmlsw.writeStartElement("analysis");
			while (xmlsr.hasNext()) {
				if (XMLStreamConstants.START_ELEMENT == xmlsr.getEventType()) {
					if ("scenario" == xmlsr.getName().getLocalPart()) {
						JAXBContext jaxbc;
						try {
							jaxbc = JAXBContext.newInstance(com.relteq.sirius.jaxb.ObjectFactory.class);
							Unmarshaller unmrsh = jaxbc.createUnmarshaller();
							unmrsh.setProperty("com.sun.xml.internal.bind.ObjectFactory", new _JaxbObjectFactory());
							scenario = (_Scenario) unmrsh.unmarshal(xmlsr);
						} catch (JAXBException exc) {
							exc.printStackTrace();
						}
					}else if ("data" == xmlsr.getName().getLocalPart()) {
						xmlsr.next();
						String network_id = null;
						Double dt = .0d;
						while (xmlsr.hasNext() && !(XMLStreamConstants.END_ELEMENT == xmlsr.getEventType() && "data" == xmlsr.getName().getLocalPart())) {
							if (XMLStreamConstants.START_ELEMENT == xmlsr.getEventType()) {
								if (xmlsr.getName().getLocalPart() == "nl") skip_current_element();
								else {
									xmlsw.writeStartElement(xmlsr.getName().getLocalPart());
									if ("net" == xmlsr.getName().getLocalPart()) {
										network_id = xmlsr.getAttributeValue(null, "id");
										String dt_attr = xmlsr.getAttributeValue(null, "dt");
										dt = null == dt_attr ? .0d : Double.valueOf(dt_attr);
									}
									copy_attributes();
									if ("l" == xmlsr.getName().getLocalPart() && dt > 0) {
										Map<String, String> attrs = get_attributes("");
										Vector<Double> f = attrs.containsKey("f") ? unformat(attrs.get("f"), ":") : null;
										Vector<Double> d = attrs.containsKey("d") ? unformat(attrs.get("d"), ":") : null;
										if (null == f && null == d) SiriusErrorLog.addErrorMessage("d and f are undefined");
										else if (null != f && null != d && f.size() != d.size()) SiriusErrorLog.addErrorMessage("size(f) != size(d)");
										else {
											Double v = attrs.containsKey("v") ? Double.valueOf(attrs.get("v")) : null;
											Double fv = attrs.containsKey("fv") ? Double.valueOf(attrs.get("fv")) : null;
											Double mf = attrs.containsKey("mf") ? Double.valueOf(attrs.get("mf")) : null;
											Double cum_f = null != f ? SiriusMath.sum(f) : null;
											Double cum_d = null != d ? SiriusMath.sum(d) : null;
											if (null == v && null != cum_d && cum_d <= 0) v = fv;
											if (null != f && null != d) {
												if (f.size() > 1 && cum_d > 0) {
													double factor = cum_f / cum_d;
													double EPSILON = .01 * cum_f;
													for (int iii = 0; iii < f.size(); ++iii)
														if (Math.abs(factor * d.get(iii) - f.get(iii)) > EPSILON) SiriusErrorLog.addErrorMessage("speeds differ");
												}
											}
											_Link link = null == scenario ? null : scenario.getLinkWithCompositeId(network_id, xmlsr.getAttributeValue(null, "id"));
											Double dx = null != link && null != link.getLength() ? link.getLength().doubleValue() : null;
											Double nlanes = null != link && null != link.getLanes()  ? link.getLanes().doubleValue() : null;
											if (null == v && null != cum_f && null != cum_d && cum_d > 0 && null != dx) {
												v = 3600.0d / dt * cum_f * dx / cum_d;
												if (null != fv && v > fv) v = fv;
											}
											if (null == cum_f && null != cum_d && null != v && null != dx) cum_f = cum_d / dx * v * dt / 3600.0d;
											Double vht = null != cum_d ? cum_d * dt / 3600 : null;
											Double pl = null;
											if (null != mf && null != fv && null != dx) {
												double d_crit = mf / fv * dx;
												if (cum_d <= d_crit) pl = .0d;
												else if (null != nlanes) pl = (1 - (3600.0d / dt * cum_f) / mf) * nlanes * dx * dt;
											}
											if (!attrs.containsKey("v") && null != v) xmlsw.writeAttribute("v", format(v));
											if (null != v && null != dx) xmlsw.writeAttribute("itt", format(dx / v));
											if (null != vht) xmlsw.writeAttribute("vht", format(vht));
											if (null != v && null != vht) {
												Double vmt = vht * v;
												xmlsw.writeAttribute("vmt", format(vmt));
												if (null != fv) xmlsw.writeAttribute("delay", format(vht - vmt / fv));
											}
											if (null != pl) xmlsw.writeAttribute("pl", format(pl));
										}
									}
								}
							} else if (XMLStreamConstants.END_ELEMENT == xmlsr.getEventType()) xmlsw.writeEndElement();
							xmlsr.next();
						}
					}
				}
				xmlsr.next();
			}
			xmlsw.writeEndElement(); // analysis
			xmlsw.close();
		} catch (XMLStreamException exc) {
			exc.printStackTrace();
		}
	}

	private void skip_current_element() throws XMLStreamException {
		int nesting_level = 0;
		while (xmlsr.hasNext()) {
			if (XMLStreamConstants.START_ELEMENT == xmlsr.getEventType()) ++nesting_level;
			else if (XMLStreamConstants.END_ELEMENT == xmlsr.getEventType()) --nesting_level;
			if (nesting_level <= 0) break;
			xmlsr.next();
		}
	}

	private void copy_attributes() throws XMLStreamException {
		int nattr = xmlsr.getAttributeCount();
		for (int iii = 0; iii < nattr; ++iii) {
			QName attrnam = xmlsr.getAttributeName(iii);
			xmlsw.writeAttribute(attrnam.getPrefix(), attrnam.getNamespaceURI(), attrnam.getLocalPart(), xmlsr.getAttributeValue(iii));
		}
	}

	private Map<String, String> get_attributes(String namespaceURI) {
		Map<String, String> res = new TreeMap<String, String>();
		int nattr = xmlsr.getAttributeCount();
		for (int iii = 0; iii < nattr; ++iii) {
			QName attrnam = xmlsr.getAttributeName(iii);
			if (attrnam.getNamespaceURI() == namespaceURI)
				res.put(attrnam.getLocalPart(), xmlsr.getAttributeValue(iii));
		}
		return res;
	}

	private static Vector<Double> unformat(String str, String delim) {
		if (null == str) return null;
		else if (0 == str.length()) return new Vector<Double>(0);
		else {
			String [] parts = str.split(delim);
			Vector<Double> res = new Vector<Double>(parts.length);
			for (int iii = 0; iii < parts.length; ++iii) res.add(Double.valueOf(parts[iii]));
			return res;
		}
	}

	private static final String NUM_FORMAT = "%.4f";
	private static String format(Double num) {
		return String.format(NUM_FORMAT, num);
	}
}
