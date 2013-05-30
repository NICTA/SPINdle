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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.app.utils.NameValuePair;

//import com.app.utils.Entry;

import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.DomUtilities;
import spindle.core.dom.Literal;
import spindle.core.dom.LiteralVariable;
import spindle.core.dom.Mode;
import spindle.core.dom.Rule;
import spindle.core.dom.RuleException;
import spindle.core.dom.RuleType;
import spindle.core.dom.Superiority;
import spindle.core.dom.Theory;
import spindle.core.dom.TheoryException;
import spindle.io.ComponentMismatchException;
import spindle.io.ParserException;
import spindle.io.outputter.DflTheoryConst;
import spindle.io.outputter.DflTheoryOutputter;
import spindle.sys.AppLogger;
import spindle.sys.message.ErrorMessage;

/**
 * Defeasible theory and conclusions parser for theory represented using DFL language.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @version Last modified 2012.09.24
 * @version 2011.07.27
 * @since version 1.0.0
 * @deprecated As of version 2.1.0, the DFL theory parser class {@link spindle.io.parser.DflTheoryParser} is replaced by {@link spindle.io.parser.DflTheoryParser2}.
 * @see spindle.io.parser.DflTheoryParser2
 */
@Deprecated
public class DflTheoryParser extends AbstractTheoryParser {
	public static final String PARSER_TYPE = DflTheoryOutputter.OUTPUTTER_TYPE;

	private static DflTheoryParser INSTANCE = null;
	private static int THEORY_VARIABLE_SYMBOL_LENGTH = RuleType.LITERAL_VARIABLE_SET.getSymbol().length();

	public static Theory getTheory(String theoryString, AppLogger logger) throws ParserException {
		if (null == INSTANCE) INSTANCE = new DflTheoryParser();
		try {
			InputStream ins = new ByteArrayInputStream(theoryString.replaceAll("[\t\r]", "").getBytes());
			if (null != logger) INSTANCE.setAppLogger(logger);
			return INSTANCE.getTheory(ins);
		} catch (ParserException e) {
			throw e;
		} catch (Exception e) {
			throw new ParserException(e);
		} finally {
			INSTANCE.resetAppLogger();
		}
	}

	public static Map<Literal, Map<ConclusionType, Conclusion>> getConclusions(String xmlString, AppLogger logger) throws ParserException {
		if (null == INSTANCE) INSTANCE = new DflTheoryParser();
		try {
			InputStream ins = new ByteArrayInputStream(xmlString.replaceAll("[\t\r]", "").getBytes());
			if (null != logger) INSTANCE.setAppLogger(logger);
			return INSTANCE.getConclusions(ins);
		} catch (ParserException e) {
			throw e;
		} catch (Exception e) {
			throw new ParserException(e);
		} finally {
			INSTANCE.resetAppLogger();
		}
	}

	public static RuleType getRuleType(final String theoryString) throws ParserException {
		if (null == INSTANCE) INSTANCE = new DflTheoryParser();
		return RuleType.getRuleType(theoryString);
	}

	public static Rule extractRuleStr(final String theoryString) throws ParserException {
		if (null == INSTANCE) INSTANCE = new DflTheoryParser();
		try {
			Theory theory = getTheory(theoryString, null);
			if (theory.getFactsAndAllRules().size() > 0) {
				return theory.getFactsAndAllRules().values().iterator().next();
			} else {
				return null;
			}
		} catch (ParserException e) {
			throw e;
		} catch (Exception e) {
			throw new ParserException(e);
		}
	}

	public static Superiority extractSuperiorityStr(final String theoryString) throws ParserException {
		if (null == INSTANCE) INSTANCE = new DflTheoryParser();
		return INSTANCE.extractSuperiority(theoryString);
	}

	public static LiteralVariable extractLiteralVariableStr(final String literalVariableString) throws ParserException, ComponentMismatchException {
		if (null == INSTANCE) INSTANCE = new DflTheoryParser();
		try {
			String valueStr = INSTANCE.getTheoryParserUtilities().formatLiteralVariableString(literalVariableString);
			Literal l = INSTANCE.extractLiteral(valueStr, true);
			if (l instanceof LiteralVariable) return (LiteralVariable) l;

			System.err.println("literalVariableString=" + literalVariableString + ", l=" + l);
			throw new ParserException(ErrorMessage.LITERAL_VARIABLE_INPUT_STRING_ERROR, new Object[] { literalVariableString });
		} catch (ParserException e) {
			throw e;
		} catch (Exception e) {
			throw new ParserException(e);
		}
	}

	private static TheoryParserUtilities parserUtilities = null;

	protected TheoryParserUtilities getTheoryParserUtilities() {
		if (null == parserUtilities) parserUtilities = TheoryParserUtilities.getInstance();
		return parserUtilities;
	}

	private LineNumberReader reader = null;
	private Map<Literal, Map<ConclusionType, Conclusion>> conclusions = null;

	private int ruleCounter = 0;

	public DflTheoryParser() {
		super(PARSER_TYPE);
	}

	@Override
	protected void generateTheory(InputStream ins) throws ParserException {
		reader = new LineNumberReader(new InputStreamReader(new BufferedInputStream(ins)));
		String str = null;
		try {
			while ((str = getNextLine()) != null) {
				int l = str.indexOf(DflTheoryConst.COMMENT_SYMBOL);
				if (l >= 0) str = str.substring(0, l);
				str = str.replace("\r", "").trim();
				if (!"".equals(str)) extractTheoryString(str);
			}
		} catch (ParserException e) {
			throw e;
		} catch (Exception e) {
			int lineNo = reader.getLineNumber();
			throw new ParserException("exception throw in line " + lineNo + ", " + str, e);
		} finally {
			try {
				reader.close();
			} catch (IOException e1) {
			}
			reader = null;
		}
	}

	private String getNextLine() throws IOException {
		return reader.readLine();
	}

	private void extractTheoryString(final String str) throws ParserException {
		try {
			RuleType ruleType = RuleType.getRuleType(str);

			switch (ruleType) {
			case LITERAL_VARIABLE_SET:
				extractLiteralVariable(str);
				break;
			case FACT:
			case STRICT:
			case DEFEASIBLE:
			case DEFEATER:
				String ruleLabel = "";
				String ruleMode = "";
				boolean ruleModeNegation = false;

				String ruleStr = str.replaceAll("\\r", "");

				int loc = ruleStr.indexOf(DflTheoryConst.RULE_LABEL_SEPARATOR);
				if (loc >= 0) {
					ruleLabel = ruleStr.substring(0, loc).trim();
					ruleStr = ruleStr.substring(loc + 1);
				}

				int ms,
				me;
				if ((ms = ruleLabel.indexOf(DflTheoryConst.MODE_START)) >= 0) {
					me = ruleLabel.indexOf(DflTheoryConst.MODE_END);
					if (me < 0) throw new ParserException("rule mode end not found");
					ruleMode = ruleLabel.substring(ms + 1, me).trim();
					ruleLabel = ruleLabel.substring(0, ms);

					if (ruleMode.startsWith(DflTheoryConst.SYMBOL_NEGATION)) {
						ruleMode = ruleMode.substring(1);
						ruleModeNegation = true;
					}
				}
				if ("".equals(ruleLabel)) ruleLabel = TEMP_RULE_LABEL + Theory.formatter.format(ruleCounter++);

				Rule rule = extractRule(ruleType, ruleLabel, ("".equals(ruleMode) ? null : new Mode(ruleMode, ruleModeNegation)), ruleStr);
				if (ruleLabel.startsWith(TEMP_RULE_LABEL)) addPendingRule(rule);
				else addRule(rule);
				break;
			case SUPERIORITY:
			case INFERIORITY:
				Superiority superiority = extractSuperiority(str);
				addSuperiority(superiority);
				break;
			case MODE_CONVERSION:
			case MODE_CONFLICT:
				extractModeConversionAndModeRuleConflict(str);
				break;
			default:
			}
		} catch (ParserException e) {
			throw e;
		} catch (Exception e) {
			throw new ParserException("exception throw while processing [" + str + "]", e);
		}
	}

	private void extractLiteralVariable(String str) throws ParserException {
		String code = str.substring(THEORY_VARIABLE_SYMBOL_LENGTH).replaceAll("\\s", "");
		int l = code.indexOf(DflTheoryConst.THEORY_EQUAL_SIGN);
		if (l < 0) throw new ParserException(ErrorMessage.LITERAL_VARIABLE_DEFINITION_NOT_FOUND);

		try {
			String nameStr = code.substring(0, l).trim();
			String valueStr = code.substring(l + 1);
			if (nameStr.length() == 0 || valueStr.length() == 0) throw new ParserException(ErrorMessage.LITERAL_VARIABLE_DEFINITION_NOT_FOUND, null, code);

			LiteralVariable lvName = (LiteralVariable) extractLiteral(nameStr, true);
			if (getAppConstants().isAppConstant(lvName)) throw new ParserException(ErrorMessage.LITERAL_VARIABLE_APP_CONSTANT_AS_NAME, new Object[] { str });

			valueStr = getTheoryParserUtilities().formatLiteralVariableString(valueStr);
			LiteralVariable lvValue = (LiteralVariable) extractLiteral(valueStr, true);

			addLiteralVariable(lvName, lvValue);
		} catch (ParserException e) {
			throw e;
		} catch (Exception e) {
			throw new ParserException(e);
		}

	}

	private Rule extractRule(RuleType ruleType, String ruleLabel, Mode ruleMode, String str) throws ParserException, RuleException {
		int loc = str.indexOf(ruleType.getSymbol());
		String bodyStr = str.substring(0, loc);
		String headStr = str.substring(loc + ruleType.getSymbol().length());

		Rule rule = DomUtilities.getRule(ruleLabel, ruleType);
		if (null != ruleMode) rule.setMode(ruleMode);
		if (!"".equals(bodyStr.trim())) {
			for (Literal literal : extractLiteralList(bodyStr)) {
				rule.addBodyLiteral(literal);
			}
		}
		if (!"".equals(headStr.trim())) {
			for (Literal literal : extractLiteralList(headStr)) {
				rule.addHeadLiteral(literal);
			}
		}
		return rule;
	}

	private List<Literal> extractLiteralList(String str) throws ParserException {
		List<Literal> literals = new ArrayList<Literal>();
		try {
			List<String> literalsString = getTheoryParserUtilities().parseLiteralString(str);
			for (String literalStr : literalsString) {
				literals.add(extractLiteral(literalStr, false));
			}
		} catch (Exception e) {
			System.err.println("str=" + str);
			throw new ParserException(e);
		}
		return literals;
	}

	private Literal extractLiteral(final String str, boolean isVerifyString) throws ParserException, ComponentMismatchException {
		boolean containsAbstractLiteralInPredicate = getAppConstants().containsAbstractLiteralInPredicate(str);

		String literalName = "";
		boolean isNegation = false;
		String modeName = "";
		boolean isModeNegation = false;
		String predicates = "";
		// TimeStamp timeStamp = null;

		String literalStr = null;
		if (isVerifyString) {
			List<String> literalList = getTheoryParserUtilities().parseLiteralString(str);
			if (literalList.size() == 0) throw new ParserException(ErrorMessage.LITERAL_STRING_INCORRECT_FORMAT);
			if (literalList.size() > 1) throw new ParserException(ErrorMessage.LITERAL_STRING_CONTAINS_MULTIPLE_LITERALS);
			literalStr = literalList.get(0);
		} else literalStr = str.trim();

		NameValuePair<String, String> theoryBooleanFunctionEntry = extractLiteralStringComponent(literalStr, DflTheoryConst.LITERAL_BOOLEAN_FUNCTION_PREFIX,
				DflTheoryConst.LITERAL_BOOLEAN_FUNCTION_POSTFIX, //
				true);

		boolean isBooleanFunction = "".equals(theoryBooleanFunctionEntry.getValue());
		if (isBooleanFunction) { return DomUtilities.getLiteralVariable(literalStr, false); }

		literalStr = theoryBooleanFunctionEntry.getValue();

		NameValuePair<String, String> modeEntry = extractLiteralStringComponent(literalStr, //
				DflTheoryConst.MODE_START, DflTheoryConst.MODE_END, true);
		modeName = modeEntry.getKey();
		NameValuePair<String, String> predicateEntry = extractLiteralStringComponent(modeEntry.getValue(), //
				DflTheoryConst.PREDICATE_START, DflTheoryConst.PREDICATE_END, !containsAbstractLiteralInPredicate);
		predicates = predicateEntry.getKey();
		NameValuePair<String, String> temporalEntry = extractLiteralStringComponent(predicateEntry.getValue(), //
				DflTheoryConst.TIMESTAMP_START, DflTheoryConst.TIMESTAMP_END, true);

		@SuppressWarnings("unused")
		String temporal = temporalEntry.getKey();

		literalStr = temporalEntry.getValue();

		if (literalStr.startsWith(DflTheoryConst.SYMBOL_NEGATION)) {
			literalStr = literalStr.substring(DflTheoryConst.SYMBOL_NEGATION.length()).trim();
			isNegation = true;
		}
		literalName = literalStr.trim();

		Literal literal = DomUtilities.getLiteral(literalName, isNegation, //
				modeName, isModeNegation);

		if (containsAbstractLiteralInPredicate) {
			literal.setPredicates(null);
			LiteralVariable lv = (LiteralVariable) DomUtilities.getLiteralVariable(literal);
			if (!"".equals(predicates)) {
				List<Literal> li = extractLiteralList(predicates);
				Literal[] literalPredicates = new Literal[li.size()];
				li.toArray(literalPredicates);
				lv.setLiteralPredicates(literalPredicates);
			}
			literal = lv;
		} else {
			literal.setPredicates(predicates.split("" + DflTheoryConst.LITERAL_SEPARATOR));
			if (literalName.startsWith("" + DflTheoryConst.LITERAL_VARIABLE_PREFIX)) {
				literal = DomUtilities.getLiteralVariable(literal);
			}
		}
		return literal;
	}

	private NameValuePair<String, String> extractLiteralStringComponent(final String literalStr, final char prefix, final char postfix, boolean verifyContent)
			throws ParserException {
		if (null == literalStr || "".equals(literalStr.trim())) return new NameValuePair<String, String>("", "");
		int locStart = literalStr.indexOf(prefix);
		int locEnd = literalStr.lastIndexOf(postfix);

		if (locStart < 0 && locEnd < 0) return new NameValuePair<String, String>("", literalStr);
		if (locStart < 0 || locEnd < 0) throw new ParserException(ErrorMessage.LITERAL_STRING_INCORRECT_FORMAT, literalStr);

		String residual = literalStr.substring(0, locStart) + literalStr.substring(locEnd + 1);
		String content = literalStr.substring(locStart + 1, locEnd);

		if (verifyContent && (content.indexOf(prefix) >= 0 || residual.indexOf(postfix) >= 0)) throw new ParserException(
				ErrorMessage.LITERAL_STRING_INCORRECT_FORMAT, new Object[] { literalStr });

		NameValuePair<String, String> entry = new NameValuePair<String, String>(content, residual);

		return entry;
	}

	private Superiority extractSuperiority(String str) throws ParserException {
		try {
			int l = str.indexOf(RuleType.SUPERIORITY.getSymbol());
			String superior = "";
			String inferior = "";
			if (l > 0) {
				superior = str.substring(0, l).trim();
				inferior = str.substring(l + RuleType.SUPERIORITY.getSymbol().length()).trim();
			} else {
				l = str.indexOf(RuleType.INFERIORITY.getSymbol());
				inferior = str.substring(0, l).trim();
				superior = str.substring(l + RuleType.SUPERIORITY.getSymbol().length()).trim();
			}
			if ("".equals(superior)) throw new ParserException(ErrorMessage.SUPERIORITY_SUPERIOR_RULE_NOT_DEFINED, str);
			if ("".equals(inferior)) throw new ParserException(ErrorMessage.SUPERIORITY_INFERIOR_RULE_NOT_DEFINED, str);

			return new Superiority(superior, inferior);
		} catch (Exception e) {
			throw new ParserException(e);
		}
	}

	private void extractModeConversionAndModeRuleConflict(String str) throws ParserException, TheoryException {
		int l = -1;
		if ((l = str.indexOf(DflTheoryConst.SYMBOL_MODE_CONVERSION)) > 0) {
			String o = str.substring(0, l).trim();
			String[] c = str.substring(l + DflTheoryConst.SYMBOL_MODE_CONVERSION.length()).split("" + DflTheoryConst.LITERAL_SEPARATOR);
			addModeConversionRule(o, c);
		} else if ((l = str.indexOf(DflTheoryConst.SYMBOL_MODE_CONFLICT)) > 0) {
			String o = str.substring(0, l).trim();
			String[] c = str.substring(l + DflTheoryConst.SYMBOL_MODE_CONVERSION.length()).split("" + DflTheoryConst.LITERAL_SEPARATOR);
			addModeConflictRule(o, c);
		} else {
			throw new ParserException(ErrorMessage.RULE_UNRECOGNIZED_RULE_TYPE);
		}
	}

	// ===========================================
	// conclusions
	@Override
	protected Map<Literal, Map<ConclusionType, Conclusion>> generateConclusions(InputStream ins) throws ParserException {
		reader = new LineNumberReader(new InputStreamReader(new BufferedInputStream(ins)));
		String str = null;
		conclusions = new TreeMap<Literal, Map<ConclusionType, Conclusion>>();
		try {
			while ((str = getNextLine()) != null) {
				int l = str.indexOf(DflTheoryConst.COMMENT_SYMBOL);
				if (l >= 0) str = str.substring(0, l);
				str = str.replace("\r", "").trim();
				if (!"".equals(str)) extractConclusionString(str);
			}
		} catch (Exception e) {
			int lineNo = reader.getLineNumber();
			conclusions = null;
			throw new ParserException(ErrorMessage.IO_FILE_READING_ERROR, new Object[] { lineNo }, str, e);
		} finally {
			try {
				reader.close();
			} catch (IOException e1) {
			}
			reader = null;
		}
		return conclusions;
	}

	protected void extractConclusionString(String str) throws ParserException, ComponentMismatchException {
		ConclusionType conclusionType = ConclusionType.getConclusionType(str);
		String literalStr = str.substring(conclusionType.getSymbol().length()).trim();
		Literal literal = extractLiteral(literalStr, true);

		Map<ConclusionType, Conclusion> conclusionList = null;
		if (conclusions.containsKey(literal)) {
			conclusionList = conclusions.get(literal);
		} else {
			conclusionList = new TreeMap<ConclusionType, Conclusion>();
			conclusions.put(literal, conclusionList);
		}
		conclusionList.put(conclusionType, new Conclusion(conclusionType, literal));
	}

}
