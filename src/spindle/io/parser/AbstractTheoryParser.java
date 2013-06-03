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
package spindle.io.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.app.utils.TextUtilities;

import spindle.core.dom.AppConstants;
import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.LiteralVariable;
import spindle.core.dom.Rule;
import spindle.core.dom.RuleType;
import spindle.core.dom.Superiority;
import spindle.core.dom.Theory;
import spindle.core.dom.TheoryException;
import spindle.io.ParserException;
import spindle.io.TheoryParser;
import spindle.io.outputter.DflTheoryConst;
import spindle.io.outputter.XmlTag.Attribute;
import spindle.io.outputter.XmlTag.Tag;
import spindle.sys.AppModuleBase;
import spindle.sys.Conf;
import spindle.sys.Messages;
import spindle.sys.message.ErrorMessage;
import spindle.sys.message.SystemMessage;

/**
 * Base class for defeasible theory praser
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @version Last modified 2011.07.27
 * @since version 1.0.0
 */
@SuppressWarnings("deprecation")
public abstract class AbstractTheoryParser extends AppModuleBase implements TheoryParser {

	protected static final String SYMBOL_NEGATION = DflTheoryConst.SYMBOL_NEGATION;
	protected static final String TEMP_RULE_LABEL = "TEMP_RULE_LABEL";

	private String parserType = null;
	private List<Rule> pendingRules = null;
	private AppConstants appConstants = null;

	protected Theory theory = null;

	protected AbstractTheoryParser(final String parserType) {
		super();
		if (null == parserType) throw new IllegalArgumentException(Messages.getErrorMessage(ErrorMessage.IO_PARSER_TYPE_NULL));
		this.parserType = parserType.trim().toLowerCase();
		pendingRules = new ArrayList<Rule>();
	}

	public String getParserType() {
		return parserType;
	}

	protected void addPendingRule(Rule rule) {
		pendingRules.add(rule);
	}

	private void addSummaryContentString(StringBuilder sb, int counter, String label) {
		if (counter < 1) return;
		sb.append("\n").append(counter).append(" ").append(label);
		if (counter > 1) sb.append("s");
	}

	protected void printSummaries(Theory theory) {
		StringBuilder sbMisc = new StringBuilder();
		addSummaryContentString(sbMisc, theory.getLiteralVariableCount() + theory.getLiteralBooleanFunctionsInRulesCount(),
				RuleType.LITERAL_VARIABLE_SET.getLabel());
		addSummaryContentString(sbMisc, theory.getModeConversionRulesCount(), RuleType.MODE_CONVERSION.getLabel() + " rule");
		addSummaryContentString(sbMisc, theory.getModeConflictRulesCount(), RuleType.MODE_CONFLICT.getLabel() + " rule");
		addSummaryContentString(sbMisc, theory.getModeExclusionRulesCount(), RuleType.MODE_EXCLUSION.getLabel() + " rule");

		StringBuilder sbRules = new StringBuilder();
		addSummaryContentString(sbRules, theory.getFactsCount(), RuleType.FACT.getLabel() + " rule");
		addSummaryContentString(sbRules, theory.getStrictRulesCount(), RuleType.STRICT.getLabel() + " rule");
		addSummaryContentString(sbRules, theory.getDefeasibleRulesCount(), RuleType.DEFEASIBLE.getLabel() + " rule");
		addSummaryContentString(sbRules, theory.getDefeatersCount(), RuleType.DEFEATER.getLabel() + " rule");
		addSummaryContentString(sbRules, theory.getSuperiorityCount(), RuleType.SUPERIORITY.getLabel() + " rule");

		StringBuilder sb = new StringBuilder(Messages.getSystemMessage(SystemMessage.THEORY_THEORY_CONTAINS));
		sb.append(":").append(sbMisc);
		if (sbMisc.length() > 0) sb.append("\n");
		sb.append(sbRules);

		String summaries = TextUtilities.generateHighLightedMessage(sb.toString());
		if (!Conf.isConsoleMode()) System.out.println(summaries);
		logMessage(Level.INFO, 0, summaries);
	}

	protected void printSummaries(Map<Literal, Map<ConclusionType, Conclusion>> conclusions) {
		StringBuilder sb = new StringBuilder("Conclusions");
		if (conclusions.size() == 0) {
			sb.append(LINE_SEPARATOR).append("** conclusion is empty");
		} else {
			for (Entry<Literal, Map<ConclusionType, Conclusion>> entry : conclusions.entrySet()) {
				sb.append(LINE_SEPARATOR).append(entry.getKey()).append(":").append(entry.getValue().keySet());
			}
		}
		System.out.println(sb.toString());
		logMessage(Level.INFO, 0, sb.toString());
	}

	protected void addLiteralVariable(LiteralVariable literalVariableName, LiteralVariable literalVariableValue) throws TheoryException {
		logMessage(
				Level.INFO,
				1,
				Messages.getSystemMessage(SystemMessage.THEORY_ADD_NEW_LITERAL_VARIABLE, new Object[] { literalVariableName,
						literalVariableValue }));
		theory.addLiteralVariable(literalVariableName, literalVariableValue);
	}

	protected void addRule(Rule rule) throws TheoryException {
		switch (rule.getRuleType()) {
		case FACT:
			logMessage(Level.INFO, 1, Messages.getSystemMessage(SystemMessage.THEORY_ADD_NEW_FACT, new Object[] { rule }));
			theory.addFact(rule);
			break;
		case STRICT:
		case DEFEASIBLE:
			logMessage(Level.INFO, 1, Messages.getSystemMessage(SystemMessage.THEORY_ADD_NEW_RULE, new Object[] { rule }));
			theory.addRule(rule);
			break;
		case DEFEATER:
			logMessage(Level.INFO, 1, Messages.getSystemMessage(SystemMessage.THEORY_ADD_NEW_DEFEATER, new Object[] { rule }));
			theory.addRule(rule);
			break;
		default:
			throw new TheoryException("unknown rule type: " + rule.toString());
		}
	}

	protected void addSuperiority(Superiority superiority) throws TheoryException {
		logMessage(Level.INFO, 1,
				Messages.getSystemMessage(SystemMessage.THEORY_ADD_NEW_SUPERIORITY_RELATION, new Object[] { superiority }));
		theory.add(superiority);
	}

	protected void addModeConversionRule(String modeName, String[] convertModes) throws TheoryException {
		logMessage(Level.INFO, 1, Messages.getSystemMessage(SystemMessage.THEORY_ADD_NEW_MODE_CONVERSION_RULE, new Object[] { modeName }));
		theory.addModeConversionRules(modeName, convertModes);
	}

	protected void addModeConflictRule(String modeName, String[] conflictModes) throws TheoryException {
		logMessage(Level.INFO, 1, Messages.getSystemMessage(SystemMessage.THEORY_ADD_NEW_MODE_CONFLICT_RULE, new Object[] { modeName }));
		theory.addModeConflictRules(modeName, conflictModes);
	}

	@Override
	public Theory getTheory(InputStream ins) throws ParserException {
		if (null == ins) throw new ParserException(ErrorMessage.IO_INPUT_STREAM_NULL);
		pendingRules.clear();
		logMessage(Level.FINE, 0, Messages.getSystemMessage(SystemMessage.THEORY_NEW_THEORY));
		try {
			theory = new Theory();
			generateTheory(ins);
		} catch (ParserException e) {
			theory = null;
			throw e;
		} catch (Exception e) {
			theory = null;
			throw new ParserException(e);
		} finally {
			if (null != ins) {
				try {
					ins.close();
				} catch (IOException e) {
				}
				ins = null;
			}
		}

		if (pendingRules.size() > 0) {
			try {
				for (Rule rule : pendingRules) {
					String ruleLabel = theory.getUniqueRuleLabel();
					rule.setLabel(ruleLabel);
					theory.addRule(rule);
				}
			} catch (Exception e) {
				throw new ParserException(e);
			}
		}

		if (null == theory) throw new ParserException(ErrorMessage.THEORY_NULL_THEORY);
		if (theory.isEmpty()) throw new ParserException(ErrorMessage.THEORY_EMPTY_THEORY);
		if (Conf.isShowProgress()) printSummaries(theory);

		return theory;
	}

	@Override
	public Map<Literal, Map<ConclusionType, Conclusion>> getConclusions(InputStream ins) throws ParserException {
		if (null == ins) throw new ParserException(ErrorMessage.IO_INPUT_STREAM_NULL);
		Map<Literal, Map<ConclusionType, Conclusion>> conclusions = null;
		try {
			conclusions = generateConclusions(ins);
		} catch (Exception e) {
			throw new ParserException(e);
		} finally {
			if (null != ins) {
				try {
					ins.close();
				} catch (IOException e) {
				}
				ins = null;
			}
		}

		if (null == conclusions) throw new ParserException(ErrorMessage.CONCLUSION_NULL_CONCLUSIONS_SET);
		if (Conf.isShowProgress()) printSummaries(conclusions);

		return conclusions;
	}

	protected AppConstants getAppConstants() {
		if (null == appConstants) appConstants = AppConstants.getInstance(null);
		return appConstants;
	}

	protected abstract void generateTheory(InputStream ins) throws ParserException;

	protected abstract Map<Literal, Map<ConclusionType, Conclusion>> generateConclusions(InputStream ins) throws ParserException;

	// //////////////////////////////////////////////
	// for DFL document
	//
	// protected RuleType getRuleType_dfl(String str) throws ParserException {
	// for (RuleType ruleType : RuleType.values()) {
	// if (str.indexOf(ruleType.getSymbol()) >= 0) return ruleType;
	// }
	// throw new ParserException("unknown rule type: ["+str+"]");
	// }
	//
	// protected ConclusionType getConclusionType_dfl(String str) throws ParserException {
	// for (ConclusionType conclusionType : ConclusionType.values()) {
	// if (str.indexOf(conclusionType.getSymbol()) >= 0) return conclusionType;
	// }
	// throw new ParserException(getClass().getName() + ":unknown conclusion type");
	// }

	// //////////////////////////////////////////////
	// for XML document
	//
	@Deprecated
	protected Tag getXmlTag(String elementName) {
		for (Tag tag : Tag.values()) {
			if (tag.getXmlTag().equals(elementName)) return tag;
		}
		return null;
	}

	@Deprecated
	protected Attribute getAttributeTag(String attributeName) {
		for (Attribute att : Attribute.values()) {
			if (att.getAttributeName().equals(attributeName)) return att;
		}
		return null;
	}

	protected RuleType getRuleType_xml(String str) throws ParserException {
		return RuleType.valueOf(str);		
//		if (Attribute.RULE_TYPE_STRICT_RULE.getAttributeValue().equals(str)) return RuleType.STRICT;
//		if (Attribute.RULE_TYPE_DEFEASIBLE_RULE.getAttributeValue().equals(str)) return RuleType.DEFEASIBLE;
//		if (Attribute.RULE_TYPE_DEFEATER.getAttributeValue().equals(str)) return RuleType.DEFEATER;
//		throw new ParserException("unknown rule type");
	}

	protected ConclusionType getConclusionType_xml(String str) throws ParserException {
		for (ConclusionType conclusionType : ConclusionType.values()) {
			if (str.indexOf(conclusionType.getTextTag()) >= 0) return conclusionType;
		}
		throw new ParserException("unknown conclusion type");
	}
}
