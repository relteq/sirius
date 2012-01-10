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
 *       &lt;attribute name="bottlenecklink" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="onramplinks" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="sat_den_multiplier" use="required" type="{http://www.w3.org/2001/XMLSchema}decimal" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "zone")
public class Zone {

    @XmlAttribute(name = "bottlenecklink", required = true)
    protected String bottlenecklink;
    @XmlAttribute(name = "onramplinks", required = true)
    protected String onramplinks;
    @XmlAttribute(name = "sat_den_multiplier", required = true)
    protected BigDecimal satDenMultiplier;

    /**
     * Gets the value of the bottlenecklink property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBottlenecklink() {
        return bottlenecklink;
    }

    /**
     * Sets the value of the bottlenecklink property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBottlenecklink(String value) {
        this.bottlenecklink = value;
    }

    /**
     * Gets the value of the onramplinks property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOnramplinks() {
        return onramplinks;
    }

    /**
     * Sets the value of the onramplinks property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOnramplinks(String value) {
        this.onramplinks = value;
    }

    /**
     * Gets the value of the satDenMultiplier property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSatDenMultiplier() {
        return satDenMultiplier;
    }

    /**
     * Sets the value of the satDenMultiplier property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSatDenMultiplier(BigDecimal value) {
        this.satDenMultiplier = value;
    }

}
