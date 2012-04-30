/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;

import javax.xml.namespace.QName;
import javax.xml.stream.*;

final class OutputWriter {

	protected Scenario myScenario;
	protected XMLStreamWriter xmlsw = null;
	protected static final String SEC_FORMAT = "%.1f";
	protected static final String NUM_FORMAT = "%.4f";

	public OutputWriter(Scenario myScenario){
		this.myScenario = myScenario;
	}

	protected boolean open(String prefix,String suffix) throws FileNotFoundException {
		XMLOutputFactory xmlof = XMLOutputFactory.newInstance();
		try {
			xmlsw = xmlof.createXMLStreamWriter(new FileOutputStream(prefix + "_" + suffix + ".xml"), "utf-8");
			xmlsw.writeStartDocument("utf-8", "1.0");
			xmlsw.writeStartElement("scenario_output");
			xmlsw.writeAttribute("schemaVersion", "XXX");
			// scenario
			if (null != myScenario) {
				String conffnam = myScenario.getConfigFilename();
				XMLStreamReader xmlsr = XMLInputFactory.newInstance().createXMLStreamReader(new FileReader(conffnam));
				while (xmlsr.hasNext()) {
					switch (xmlsr.getEventType()) {
					case XMLStreamConstants.START_ELEMENT:
						QName ename = xmlsr.getName();
						xmlsw.writeStartElement(ename.getPrefix(), ename.getLocalPart(), ename.getNamespaceURI());
						for (int iii = 0; iii < xmlsr.getAttributeCount(); ++iii) {
							QName aname = xmlsr.getAttributeName(iii);
							xmlsw.writeAttribute(aname.getPrefix(), aname.getNamespaceURI(), aname.getLocalPart(), xmlsr.getAttributeValue(iii));
						}
						break;
					case XMLStreamConstants.END_ELEMENT:
						xmlsw.writeEndElement();
						break;
					case XMLStreamConstants.CHARACTERS:
						if (!xmlsr.isWhiteSpace()) xmlsw.writeCharacters(xmlsr.getText());
						break;
					case XMLStreamConstants.CDATA:
						if (!xmlsr.isWhiteSpace()) xmlsw.writeCData(xmlsr.getText());
						break;
					}
					xmlsr.next();
				}
				xmlsr.close();
			}
			// report
			xmlsw.writeStartElement("report");
			xmlsw.writeStartElement("settings");
			xmlsw.writeStartElement("units");
			xmlsw.writeCharacters("US");
			xmlsw.writeEndElement(); // units
			xmlsw.writeEndElement(); // settings
			xmlsw.writeStartElement("link_report");
			xmlsw.writeAttribute("density_report", "true");
			xmlsw.writeAttribute("flow_report", "true");
			xmlsw.writeEndElement(); // link_report
			xmlsw.writeStartElement("node_report");
			xmlsw.writeAttribute("srm_report", "true");
			xmlsw.writeEndElement(); // node_report
			xmlsw.writeStartElement("signal_report");
			xmlsw.writeAttribute("cycle_report", "true");
			xmlsw.writeEndElement(); // signal_report
			xmlsw.writeEndElement(); // report
			// data
			xmlsw.writeStartElement("data");
		} catch (XMLStreamException exc) {
			SiriusErrorLog.addErrorMessage(exc.toString());
		}
		return true;
	}

	protected void recordstate(double time,boolean exportflows,int outsteps) throws SiriusException {
		boolean firststep = 0 == myScenario.clock.getCurrentstep();
		double invsteps = firststep ? 1.0d : 1.0d / outsteps;
		String dt = String.format(SEC_FORMAT, firststep ? .0d : myScenario.getSimDtInSeconds() * outsteps);
		try {
			xmlsw.writeStartElement("ts");
			xmlsw.writeAttribute("sec", String.format(SEC_FORMAT, time));
			xmlsw.writeStartElement("netl");
			for (com.relteq.sirius.jaxb.Network network : myScenario.getNetworkList().getNetwork()) {
				xmlsw.writeStartElement("net");
				xmlsw.writeAttribute("id", network.getId());
				// dt = time interval of reporting, sec
				xmlsw.writeAttribute("dt", dt);
				// link list
				xmlsw.writeStartElement("ll");
				for (com.relteq.sirius.jaxb.Link link : network.getLinkList().getLink()) {
					xmlsw.writeStartElement("l");
					xmlsw.writeAttribute("id", link.getId());
					Link _link = (Link) link;
					// d = average number of vehicles during the interval of reporting dt
					xmlsw.writeAttribute("d", format(SiriusMath.times(_link.cumulative_density, invsteps), ":"));
					// f = flow per dt, vehicles
					if (exportflows) xmlsw.writeAttribute("f", format(_link.cumulative_outflow, ":"));
					_link.reset_cumulative();
					// mf = capacity, vehicles per hour
					xmlsw.writeAttribute("mf", String.format(NUM_FORMAT, _link.getCapacityInVPH()));
					// fv = free flow speed, miles per hour
					xmlsw.writeAttribute("fv", String.format(NUM_FORMAT, _link.getVfInMPH()));
					xmlsw.writeEndElement(); // l
				}
				xmlsw.writeEndElement(); // ll
				// node list
				xmlsw.writeStartElement("nl");
				for (com.relteq.sirius.jaxb.Node node : network.getNodeList().getNode()) {
					xmlsw.writeStartElement("n");
					xmlsw.writeAttribute("id", node.getId());
					Node _node = (Node) node;
					Double3DMatrix srm = _node.splitratio;
					for (int ili = 0; ili < _node.getnIn(); ++ili)
						for (int oli = 0; oli < _node.getnOut(); ++oli) {
							xmlsw.writeStartElement("io");
							xmlsw.writeAttribute("il", _node.getInput_link()[ili].getId());
							xmlsw.writeAttribute("ol", _node.getOutput_link()[oli].getId());
							xmlsw.writeAttribute("r", format(srm.getData()[ili][oli], ":"));
							xmlsw.writeEndElement(); // io
						}
					xmlsw.writeEndElement(); // n
				}
				xmlsw.writeEndElement(); // nl
				// signal list
				if (null != network.getSignalList()) {
					xmlsw.writeStartElement("sigl");
					for (com.relteq.sirius.jaxb.Signal signal : network.getSignalList().getSignal()) {
						xmlsw.writeStartElement("sig");
						xmlsw.writeAttribute("id", signal.getId());
						xmlsw.writeEndElement(); // sig
					}
					xmlsw.writeEndElement(); // sigl
				}
				xmlsw.writeEndElement(); // net
			}
			xmlsw.writeEndElement(); // netl
			xmlsw.writeEndElement(); // ts
		} catch (XMLStreamException exc) {
			throw new SiriusException(exc.getMessage());
		}
	}

	protected void close(){
		try {
			xmlsw.writeEndElement(); // data
			xmlsw.writeEndElement(); // scenario_output
			xmlsw.writeEndDocument();
			xmlsw.close();
		} catch (XMLStreamException exc) {
			SiriusErrorLog.addErrorMessage(exc.toString());
		}
	}

	/** Join the elements of an array into a string.
	 * @param V      an array to be serialized
	 * @param delim  the string used to separate the elements of the array
	 * @return a string containing the array values
	 */
	protected String format(Double [] V,String delim){
		if (0 == V.length) return "";
		else if (1 == V.length) return String.format(NUM_FORMAT, V[0]);
		else {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < V.length; ++i){
				if (i > 0) sb.append(delim);
				sb.append(String.format(NUM_FORMAT, V[i]));
			}
			return sb.toString();
		}
	}

}
