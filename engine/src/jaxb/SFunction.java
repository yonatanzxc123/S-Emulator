
package jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType>
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element ref="{}S-Instructions"/>
 *       </sequence>
 *       <attribute name="user-string" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "sInstructions"
})
@XmlRootElement(name = "S-Function")
public class SFunction {

    @XmlElement(name = "S-Instructions", required = true)
    protected SInstructions sInstructions;
    @XmlAttribute(name = "user-string", required = true)
    protected String userString;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    /**
     * Gets the value of the sInstructions property.
     * 
     * @return
     *     possible object is
     *     {@link SInstructions }
     *     
     */
    public SInstructions getSInstructions() {
        return sInstructions;
    }

    /**
     * Sets the value of the sInstructions property.
     * 
     * @param value
     *     allowed object is
     *     {@link SInstructions }
     *     
     */
    public void setSInstructions(SInstructions value) {
        this.sInstructions = value;
    }

    /**
     * Gets the value of the userString property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUserString() {
        return userString;
    }

    /**
     * Sets the value of the userString property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUserString(String value) {
        this.userString = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

}
