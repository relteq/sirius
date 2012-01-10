//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.11.03 at 03:10:39 PM PDT 
//


package jaxb;

import java.math.BigDecimal;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element ref="{}demand" minOccurs="0"/>
 *         &lt;element ref="{}description" minOccurs="0"/>
 *         &lt;element ref="{}fd" minOccurs="0"/>
 *         &lt;element ref="{}srm" minOccurs="0"/>
 *         &lt;element ref="{}qmax" minOccurs="0"/>
 *         &lt;element ref="{}lkid" minOccurs="0"/>
 *         &lt;element ref="{}controller" minOccurs="0"/>
 *         &lt;element ref="{}wfm" minOccurs="0"/>
 *         &lt;element ref="{}control" minOccurs="0"/>
 *         &lt;element ref="{}display_position" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="tstamp" use="required" type="{http://www.w3.org/2001/XMLSchema}decimal" />
 *       &lt;attribute name="node_id" type="{http://www.w3.org/2001/XMLSchema}string" default="" />
 *       &lt;attribute name="link_id" type="{http://www.w3.org/2001/XMLSchema}string" default="" />
 *       &lt;attribute name="network_id" type="{http://www.w3.org/2001/XMLSchema}string" default="" />
 *       &lt;attribute name="enabled" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="type" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="FD|DEMAND|QLIM|SRM|WFM|SCONTROL|NCONTROL|CCONTROL|TCONTROL|MONITOR"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {

})
@XmlRootElement(name = "event")
public class Event {

    protected Demand demand;
    protected String description;
    protected Fd fd;
    protected Srm srm;
    protected BigDecimal qmax;
    protected String lkid;
    protected Controller controller;
    protected Wfm wfm;
    protected Control control;
    @XmlElement(name = "display_position")
    protected DisplayPosition displayPosition;
    @XmlAttribute(name = "tstamp", required = true)
    protected BigDecimal tstamp;
    @XmlAttribute(name = "node_id")
    protected String nodeId;
    @XmlAttribute(name = "link_id")
    protected String linkId;
    @XmlAttribute(name = "network_id")
    protected String networkId;
    @XmlAttribute(name = "enabled", required = true)
    protected boolean enabled;
    @XmlAttribute(name = "type", required = true)
    protected String type;

    /**
     * Gets the value of the demand property.
     * 
     * @return
     *     possible object is
     *     {@link Demand }
     *     
     */
    public Demand getDemand() {
        return demand;
    }

    /**
     * Sets the value of the demand property.
     * 
     * @param value
     *     allowed object is
     *     {@link Demand }
     *     
     */
    public void setDemand(Demand value) {
        this.demand = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the fd property.
     * 
     * @return
     *     possible object is
     *     {@link Fd }
     *     
     */
    public Fd getFd() {
        return fd;
    }

    /**
     * Sets the value of the fd property.
     * 
     * @param value
     *     allowed object is
     *     {@link Fd }
     *     
     */
    public void setFd(Fd value) {
        this.fd = value;
    }

    /**
     * Gets the value of the srm property.
     * 
     * @return
     *     possible object is
     *     {@link Srm }
     *     
     */
    public Srm getSrm() {
        return srm;
    }

    /**
     * Sets the value of the srm property.
     * 
     * @param value
     *     allowed object is
     *     {@link Srm }
     *     
     */
    public void setSrm(Srm value) {
        this.srm = value;
    }

    /**
     * Gets the value of the qmax property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getQmax() {
        return qmax;
    }

    /**
     * Sets the value of the qmax property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setQmax(BigDecimal value) {
        this.qmax = value;
    }

    /**
     * Gets the value of the lkid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLkid() {
        return lkid;
    }

    /**
     * Sets the value of the lkid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLkid(String value) {
        this.lkid = value;
    }

    /**
     * Gets the value of the controller property.
     * 
     * @return
     *     possible object is
     *     {@link Controller }
     *     
     */
    public Controller getController() {
        return controller;
    }

    /**
     * Sets the value of the controller property.
     * 
     * @param value
     *     allowed object is
     *     {@link Controller }
     *     
     */
    public void setController(Controller value) {
        this.controller = value;
    }

    /**
     * Gets the value of the wfm property.
     * 
     * @return
     *     possible object is
     *     {@link Wfm }
     *     
     */
    public Wfm getWfm() {
        return wfm;
    }

    /**
     * Sets the value of the wfm property.
     * 
     * @param value
     *     allowed object is
     *     {@link Wfm }
     *     
     */
    public void setWfm(Wfm value) {
        this.wfm = value;
    }

    /**
     * Gets the value of the control property.
     * 
     * @return
     *     possible object is
     *     {@link Control }
     *     
     */
    public Control getControl() {
        return control;
    }

    /**
     * Sets the value of the control property.
     * 
     * @param value
     *     allowed object is
     *     {@link Control }
     *     
     */
    public void setControl(Control value) {
        this.control = value;
    }

    /**
     * Gets the value of the displayPosition property.
     * 
     * @return
     *     possible object is
     *     {@link DisplayPosition }
     *     
     */
    public DisplayPosition getDisplayPosition() {
        return displayPosition;
    }

    /**
     * Sets the value of the displayPosition property.
     * 
     * @param value
     *     allowed object is
     *     {@link DisplayPosition }
     *     
     */
    public void setDisplayPosition(DisplayPosition value) {
        this.displayPosition = value;
    }

    /**
     * Gets the value of the tstamp property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getTstamp() {
        return tstamp;
    }

    /**
     * Sets the value of the tstamp property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setTstamp(BigDecimal value) {
        this.tstamp = value;
    }

    /**
     * Gets the value of the nodeId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNodeId() {
        if (nodeId == null) {
            return "";
        } else {
            return nodeId;
        }
    }

    /**
     * Sets the value of the nodeId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNodeId(String value) {
        this.nodeId = value;
    }

    /**
     * Gets the value of the linkId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLinkId() {
        if (linkId == null) {
            return "";
        } else {
            return linkId;
        }
    }

    /**
     * Sets the value of the linkId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLinkId(String value) {
        this.linkId = value;
    }

    /**
     * Gets the value of the networkId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNetworkId() {
        if (networkId == null) {
            return "";
        } else {
            return networkId;
        }
    }

    /**
     * Sets the value of the networkId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNetworkId(String value) {
        this.networkId = value;
    }

    /**
     * Gets the value of the enabled property.
     * 
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the value of the enabled property.
     * 
     */
    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

}
