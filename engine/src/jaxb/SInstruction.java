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
 *       <all>
 *         <element ref="{}S-Variable"/>
 *         <element ref="{}S-Instruction-Arguments" minOccurs="0"/>
 *         <element ref="{}S-Label" minOccurs="0"/>
 *       </all>
 *       <attribute name="type" use="required">
 *         <simpleType>
 *           <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             <enumeration value="basic"/>
 *             <enumeration value="synthetic"/>
 *           </restriction>
 *         </simpleType>
 *       </attribute>
 *       <attribute name="name" use="required">
 *         <simpleType>
 *           <restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             <enumeration value="NEUTRAL"/>
 *             <enumeration value="INCREASE"/>
 *             <enumeration value="DECREASE"/>
 *             <enumeration value="JUMP_NOT_ZERO"/>
 *             <enumeration value="ZERO_VARIABLE"/>
 *             <enumeration value="ASSIGNMENT"/>
 *             <enumeration value="GOTO_LABEL"/>
 *             <enumeration value="CONSTANT_ASSIGNMENT"/>
 *             <enumeration value="JUMP_ZERO"/>
 *             <enumeration value="JUMP_EQUAL_CONSTANT"/>
 *             <enumeration value="JUMP_EQUAL_VARIABLE"/>
 *             <enumeration value="QUOTE"/>
 *             <enumeration value="JUMP_EQUAL_FUNCTION"/>
 *           </restriction>
 *         </simpleType>
 *       </attribute>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {

})
@XmlRootElement(name = "S-Instruction")
public class SInstruction {

    @XmlElement(name = "S-Variable", required = true)
    protected String sVariable;
    @XmlElement(name = "S-Instruction-Arguments")
    protected SInstructionArguments sInstructionArguments;
    @XmlElement(name = "S-Label")
    protected String sLabel;
    @XmlAttribute(name = "type", required = true)
    protected String type;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    /**
     * Gets the value of the sVariable property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSVariable() {
        return sVariable;
    }

    /**
     * Sets the value of the sVariable property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSVariable(String value) {
        this.sVariable = value;
    }

    /**
     * Gets the value of the sInstructionArguments property.
     * 
     * @return
     *     possible object is
     *     {@link SInstructionArguments }
     *     
     */
    public SInstructionArguments getSInstructionArguments() {
        return sInstructionArguments;
    }

    /**
     * Sets the value of the sInstructionArguments property.
     * 
     * @param value
     *     allowed object is
     *     {@link SInstructionArguments }
     *     
     */
    public void setSInstructionArguments(SInstructionArguments value) {
        this.sInstructionArguments = value;
    }

    /**
     * Gets the value of the sLabel property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSLabel() {
        return sLabel;
    }

    /**
     * Sets the value of the sLabel property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSLabel(String value) {
        this.sLabel = value;
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
