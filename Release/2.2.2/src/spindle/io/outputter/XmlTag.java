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
package spindle.io.outputter;

/**
 * Tags used in XML theory parser and outputter.
 * 
 * @deprecated As of version 2.2.2, the XmlTag interface is depreacted as the associated XML theory parser ({@link spindle.io.parser.XmlTheoryParser}) 
 * and outputter ({@link spindle.io.outputter.XmlTheoryOutputter})  classes are deprecated.
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2013.05.30
 * @see spindle.io.parser.XmlTheoryParser
 * @see spindle.io.parser.XmlTheoryParser2
 * @see spindle.io.outputter.XmlTheoryOutputter
 * @see spindle.io.outputter.XmlTheoryOutputter2
 */
@Deprecated
public interface XmlTag {
	/**
	 * XML tags used in XML theory parser.
	 * 
	 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
	 * @since version 1.0.0
	 * @deprecated As of version 2.2.2, the XmlTag.Tag enumeration is depreacted as the associated XML theory parser ({@link spindle.io.parser.XmlTheoryParser}) 
	 * and outputter ({@link spindle.io.outputter.XmlTheoryOutputter})  classes are deprecated.
	 */
	@Deprecated 
	enum Tag {
		DOC_ROOT("theory"), //
		LITERAL_VARIABLE("literalVariable"), LITERAL_VARIABLE_NAME("name"), LITERAL_VARIABLE_VALUE("value"), //
		LITERAL_BOOLEAN_FUNCTION("literalBooleanFunction"), LITERAL_BOOLEAN_FUNCTION_FORMULA("formula"), //
		AND("and"), NOT("not"), //
		FACT("fact"), //
		SUPERIORITY("sup"), //
		RULE("rule"), //
		LITERAL("literal"), //
		ATOM("atom"), //
		HEAD("head"), //
		BODY("body"), //
		MODE("mode"), //
		PREDICATE("predicate"), //
		MODE_CONVERSION("conversion"), //
		MODE_CONVERSION_FROM("conversionFrom"), MODE_CONVERSION_TO("conversionTo"), //
		MODE_CONFLICT("conflict"), //
		MODE_CONFLICT_MODE("conflictMode"), MODE_CONFLICT_WITH("conflictWith"), //
		CONCLUSION("conclusion");//

		private final String xmlTag;

		Tag(String _xmlTag) {
			xmlTag = _xmlTag;
		}

		public String getXmlTag() {
			return xmlTag;
		}
	}

	/**
	 * XML attributed values used in XML theory parser.
	 * 
	 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
	 * @since version 1.0.0
	 * @deprecated As of version 2.2.2, the XmlTag.Attribute enumeration is depreacted as the associated XML theory parser ({@link spindle.io.parser.XmlTheoryParser}) 
	 * and outputter ({@link spindle.io.outputter.XmlTheoryOutputter})  classes are deprecated.
	 */
	@Deprecated
	enum Attribute {
		RULE_TYPE_STRICT_RULE("strength", "STRICT"), //
		RULE_TYPE_DEFEASIBLE_RULE("strength", "DEFEASIBLE"), //
		RULE_TYPE_DEFEATER("strength", "DEFEATER"), //
		RULE_LABEL("label"), //
		SUPERIORITY_SUPERIOR("superior"), //
		SUPERIORITY_INFERIOR("inferior"), //
		CONCLUSION_TYPE("type"); //

		private String xmlAttribute;
		private String attributeValue;

		Attribute(String _xmlAttribute) {
			this(_xmlAttribute, "");
		}

		Attribute(String _xmlAttribute, String _attributeValue) {
			xmlAttribute = _xmlAttribute;
			attributeValue = _attributeValue;
		}

		public String getAttributeName() {
			return xmlAttribute;
		}

		public String getAttributeValue() {
			return attributeValue;
		}
	}
}
