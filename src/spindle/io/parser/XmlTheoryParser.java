/**
 * SPINdle (version 2.2.0)
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
package spindle.io.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.DomUtilities;
import spindle.core.dom.Literal;
import spindle.core.dom.LiteralVariable;
import spindle.core.dom.Mode;
import spindle.core.dom.Rule;
import spindle.core.dom.RuleType;
import spindle.core.dom.Superiority;
import spindle.core.dom.Theory;
import spindle.core.dom.TheoryException;
import spindle.io.ParserException;
import spindle.io.outputter.DflTheoryConst;
import spindle.io.outputter.XmlTheoryOutputter;
import spindle.io.outputter.XmlTag.Attribute;
import spindle.io.outputter.XmlTag.Tag;
import spindle.sys.AppLogger;
import spindle.sys.message.ErrorMessage;

/**
 * Defeasible theory and conclusions parser for theory represented using XML.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 */
public class XmlTheoryParser extends AbstractTheoryParser {
	public static final String PARSER_TYPE = XmlTheoryOutputter.OUTPUTTER_TYPE;
	private static XmlTheoryParser INSTANCE = null;

	public static Theory getTheory(String xmlString, AppLogger logger) throws ParserException {
		if (null == INSTANCE) INSTANCE = new XmlTheoryParser();
		try {
			InputStream ins = new ByteArrayInputStream(xmlString.replaceAll("[\t\r\n]", "").getBytes());
			if (null != logger) INSTANCE.setAppLogger(logger);
			return INSTANCE.getTheory(ins);
		} catch (Exception e) {
			throw new ParserException(e);
		} finally {
			INSTANCE.resetAppLogger();
		}
	}

	public static Map<Literal, Map<ConclusionType, Conclusion>> getConclusions(String xmlString, AppLogger logger) throws ParserException {
		if (null == INSTANCE) INSTANCE = new XmlTheoryParser();
		try {
			InputStream ins = new ByteArrayInputStream(xmlString.replaceAll("[\t\r\n]", "").getBytes());
			if (null != logger) INSTANCE.setAppLogger(logger);
			return INSTANCE.getConclusions(ins);
		} catch (Exception e) {
			throw new ParserException(e);
		} finally {
			INSTANCE.resetAppLogger();
		}
	}

	private XMLStreamReader reader = null;
	private Tag currTag = null;
	private int ruleCounter = 0;

	public XmlTheoryParser() {
		super(PARSER_TYPE);
	}

	@Override
	protected void generateTheory(InputStream ins) throws ParserException {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		inputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);

		try {
			reader = inputFactory.createXMLStreamReader(ins);
			while (reader.hasNext()) {
				int event = reader.next();
				switch (event) {
				case XMLStreamConstants.START_DOCUMENT:
					break;
				case XMLStreamConstants.START_ELEMENT:
					currTag = getXmlTag(reader.getLocalName());
					if (null == currTag) throw new ParserException("unknown element found in the theory file: " + reader.getLocalName());

					switch (currTag) {
					case DOC_ROOT:
						break;
					case LITERAL_VARIABLE:
						extractLiteralVariable(Tag.LITERAL_VARIABLE);
						break;
					case LITERAL_BOOLEAN_FUNCTION:
						extractLiteralVariable(Tag.LITERAL_BOOLEAN_FUNCTION);
						break;
					case FACT:
						Rule fact = extractFact();
						if (fact.getLabel().startsWith(TEMP_RULE_LABEL)) addPendingRule(fact);
						else addRule(fact);
						break;
					case RULE:
						Rule rule = extractRule();
						if (rule.getLabel().startsWith(TEMP_RULE_LABEL)) addPendingRule(rule);
						else addRule(rule);
						break;
					case SUPERIORITY:
						Superiority superiority = extractSuperiority();
						addSuperiority(superiority);
						break;
					case MODE_CONVERSION:
						extractModeConversionRule();
						break;
					case MODE_CONFLICT:
						extractModeConflictRule();
						break;
					default:
					}
					break;
				case XMLStreamConstants.CHARACTERS:
					break;
				case XMLStreamConstants.END_ELEMENT:
					break;
				case XMLStreamConstants.END_DOCUMENT:
					break;
				case XMLStreamConstants.COMMENT:
				}
			}
		} catch (ParserException e) {
			throw e;
		} catch (Exception e) {
			throw new ParserException("exception throw while parsing document", e);
		} finally {
			if (null != reader) {
				try {
					reader.close();
				} catch (XMLStreamException e) {
				}
				reader = null;
			}
		}
	}

	private void extractLiteralVariable(Tag elementType) throws ParserException {
		LiteralVariable literalVariableName = null;
		LiteralVariable literalVariableValue = null;
		LiteralVariable currLiteralVariable = null;
		try {
			while (reader.hasNext()) {
				int event = reader.next();
				switch (event) {
				case XMLStreamConstants.START_ELEMENT:
					currTag = getXmlTag(reader.getLocalName());
					if (Tag.LITERAL_VARIABLE_NAME.equals(currTag)) {
						// currentTag=Tag.LITERAL_VARIABLE_NAME;
					} else if (Tag.LITERAL_VARIABLE_VALUE.equals(currTag)) {
						// currentTag=Tag.LITERAL_BOOLEAN_FUNCTION;
					} else if (Tag.LITERAL_BOOLEAN_FUNCTION_FORMULA.equals(currTag)) {
						currLiteralVariable = DomUtilities.getLiteralVariable(extractLiteral(Tag.LITERAL_BOOLEAN_FUNCTION_FORMULA));
						// currentTag=Tag.LITERAL_BOOLEAN_FUNCTION_FORMULA;
					} else if (Tag.LITERAL.equals(currTag)) {
						currLiteralVariable = DomUtilities.getLiteralVariable(extractLiteral(Tag.LITERAL));
					} else {
						throw new ParserException("Illegal element appeared in rule: " + currTag + ":" + elementType + "::" + literalVariableName + ":::"
								+ literalVariableValue);
					}
					break;
				case XMLStreamConstants.END_ELEMENT:
					currTag = getXmlTag(reader.getLocalName());
					if (Tag.LITERAL_VARIABLE_NAME.equals(currTag)) {
						literalVariableName = currLiteralVariable;
					} else if (Tag.LITERAL_VARIABLE_VALUE.equals(currTag)) {
						literalVariableValue = currLiteralVariable;
					} else if (Tag.LITERAL_BOOLEAN_FUNCTION_FORMULA.equals(currTag)) {
						literalVariableValue = currLiteralVariable;
					} else if (elementType.equals(currTag)) {
						if (null == literalVariableName) throw new ParserException("literal variable name not defined");
						if (null == literalVariableValue) throw new ParserException("literal variable name not defined");
						addLiteralVariable(literalVariableName, literalVariableValue);
						return;
					} else throw new ParserException(ErrorMessage.IO_UNEXPECTED_END_OF_FILE);
				}
			}
		} catch (ParserException e) {
			throw e;
		} catch (Exception e) {
			throw new ParserException(e);
		}
		throw new ParserException(ErrorMessage.IO_UNEXPECTED_END_OF_FILE);
	}

	private Rule extractFact() throws ParserException {
		return extractRule(RuleType.FACT);
	}

	private Rule extractRule() throws ParserException {
		RuleType ruleType = null;

		for (int i = 0; i < reader.getAttributeCount() && null == ruleType; i++) {
			String attributeName = reader.getAttributeLocalName(i);
			String attributeValue = reader.getAttributeValue(i);
			if (Attribute.RULE_TYPE_STRICT_RULE.getAttributeName().equals(attributeName)) {
				ruleType = getRuleType_xml(attributeValue);
			}
		}
		return extractRule(ruleType);
	}

	private Rule extractRule(RuleType ruleType) throws ParserException {
		String ruleLabel = "";

		for (int i = 0; i < reader.getAttributeCount(); i++) {
			String attributeName = reader.getAttributeLocalName(i);
			String attributeValue = reader.getAttributeValue(i);
			if (Attribute.RULE_LABEL.getAttributeName().equals(attributeName)) {
				ruleLabel = attributeValue;
			}
		}
		if ("".equals(ruleLabel)) ruleLabel = TEMP_RULE_LABEL + Theory.formatter.format(ruleCounter++);

		Rule rule = DomUtilities.getRule(ruleLabel, ruleType);
		List<Literal> bodyLiterals = null;
		List<Literal> headLiterals = null;
		Mode mode = null;

		try {
			while (reader.hasNext()) {
				int event = reader.next();
				switch (event) {
				case XMLStreamConstants.START_ELEMENT:
					currTag = getXmlTag(reader.getLocalName());
					if (Tag.BODY.equals(currTag)) {
						bodyLiterals = extractLiteralList(reader.getLocalName());
					} else if (Tag.HEAD.equals(currTag)) {
						headLiterals = extractLiteralList(reader.getLocalName());
					} else if (Tag.LITERAL.equals(currTag) && RuleType.FACT.equals(ruleType)) {
						Literal literal = extractLiteral(Tag.LITERAL);
						headLiterals = new ArrayList<Literal>();
						headLiterals.add(literal);
					} else if (Tag.MODE.equals(currTag) || Tag.NOT.equals(currTag)) {
						mode = extractRuleMode();
					} else {
						throw new ParserException("Illegal element appeared in rule: " + reader.getLocalName());
					}
					break;
				case XMLStreamConstants.END_ELEMENT:
					currTag = getXmlTag(reader.getLocalName());
					if (Tag.RULE.equals(currTag) || Tag.FACT.equals(currTag)) {

						if (null == headLiterals || headLiterals.size() == 0) throw new ParserException("rule [" + ruleLabel + "] contains no heads");

						for (Literal literal : headLiterals) {
							rule.addHeadLiteral(literal);
						}

						if (null != bodyLiterals) {
							for (Literal literal : bodyLiterals) {
								rule.addBodyLiteral(literal);
							}
						}

						if (null != mode) rule.setMode(mode);

						return rule;
					}
					break;
				}
			}
		} catch (Exception e) {
			throw new ParserException(e);
		}
		throw new ParserException("unexpected end of document");
	}

	private List<Literal> extractLiteralList(String endElement) throws XMLStreamException, ParserException {
		List<Literal> literals = new ArrayList<Literal>();
		while (reader.hasNext()) {
			int event = reader.next();
			switch (event) {
			case XMLStreamConstants.START_ELEMENT:
				currTag = getXmlTag(reader.getLocalName());
				if (Tag.AND.equals(currTag)) {
				} else if (Tag.LITERAL.equals(currTag)) {
					Literal literal = extractLiteral(Tag.LITERAL);
					literals.add(literal);
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				if (endElement.equals(reader.getLocalName())) return literals;
			}
		}
		throw new ParserException(ErrorMessage.IO_UNEXPECTED_END_OF_FILE);
	}

	private Mode extractRuleMode() throws XMLStreamException, ParserException {
		String modeName = "";
		boolean isModeNegation = false;

		Tag startTag = currTag;

		if (Tag.NOT.equals(currTag)) isModeNegation = true;
		do {
			int event = reader.next();
			switch (event) {
			case XMLStreamConstants.START_ELEMENT:
				currTag = getXmlTag(reader.getLocalName());
				break;
			case XMLStreamConstants.END_ELEMENT:
				if (startTag.equals(currTag)) return new Mode(modeName.trim(), isModeNegation);
				break;
			case XMLStreamConstants.CHARACTERS:
				if (Tag.MODE.equals(currTag)) {
					modeName += reader.getText();
				}
				break;
			}
		} while (reader.hasNext());
		throw new ParserException(ErrorMessage.IO_UNEXPECTED_END_OF_FILE);
	}

	private Literal extractLiteral(Tag xmlRootTag) throws XMLStreamException, ParserException {
		String literalName = "";
		boolean isNegation = false;
		boolean isModeNegation = false;
		String modeName = "";
		String predicates = "";

		boolean currTagNegation = false;

		while (reader.hasNext()) {
			int event = reader.next();
			switch (event) {
			case XMLStreamConstants.START_ELEMENT:
				currTag = getXmlTag(reader.getLocalName());
				if (Tag.ATOM.equals(currTag)) {
					isNegation = currTagNegation;
				} else if (Tag.MODE.equals(currTag)) {
					isModeNegation = currTagNegation;
				} else if (Tag.NOT.equals(currTag)) {
					currTagNegation = true;
				} else if (Tag.PREDICATE.equals(currTag)) {
				} else {
					throw new ParserException("Illegal element appeared in theory file: " + reader.getLocalName());
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				currTag = getXmlTag(reader.getLocalName());
				if (Tag.NOT.equals(currTag)) {
					currTagNegation = false;
				} else if (Tag.PREDICATE.equals(currTag)) {
					// System.out.println("predicate=" + predicates);
				} else if (xmlRootTag.equals(currTag)) {
					Literal literal = DomUtilities.getLiteral(literalName, isNegation, //
							modeName, isModeNegation,//
							null,
							predicates.split("" + DflTheoryConst.LITERAL_SEPARATOR));
					return literal;
				}
				break;
			case XMLStreamConstants.CHARACTERS:
				if (Tag.ATOM.equals(currTag)) {
					literalName += reader.getText();
					literalName = literalName.trim();
				} else if (Tag.MODE.equals(currTag)) {
					modeName += reader.getText();
					modeName = modeName.trim();
				} else if (Tag.PREDICATE.equals(currTag)) {
					predicates += reader.getText();
				}
				break;
			}
		}
		throw new ParserException(ErrorMessage.IO_UNEXPECTED_END_OF_FILE);
	}

	private Superiority extractSuperiority() {
		String superior = null;
		String inferior = null;
		for (int i = 0; i < reader.getAttributeCount(); i++) {
			String attributeName = reader.getAttributeLocalName(i);
			String attributeValue = reader.getAttributeValue(i);

			if (Attribute.SUPERIORITY_SUPERIOR.getAttributeName().equals(attributeName)) {
				superior = attributeValue;
			} else if (Attribute.SUPERIORITY_INFERIOR.getAttributeName().equals(attributeName)) {
				inferior = attributeValue;
			}
		}
		return new Superiority(superior, inferior);
	}

	private void extractModeConversionRule() throws XMLStreamException, TheoryException {
		String mode = "";
		List<String> conversionList = new ArrayList<String>();
		while (reader.hasNext()) {
			int event = reader.next();
			switch (event) {
			case XMLStreamConstants.START_ELEMENT:
				currTag = getXmlTag(reader.getLocalName());
				break;
			case XMLStreamConstants.CHARACTERS:
				if (Tag.MODE_CONVERSION_FROM.equals(currTag)) {
					String v = reader.getText().trim();
					if (!"".equals(v)) mode += v;
				} else if (Tag.MODE_CONVERSION_TO.equals(currTag)) {
					String v = reader.getText().trim();
					if (!"".equals(v)) conversionList.add(reader.getText());
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				currTag = getXmlTag(reader.getLocalName());
				if (Tag.MODE_CONVERSION.equals(currTag)) {
					String[] modes = new String[conversionList.size()];
					addModeConversionRule(mode, conversionList.toArray(modes));
					return;
				}
			}
		}
	}

	private void extractModeConflictRule() throws XMLStreamException, ParserException, TheoryException {
		String mode = "";
		List<String> conflictList = new ArrayList<String>();
		while (reader.hasNext()) {
			int event = reader.next();
			switch (event) {
			case XMLStreamConstants.START_ELEMENT:
				currTag = getXmlTag(reader.getLocalName());
				if (Tag.MODE_CONFLICT_MODE.equals(currTag)) {
				} else if (Tag.MODE_CONFLICT_WITH.equals(currTag)) {
				} else throw new ParserException("unexpected mode found, currTag=" + currTag);
				break;
			case XMLStreamConstants.CHARACTERS:
				if (Tag.MODE_CONFLICT_MODE.equals(currTag)) {
					String v = reader.getText().trim();
					if (!"".equals(v)) mode += v;
				} else if (Tag.MODE_CONFLICT_WITH.equals(currTag)) {
					String v = reader.getText().trim();
					if (!"".equals(v)) conflictList.add(reader.getText());
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				currTag = getXmlTag(reader.getLocalName());
				if (Tag.MODE_CONFLICT.equals(currTag)) {
					String[] modes = new String[conflictList.size()];
					addModeConflictRule(mode, conflictList.toArray(modes));
					return;
				}
			}
		}
	}

	// ===========================================
	// conclusions
	@Override
	protected Map<Literal, Map<ConclusionType, Conclusion>> generateConclusions(InputStream ins) throws ParserException {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		inputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);

		Map<Literal, Map<ConclusionType, Conclusion>> conclusions = new TreeMap<Literal, Map<ConclusionType, Conclusion>>();

		try {
			reader = inputFactory.createXMLStreamReader(ins);

			while (reader.hasNext()) {
				int event = reader.next();
				switch (event) {
				case XMLStreamConstants.START_DOCUMENT:
					break;
				case XMLStreamConstants.START_ELEMENT:
					currTag = getXmlTag(reader.getLocalName());

					if (null == currTag) throw new ParserException("unknown element found in the theory file: " + reader.getLocalName());

					switch (currTag) {
					case DOC_ROOT:
						break;
					case CONCLUSION:
						Conclusion conclusion = extractConclusion(currTag.getXmlTag());
						Literal literal = conclusion.getLiteral();
						Map<ConclusionType, Conclusion> conclusionList = null;
						if (conclusions.containsKey(literal)) {
							conclusionList = conclusions.get(literal);
						} else {
							conclusionList = new TreeMap<ConclusionType, Conclusion>();
							conclusions.put(literal, conclusionList);
						}
						conclusionList.put(conclusion.getConclusionType(), conclusion);
						break;
					default:
					}
					break;
				case XMLStreamConstants.CHARACTERS:
					break;
				case XMLStreamConstants.END_ELEMENT:
					break;
				case XMLStreamConstants.END_DOCUMENT:
					break;
				case XMLStreamConstants.COMMENT:
				}
			}
		} catch (Exception e) {
			throw new ParserException("exception throw while parsing document", e);
		} finally {
			if (null != reader) {
				try {
					reader.close();
				} catch (XMLStreamException e) {
				}
				reader = null;
			}
		}
		return conclusions;
	}

	private Conclusion extractConclusion(String endElement) throws ParserException, XMLStreamException {
		ConclusionType conclusionType = null;
		Literal literal = null;
		for (int i = 0; i < reader.getAttributeCount(); i++) {
			String attributeName = reader.getAttributeLocalName(i);
			String attributeValue = reader.getAttributeValue(i);
			if (Attribute.CONCLUSION_TYPE.getAttributeName().equals(attributeName)) {
				conclusionType = getConclusionType_xml(attributeValue);
			}
		}
		while (reader.hasNext()) {
			int event = reader.next();
			switch (event) {
			case XMLStreamConstants.START_ELEMENT:
				currTag = getXmlTag(reader.getLocalName());
				if (Tag.LITERAL.equals(currTag)) literal = extractLiteral(Tag.LITERAL);
				break;
			case XMLStreamConstants.END_ELEMENT:
				if (endElement.equals(reader.getLocalName())) return new Conclusion(conclusionType, literal);
			}
		}
		throw new ParserException(ErrorMessage.IO_UNEXPECTED_END_OF_FILE);
	}
}
