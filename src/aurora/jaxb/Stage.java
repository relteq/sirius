//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.11.23 at 08:38:57 AM PST 
//


package aurora.jaxb;

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
 *       &lt;attribute name="greentime" use="required" type="{http://www.w3.org/2001/XMLSchema}decimal" />
 *       &lt;attribute name="movA" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="1|2|3|4|5|6|7|8"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="movB" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;pattern value="1|2|3|4|5|6|7|8"/>
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
@XmlType(name = "")
@XmlRootElement(name = "stage")
public class Stage {

    @XmlAttribute(name = "greentime", required = true)
    protected BigDecimal greentime;
    @XmlAttribute(name = "movA", required = true)
    protected String movA;
    @XmlAttribute(name = "movB", required = true)
    protected String movB;

    /**
     * Gets the value of the greentime property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getGreentime() {
        return greentime;
    }

    /**
     * Sets the value of the greentime property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setGreentime(BigDecimal value) {
        this.greentime = value;
    }

    /**
     * Gets the value of the movA property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMovA() {
        return movA;
    }

    /**
     * Sets the value of the movA property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMovA(String value) {
        this.movA = value;
    }

    /**
     * Gets the value of the movB property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMovB() {
        return movB;
    }

    /**
     * Sets the value of the movB property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMovB(String value) {
        this.movB = value;
    }

}
