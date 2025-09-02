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
 *         <element ref="{}S-Functions" minOccurs="0"/>
 *       </sequence>
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
    "sInstructions",
    "sFunctions"
})
@XmlRootElement(name = "S-Program")
public class SProgram {

    @XmlElement(name = "S-Instructions", required = true)
    protected SInstructions sInstructions;
    @XmlElement(name = "S-Functions")
    protected SFunctions sFunctions;
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
     * Gets the value of the sFunctions property.
     * 
     * @return
     *     possible object is
     *     {@link SFunctions }
     *     
     */
    public SFunctions getSFunctions() {
        return sFunctions;
    }

    /**
     * Sets the value of the sFunctions property.
     * 
     * @param value
     *     allowed object is
     *     {@link SFunctions }
     *     
     */
    public void setSFunctions(SFunctions value) {
        this.sFunctions = value;
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
