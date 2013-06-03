/**
 * SPINdle (version 2.2.2)
 * Copyright (C) 2009-2012 NICTA Ltd.
 *
 * This file is part of SPINdle project.
 * 
 * SPINdle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * SPINdle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with SPINdle.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory 
 */
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.05.30 at 12:44:15 PM EST 
//


package spindle.io.xjc.dom2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ctConclusion complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ctConclusion">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="tag">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;enumeration value="DEFINITE_PROVABLE"/>
 *               &lt;enumeration value="NOT_DEFINITE_PROVABLE"/>
 *               &lt;enumeration value="DEFEASIBLE_PROVABLE"/>
 *               &lt;enumeration value="NOT_DEFEASIBLE_PROVABLE"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="literal" type="{http://spin.nicta.org.au/spindle/spindleDefeasibleTheory2.xsd}ctLiteral"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ctConclusion", namespace = "http://spin.nicta.org.au/spindle/spindleDefeasibleTheory2.xsd", propOrder = {
    "tag",
    "literal"
})
public class CtConclusion {

    @XmlElement(namespace = "http://spin.nicta.org.au/spindle/spindleDefeasibleTheory2.xsd", required = true)
    protected String tag;
    @XmlElement(namespace = "http://spin.nicta.org.au/spindle/spindleDefeasibleTheory2.xsd", required = true)
    protected CtLiteral literal;

    /**
     * Gets the value of the tag property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTag() {
        return tag;
    }

    /**
     * Sets the value of the tag property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTag(String value) {
        this.tag = value;
    }

    /**
     * Gets the value of the literal property.
     * 
     * @return
     *     possible object is
     *     {@link CtLiteral }
     *     
     */
    public CtLiteral getLiteral() {
        return literal;
    }

    /**
     * Sets the value of the literal property.
     * 
     * @param value
     *     allowed object is
     *     {@link CtLiteral }
     *     
     */
    public void setLiteral(CtLiteral value) {
        this.literal = value;
    }

}