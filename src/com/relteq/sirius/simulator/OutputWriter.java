/*  Copyright (c) 2012, Relteq Systems, Inc. All rights reserved.
	This source is subject to the following copyright notice:
	http://relteq.com/COPYRIGHT_RelteqSystemsInc.txt
*/

package com.relteq.sirius.simulator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import javax.xml.stream.*;

import com.relteq.sirius.jaxb.Link;
import com.relteq.sirius.jaxb.Node;
import com.relteq.sirius.jaxb.Network;

final class OutputWriter {

	protected _Scenario myScenario;
	protected XMLStreamWriter xmlsw = null;
	protected static final String sec_format = "%.1f";
	protected static final String num_format = "%.4f";

	public OutputWriter(_Scenario myScenario){
		this.myScenario = myScenario;
		//this.outsteps = osteps;
	}

	protected boolean open(String prefix,String suffix) throws FileNotFoundException {
		XMLOutputFactory xmlof = XMLOutputFactory.newInstance();
		try {
			xmlsw = xmlof.createXMLStreamWriter(new FileOutputStream(prefix + "_" + suffix + ".xml"), "utf-8");
			xmlsw.writeStartDocument("utf-8", "1.0");
			xmlsw.writeStartElement("scenario_output");
			// scenario
			if (null != myScenario) try {
				JAXBContext jaxbc = JAXBContext.newInstance("com.relteq.sirius.jaxb");
				Marshaller mrsh = jaxbc.createMarshaller();
				mrsh.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
				mrsh.marshal(myScenario, xmlsw);
			} catch (JAXBException exc) {
				SiriusErrorLog.addErrorMessage(exc.toString());
			}
			// report
			xmlsw.writeStartElement("report");
			xmlsw.writeStartElement("link_report");
			xmlsw.writeAttribute("density_report", "true");
			xmlsw.writeAttribute("flow_report", "true");
			xmlsw.writeEndElement(); // link_report
			xmlsw.writeStartElement("node_report");
			xmlsw.writeAttribute("srm_report", "true");
			xmlsw.writeEndElement(); // node_report
			xmlsw.writeEndElement(); // report
			// data
			xmlsw.writeStartElement("data");
		} catch (XMLStreamException exc) {
			SiriusErrorLog.addErrorMessage(exc.toString());
		}
		return true;
	}

	protected void recordstate(double time,boolean exportflows,int outsteps) throws SiriusException {
		double invsteps = (1 == myScenario.clock.getCurrentstep()) ? 1f : 1f / (double) outsteps;
		try {
			xmlsw.writeStartElement("ts");
			xmlsw.writeAttribute("sec", String.format(sec_format, time));
			xmlsw.writeStartElement("netl");
			for (Network network : myScenario.getNetworkList().getNetwork()) {
				xmlsw.writeStartElement("net");
				xmlsw.writeAttribute("id", network.getId());
				xmlsw.writeStartElement("ll");
				for (Link link : network.getLinkList().getLink()) {
					xmlsw.writeStartElement("l");
					xmlsw.writeAttribute("id", link.getId());
					_Link _link = (_Link) link;
					xmlsw.writeAttribute("d", format(SiriusMath.times(_link.cumulative_density, invsteps), ":"));
					if (exportflows) xmlsw.writeAttribute("f", format(SiriusMath.times(_link.cumulative_outflow, invsteps), ":"));
					_link.reset_cumulative();
					xmlsw.writeAttribute("mf", String.format(num_format, _link.getCapacityInVPHPL()));
					xmlsw.writeAttribute("fv", String.format(num_format, _link.getVfInMPH()));
					xmlsw.writeEndElement(); // l
				}
				xmlsw.writeEndElement(); // ll
				xmlsw.writeStartElement("nl");
				for (Node node : network.getNodeList().getNode()) {
					xmlsw.writeStartElement("n");
					xmlsw.writeAttribute("id", node.getId());
					_Node _node = (_Node) node;
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

	protected String format(Double [] V,String delim){
		if (0 == V.length) return "";
		else if (1 == V.length) return String.format(num_format, V[0]);
		else {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < V.length; ++i){
				if (i > 0) sb.append(delim);
				sb.append(String.format(num_format, V[i]));
			}
			return sb.toString();
		}
	}

}
