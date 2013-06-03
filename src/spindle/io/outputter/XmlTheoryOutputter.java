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
package spindle.io.outputter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import spindle.core.dom.Conclusion;
import spindle.core.dom.DomConst;
import spindle.core.dom.Literal;
import spindle.core.dom.LiteralVariable;
import spindle.core.dom.Mode;
import spindle.core.dom.Rule;
import spindle.core.dom.Superiority;
import spindle.core.dom.Theory;
import spindle.io.OutputterException;
import spindle.io.outputter.XmlTag.Attribute;
import spindle.io.outputter.XmlTag.Tag;

/**
 * Defeasible theory and conclusions outputter in XML.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 */
public class XmlTheoryOutputter extends AbstractTheoryOutputter {
	public static final String OUTPUTTER_TYPE = "xml";

	public static String getTheoryAsXmlString(Theory theory) throws OutputterException {
		ByteArrayOutputStream writer = new ByteArrayOutputStream();

		XmlTheoryOutputter outputter = new XmlTheoryOutputter();
		outputter.save(writer, theory);
		return writer.toString();
	}

	public static String getConclusionsAsXmlString(List<Conclusion> conclusionsAsList) throws OutputterException {
		ByteArrayOutputStream writer = new ByteArrayOutputStream();

		XmlTheoryOutputter outputter = new XmlTheoryOutputter();
		outputter.save(writer, conclusionsAsList);
		return writer.toString();
	}

	public XmlTheoryOutputter() {
		super(OUTPUTTER_TYPE);
	}

	@Override
	protected void saveToStream(OutputStream os, List<Conclusion> conclusionsAsList) throws OutputterException {
		try {
			if (null == writer) throw new IOException("writer is null");

			Document document = generateConclusionsDocument(conclusionsAsList);
			Transformer xmlTransformer = getXmlTransformer();

			StreamResult result = new StreamResult(os);
			DOMSource source = new DOMSource(document);
			xmlTransformer.transform(source, result);
		} catch (Exception e) {
			throw new OutputterException(e);
		}
	}

	private Document generateConclusionsDocument(List<Conclusion> conclusionsAsList) throws OutputterException {
		Document document = null;
		try {
			document = getNewXmlDocument();
			document.appendChild(document.createComment(getHeaderComment()));
			document.appendChild(document.createComment(getGenerationTimeString()));

			Element docRoot = document.createElement(XmlTag.Tag.DOC_ROOT.getXmlTag());
			document.appendChild(docRoot);

			for (Conclusion conclusion : conclusionsAsList) {
				Element conclusionEle = document.createElement(XmlTag.Tag.CONCLUSION.getXmlTag());
				conclusionEle.setAttribute(XmlTag.Attribute.CONCLUSION_TYPE.getAttributeName(), conclusion.getConclusionType().getTextTag());

				Element literalEle = generateLiteral(document, conclusion.getLiteral(), Tag.LITERAL);
				conclusionEle.appendChild(literalEle);

				docRoot.appendChild(conclusionEle);
			}
		} catch (ParserConfigurationException e) {
			throw new OutputterException("exception throw while creating XML document", e);
		}
		return document;
	}

	@Override
	protected void saveToStream(OutputStream os, Theory theory) throws OutputterException {
		try {
			if (null == writer) throw new IOException("writer is null");

			Document document = generateTheoryDocument(theory);
			Transformer xmlTransformer = getXmlTransformer();

			StreamResult result = new StreamResult(os);
			DOMSource source = new DOMSource(document);
			xmlTransformer.transform(source, result);
		} catch (Exception e) {
			throw new OutputterException(e);
		}
	}

	private Document generateTheoryDocument(Theory theory) throws OutputterException {
		Document document = null;
		try {
			document = getNewXmlDocument();
			document.appendChild(document.createComment(getHeaderComment()));
			document.appendChild(document.createComment(getGenerationTimeString()));

			Element docRoot = document.createElement(Tag.DOC_ROOT.getXmlTag());
			document.appendChild(docRoot);

			addLiteralVariables(document, theory.getLiteralVariables(), Tag.LITERAL_VARIABLE);
			addLiteralVariables(document, theory.getLiteralBooleanFunctions(), Tag.LITERAL_BOOLEAN_FUNCTION);

			for (Rule rule : theory.getFactsAndAllRules().values()) {
				switch (rule.getRuleType()) {
				case FACT:
					addFact(document, rule);
					break;
				case STRICT:
				case DEFEASIBLE:
				case DEFEATER:
					addRule(document, rule);
					break;
				default:
				}
			}

			for (Superiority sup : theory.getAllSuperiority()) {
				addSuperiority(document, sup);
			}

			Map<String, Set<String>> resolveRules_conversion = theory.getAllModeConversionRules();
			if (null != resolveRules_conversion) {
				for (Entry<String, Set<String>> entry : resolveRules_conversion.entrySet()) {
					addModeConversionRule(document, entry.getKey(), entry.getValue());
				}
			}
			Map<String, Set<String>> resolveRules_conflict = theory.getAllModeConflictRules();
			if (null != resolveRules_conflict) {
				for (Entry<String, Set<String>> entry : resolveRules_conflict.entrySet()) {
					addModeConflictRule(document, entry.getKey(), entry.getValue());
				}
			}

		} catch (ParserConfigurationException e) {
			throw new OutputterException("exception throw while creating XML document", e);
		}
		return document;
	}

	private void addLiteralVariables(Document document, Map<LiteralVariable, LiteralVariable> literalVariables, Tag xmlRootTag) throws OutputterException {
		if (null == literalVariables || literalVariables.size() == 0) return;
		for (Entry<LiteralVariable, LiteralVariable> entry : literalVariables.entrySet()) {
			Element elementRoot = document.createElement(xmlRootTag.getXmlTag());
			Element eleName = document.createElement(Tag.LITERAL_VARIABLE_NAME.getXmlTag());
			Element eleValue = document.createElement(Tag.LITERAL_VARIABLE_VALUE.getXmlTag());
			elementRoot.appendChild(eleName);
			elementRoot.appendChild(eleValue);

			eleName.appendChild(generateLiteral(document, entry.getKey(), Tag.LITERAL));
			if (xmlRootTag.equals(Tag.LITERAL_VARIABLE)) {
				eleValue.appendChild(generateLiteral(document, entry.getValue(), Tag.LITERAL));
			} else {
				eleValue.appendChild(generateLiteral(document, entry.getValue(), Tag.LITERAL_BOOLEAN_FUNCTION_FORMULA));
			}

			document.getDocumentElement().appendChild(elementRoot);
		}
	}

	private void addFact(Document document, Rule rule) throws OutputterException {
		Element ele = document.createElement(Tag.FACT.getXmlTag());
		if (!rule.getLabel().startsWith(DEFAULT_RULE_LABEL_PREFIX)) {
			ele.setAttribute(XmlTag.Attribute.RULE_LABEL.getAttributeName(), rule.getLabel());
		}
		Element modeEle = generateMode(document, rule.getMode());
		if (null != modeEle) ele.appendChild(modeEle);

		Element headEle = generateLiteralList(document, rule.getHeadLiterals());
		if (null == headEle) throw new OutputterException("exception throw while generating xml model for rule [" + rule.getLabel()
				+ "], head literal is empty");
		ele.appendChild(headEle);

		document.getDocumentElement().appendChild(ele);
	}

	private void addRule(Document document, Rule rule) throws OutputterException {
		Element ele = document.createElement(Tag.RULE.getXmlTag());
		if (!rule.getLabel().startsWith(DEFAULT_RULE_LABEL_PREFIX)) ele.setAttribute(Attribute.RULE_LABEL.getAttributeName(), rule.getLabel());
		switch (rule.getRuleType()) {
		case STRICT:
			ele.setAttribute(Attribute.RULE_TYPE_STRICT_RULE.getAttributeName(), Attribute.RULE_TYPE_STRICT_RULE.getAttributeValue());
			break;
		case DEFEASIBLE:
			ele.setAttribute(Attribute.RULE_TYPE_DEFEASIBLE_RULE.getAttributeName(), Attribute.RULE_TYPE_DEFEASIBLE_RULE.getAttributeValue());
			break;
		case DEFEATER:
			ele.setAttribute(Attribute.RULE_TYPE_DEFEATER.getAttributeName(), XmlTag.Attribute.RULE_TYPE_DEFEATER.getAttributeValue());
			break;
		default:
		}

		Element modeEle = generateMode(document, rule.getMode());
		if (null != modeEle) ele.appendChild(modeEle);

		Element headEleList = generateLiteralList(document, rule.getHeadLiterals());
		if (null == headEleList) throw new OutputterException("exception throw while generating xml model for rule [" + rule.getLabel()
				+ "], head literal is empty");
		Element headEle = document.createElement(Tag.HEAD.getXmlTag());
		headEle.appendChild(headEleList);
		ele.appendChild(headEle);

		Element bodyEleList = generateLiteralList(document, rule.getBodyLiterals());
		if (null != bodyEleList) {
			Element bodyEle = document.createElement(Tag.BODY.getXmlTag());
			bodyEle.appendChild(bodyEleList);
			ele.appendChild(bodyEle);
		}

		document.getDocumentElement().appendChild(ele);

	}

	private Element generateLiteralList(Document document, List<Literal> literals) {
		if (null == literals || literals.size() == 0) return null;

		if (literals.size() == 1) return generateLiteral(document, literals.get(0), Tag.LITERAL);

		Element andEle = document.createElement(Tag.AND.getXmlTag());
		for (Literal literal : literals) {
			andEle.appendChild(generateLiteral(document, literal, Tag.LITERAL));
		}

		return andEle;
	}

	private Element generateLiteral(Document document, Literal literal, Tag xmlRootTag) {
		Element literalEle = document.createElement(xmlRootTag.getXmlTag());

		Element atomEle = document.createElement(Tag.ATOM.getXmlTag());
		atomEle.appendChild(document.createTextNode(literal.getName()));

		if (literal.isNegation()) {
			Element negEle = document.createElement(Tag.NOT.getXmlTag());
			negEle.appendChild(atomEle);
			atomEle = negEle;
		}
		literalEle.appendChild(atomEle);

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < literal.getPredicatesSize(); i++) {
			if (i > 0) sb.append(DomConst.Literal.LITERAL_SEPARATOR);
			sb.append(literal.getPredicate(i));
		}

		if (!DomConst.Literal.DEFAULT_PREDICATE_VALUE.equals(sb.toString())) {
			Element elePredicate = document.createElement(Tag.PREDICATE.getXmlTag());
			elePredicate.appendChild(document.createTextNode(sb.toString()));
			literalEle.appendChild(elePredicate);
		}

		Element modeEle = generateMode(document, literal.getMode());
		if (null != modeEle) literalEle.appendChild(modeEle);

		return literalEle;
	}

	private Element generateMode(Document document, Mode mode) {
		Element modeEle = null;
		if (!"".equals(mode.getName())) {
			modeEle = document.createElement(Tag.MODE.getXmlTag());
			modeEle.appendChild(document.createTextNode(mode.getName()));
			if (mode.isNegation()) {
				Element negEle = document.createElement(Tag.NOT.getXmlTag());
				negEle.appendChild(modeEle);
				modeEle = negEle;
			}
		}
		return modeEle;
	}

	private void addSuperiority(Document document, Superiority sup) {
		Element ele = document.createElement(Tag.SUPERIORITY.getXmlTag());
		ele.setAttribute(Attribute.SUPERIORITY_SUPERIOR.getAttributeName(), sup.getSuperior());
		ele.setAttribute(Attribute.SUPERIORITY_INFERIOR.getAttributeName(), sup.getInferior());

		document.getDocumentElement().appendChild(ele);
	}

	private void addModeConversionRule(Document document, String mode, Set<String> convertModes) {
		Element ele = document.createElement(Tag.MODE_CONVERSION.getXmlTag());

		Element fromEle = document.createElement(Tag.MODE_CONVERSION_FROM.getXmlTag());
		fromEle.appendChild(document.createTextNode(mode));
		ele.appendChild(fromEle);

		for (String m : convertModes) {
			Element toEle = document.createElement(Tag.MODE_CONVERSION_TO.getXmlTag());
			toEle.appendChild(document.createTextNode(m));
			ele.appendChild(toEle);
		}

		document.getDocumentElement().appendChild(ele);
	}

	private void addModeConflictRule(Document document, String mode, Set<String> conflictModes) {
		Element ele = document.createElement(Tag.MODE_CONFLICT.getXmlTag());

		Element modeEle = document.createElement(Tag.MODE_CONFLICT_MODE.getXmlTag());
		modeEle.appendChild(document.createTextNode(mode));
		ele.appendChild(modeEle);

		for (String m : conflictModes) {
			Element conflictWithEle = document.createElement(Tag.MODE_CONFLICT_WITH.getXmlTag());
			conflictWithEle.appendChild(document.createTextNode(m));
			ele.appendChild(conflictWithEle);
		}

		document.getDocumentElement().appendChild(ele);
	}
}
