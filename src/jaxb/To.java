//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.11.03 at 03:10:39 PM PDT 
//


package jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
 *         &lt;element ref="{}ALatLng"/>
 *       &lt;/all>
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
@XmlRootElement(name = "To")
public class To {

    @XmlElement(name = "ALatLng", required = true)
    protected ALatLng aLatLng;

    /**
     * Gets the value of the aLatLng property.
     * 
     * @return
     *     possible object is
     *     {@link ALatLng }
     *     
     */
    public ALatLng getALatLng() {
        return aLatLng;
    }

    /**
     * Sets the value of the aLatLng property.
     * 
     * @param value
     *     allowed object is
     *     {@link ALatLng }
     *     
     */
    public void setALatLng(ALatLng value) {
        this.aLatLng = value;
    }

}
