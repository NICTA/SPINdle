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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.app.utils.Converter;

import spindle.core.dom.AppConstant;
import spindle.core.dom.AppConstants;
import spindle.core.dom.DomConst;
import spindle.core.dom.LiteralVariable;
import spindle.core.dom.DomConst.Literal;
import spindle.core.dom.impl.Duration;
import spindle.core.dom.impl.Val;
import spindle.io.ComponentMismatchException;
import spindle.io.ParserException;
import spindle.io.outputter.DflTheoryConst;
import spindle.sys.InvalidArgumentException;
import spindle.sys.message.ErrorMessage;

/**
 * Theory parser utilities class.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @version 2011.07.26
 * @since version 2.1.0
 * @deprecated As of version 2.1.0, the DFL theory parser class {@link spindle.io.parser.DflTheoryParser} will be replaced by
 *             {@link spindle.io.parser.DflTheoryParser2}. The theory parser utilities class will no longer be used
 *             anymore.
 */
@Deprecated
public class TheoryParserUtilities {
	private static TheoryParserUtilities INSTANCE = null;

	public static TheoryParserUtilities getInstance() {
		if (null == INSTANCE) INSTANCE = new TheoryParserUtilities();
		return INSTANCE;
	}

	private AppConstants appConstants = null;

	protected AppConstants getAppConstants() {
		if (null == appConstants) appConstants = AppConstants.getInstance(null);
		return appConstants;
	}

	private static enum LiteralComponent {
		THEORY_FUNCTION_START(DflTheoryConst.LITERAL_BOOLEAN_FUNCTION_PREFIX), //
		MODE_START(DflTheoryConst.MODE_START), //
		MODE_END(DflTheoryConst.MODE_END), //
		NAME_START(""), //
		NAME_END(""), //
		PREDICATE_START(DflTheoryConst.PREDICATE_START), //
		PREDICATE_END(DflTheoryConst.PREDICATE_END), //
		TEMPORAL_START(DflTheoryConst.TIMESTAMP_START), //
		TEMPORAL_END(DflTheoryConst.TIMESTAMP_END), //
		THEORY_FUNCTION_END(DflTheoryConst.LITERAL_BOOLEAN_FUNCTION_POSTFIX);

		private String symbol;

		LiteralComponent(String symbol) {
			setSymbol(symbol);
		}

		LiteralComponent(char symbol) {
			setSymbol(symbol);
		}

		protected void setSymbol(char _symbol) {
			symbol = "" + _symbol;
		}

		protected void setSymbol(String _symbol) {
			symbol = _symbol;
		}

		public String getSymbol() {
			return symbol;
		}
	};

	private void verifyComponentStart(Stack<LiteralComponent> componentStack, LiteralComponent componentToCheck,
			String literalName) throws ComponentMismatchException {
		if (componentStack.size() == 0) {
			if (componentToCheck.compareTo(LiteralComponent.NAME_START) > 0) throw new ComponentMismatchException(
					ErrorMessage.LITERAL_NAME_MISSING);
		} else {
			LiteralComponent lastComponent = componentStack.peek();
			if (lastComponent.equals(LiteralComponent.NAME_START)
					&& componentToCheck.compareTo(LiteralComponent.NAME_END) > 0) {
				if ("".equals(literalName)) throw new ComponentMismatchException(ErrorMessage.LITERAL_NAME_MISSING);
				lastComponent = LiteralComponent.NAME_END;
				componentStack.push(lastComponent);
			}
			if (lastComponent.ordinal() % 2 != 0) throw new ComponentMismatchException(
					ErrorMessage.LITERAL_COMPONENT_MISMATCH, new Object[] { "" + lastComponent.getSymbol() + ":::::"
							+ componentToCheck.getSymbol() });
			else if (lastComponent.compareTo(componentToCheck) >= 0) throw new ComponentMismatchException(
					ErrorMessage.LITERAL_COMPONENT_MISORDERED, new Object[] { componentToCheck.getSymbol() });
		}
		componentStack.push(componentToCheck);
	}

	private void verifyComponentEnd(Stack<LiteralComponent> componentStack, LiteralComponent componentToCheck,
			String literalName, String arguments) throws ComponentMismatchException {
		if (componentStack.size() == 0) throw new ComponentMismatchException(ErrorMessage.LITERAL_NAME_MISSING);
		else {
			LiteralComponent lastComponent = componentStack.peek();
			if (lastComponent.ordinal() % 2 == 0 && componentToCheck.equals(LiteralComponent.THEORY_FUNCTION_END)) {
				if ("".equals(literalName)) throw new ComponentMismatchException(ErrorMessage.LITERAL_NAME_MISSING);
			} else if (lastComponent.ordinal() % 2 == 0) {
				throw new ComponentMismatchException(ErrorMessage.LITERAL_COMPONENT_MISMATCH,
						new Object[] { componentToCheck.getSymbol() });
			} else if (lastComponent.ordinal() + 1 != componentToCheck.ordinal()) {
				throw new ComponentMismatchException(ErrorMessage.LITERAL_COMPONENT_MISMATCH,
						new Object[] { componentToCheck.getSymbol() });
			} else {
				switch (componentToCheck) {
				case NAME_END:
					if ("".equals(literalName)) throw new ComponentMismatchException(ErrorMessage.LITERAL_NAME_MISSING);
					break;
				case MODE_END:
					if ("".equals(arguments)) throw new ComponentMismatchException(
							ErrorMessage.LITERAL_MODE_ARGUMENT_MISSING);
					break;
				case TEMPORAL_END:
					if ("".equals(arguments)) throw new ComponentMismatchException(
							ErrorMessage.LITERAL_TEMPORAL_ARGUMENT_MISSING);
					break;
				default:
				}
			}
		}
		componentStack.push(componentToCheck);
	}

	private int getBalancedPredciateEndLocation(String literalString, int locStart) throws ComponentMismatchException {
		int bracketCount = 0;

		int currLoc = locStart;
		while (currLoc < literalString.length() && literalString.charAt(currLoc) != DflTheoryConst.PREDICATE_START) {
			currLoc++;
		}
		if (literalString.charAt(currLoc) != DflTheoryConst.PREDICATE_START) throw new ComponentMismatchException(
				"no start predicate found");
		do {
			switch (literalString.charAt(currLoc)) {
			case DflTheoryConst.PREDICATE_START:
				bracketCount++;
				break;
			case DflTheoryConst.PREDICATE_END:
				bracketCount--;
				break;
			default:
			}
			if (bracketCount == 0) return currLoc;
		} while (currLoc++ < literalString.length());
		throw new ComponentMismatchException("no end predicate found, literalString=" + literalString);
	}

	public List<String> parseLiteralString(final String literalString) throws ComponentMismatchException,
			ParserException {
		List<String> literals = new ArrayList<String>();
		if (null == literalString || "".equals(literalString.trim())) return literals;
		String literalStr = literalString.replaceAll("\\s\\r\\n", "");
		literalStr = literalStr.trim() + DflTheoryConst.LITERAL_SEPARATOR;

		String literalName = "", arguments = "";
		int literalStart = 0;
		int dollarSignCount = 0;
		Stack<LiteralComponent> componentStack = new Stack<LiteralComponent>();
		int i = 0;
		try {
			for (i = 0; i < literalStr.length(); i++) {
				char c = literalStr.charAt(i);
				LiteralComponent lc = (componentStack.size() == 0) ? null : componentStack.peek();
				switch (c) {
				case DflTheoryConst.LITERAL_BOOLEAN_FUNCTION_PREFIX:
					if (dollarSignCount++ % 2 == 0) {
						if (componentStack.size() > 0) throw new ComponentMismatchException(
								ErrorMessage.LITERAL_STRING_INCORRECT_FORMAT,
								new Object[] { "literal boolean fucntion should start at the beginning" });
						verifyComponentStart(componentStack, LiteralComponent.THEORY_FUNCTION_START, literalName);
					} else {
						if (componentStack.get(0).equals(LiteralComponent.THEORY_FUNCTION_START)) {
							if (lc.equals(LiteralComponent.NAME_START)) componentStack.push(LiteralComponent.NAME_END);
							verifyComponentEnd(componentStack, LiteralComponent.THEORY_FUNCTION_END, literalName,
									arguments);
						} else throw new ComponentMismatchException(ErrorMessage.LITERAL_STRING_INCORRECT_FORMAT,
								new Object[] { "literal boolean fucntion should start at the beginning" });
					}
					break;
				case DflTheoryConst.MODE_START:
					verifyComponentStart(componentStack, LiteralComponent.MODE_START, literalName);
					break;
				case DflTheoryConst.MODE_END:
					verifyComponentEnd(componentStack, LiteralComponent.MODE_END, literalName, arguments);
					componentStack.push(LiteralComponent.NAME_START);
					break;
				case DflTheoryConst.PREDICATE_START:
					verifyComponentStart(componentStack, LiteralComponent.PREDICATE_START, literalName);

					boolean abstractLiteralVariableStart = getAppConstants().containsAbstractLiteralInPredicate(
							literalName);
					if (abstractLiteralVariableStart) {
						int bracketEnd = getBalancedPredciateEndLocation(literalStr, i);
						String abstractLiteralPredicateString = literalStr.substring(i + 1, bracketEnd);
						List<String> pList = parseLiteralString(abstractLiteralPredicateString);
						for (int ii = 0; ii < pList.size(); ii++) {
							if (ii > 0) arguments += ",";
							arguments += pList.get(ii);
						}
						i = bracketEnd - 1;
					}
					break;
				case DflTheoryConst.PREDICATE_END:
					verifyComponentEnd(componentStack, LiteralComponent.PREDICATE_END, literalName, arguments);
					break;
				case DflTheoryConst.TIMESTAMP_START:
					verifyComponentStart(componentStack, LiteralComponent.TEMPORAL_START, literalName);
					break;
				case DflTheoryConst.TIMESTAMP_END:
					verifyComponentEnd(componentStack, LiteralComponent.TEMPORAL_END, literalName, arguments);
					break;
				case DflTheoryConst.LITERAL_SEPARATOR:
					if (componentStack.size() == 0 || lc.equals(LiteralComponent.NAME_START)
							|| lc.equals(LiteralComponent.PREDICATE_END) || lc.equals(LiteralComponent.TEMPORAL_END)
							|| lc.equals(LiteralComponent.THEORY_FUNCTION_END)) {
						if ("".equals(literalName) || literalName.equals("" + DflTheoryConst.LITERAL_VARIABLE_PREFIX)) throw new ComponentMismatchException(
								ErrorMessage.LITERAL_NAME_MISSING);
						String literal = literalStr.substring(literalStart, i);
						literals.add(literal);
						literalStart = i + 1;
						literalName = "";
						arguments = "";
						componentStack.clear();
					} else {
						switch (lc) {
						case MODE_START:
						case MODE_END:
						case NAME_END:
							throw new ComponentMismatchException(ErrorMessage.LITERAL_STRING_INCORRECT_FORMAT);
						case TEMPORAL_START:
							if (arguments.contains("" + DflTheoryConst.LITERAL_SEPARATOR)) throw new ComponentMismatchException(
									ErrorMessage.LITERAL_STRING_INCORRECT_FORMAT);
						default:
							if (componentStack.get(0).equals(LiteralComponent.THEORY_FUNCTION_START)
									&& (!(lc.equals(LiteralComponent.PREDICATE_START) || lc
											.equals(LiteralComponent.TEMPORAL_START)))) throw new ComponentMismatchException(
									ErrorMessage.LITERAL_STRING_INCORRECT_FORMAT, new Object[] { "1.2" });
							if ("".equals(arguments)) throw new ComponentMismatchException(
									ErrorMessage.LITERAL_TEMPORAL_ARGUMENT_MISSING);
						}
						arguments += c;
					}
					break;
				default:
					if (componentStack.size() == 0 || lc.equals(LiteralComponent.THEORY_FUNCTION_START)) componentStack
							.push(LiteralComponent.NAME_START);
					if (componentStack.peek().equals(LiteralComponent.NAME_START)) literalName += c;
					else arguments += c;
				}
			}
		} catch (ComponentMismatchException e) {
			System.out.println(literals.toString());
			System.out.println("literalString=" + literalString);
			System.out.println("literalName=" + literalName);
			System.out.println("componentStack=" + componentStack);
			System.out.println("arguments=" + arguments);
			int ex = i;
			if (ex > literalStr.length()) ex = literalStr.length();
			throw new ParserException(literalStr + ", i=" + i + ", c=" + literalStr.charAt(i), e);
		}
		return literals;
	}

	public synchronized String formalizeLiteralBooleanFunctionString(final String literalVariableStr,
			Map<LiteralVariable, LiteralVariable> literalVariableMapping,
			Map<LiteralVariable, String> literalBooleanFunctionAnswers, boolean isSubstituteWithJavaCode)
			throws ParserException {
		if (null == literalVariableStr || "".equals(literalVariableStr.trim())) return "";
		String originalStr = literalVariableStr.replaceAll("\\s", "");
		int l = originalStr.length();
		if (originalStr.charAt(0) != DflTheoryConst.LITERAL_BOOLEAN_FUNCTION_PREFIX) throw new ParserException(
				ErrorMessage.LITERAL_BOOLEAN_FUNCTION_PREFIX_MISMATCH, new Object[] { ""
						+ DflTheoryConst.LITERAL_BOOLEAN_FUNCTION_PREFIX });
		if (originalStr.charAt(l - 1) != DflTheoryConst.LITERAL_BOOLEAN_FUNCTION_POSTFIX) throw new ParserException(
				ErrorMessage.LITERAL_BOOLEAN_FUNCTION_POSTFIX_MISMATCH, new Object[] { ""
						+ DflTheoryConst.LITERAL_BOOLEAN_FUNCTION_POSTFIX });
		try {
			List<String> tokens = tokenizeBooleanFunctionString(originalStr, 0, literalVariableMapping,
					literalBooleanFunctionAnswers, isSubstituteWithJavaCode, 0);
			StringBuilder sb = new StringBuilder();
			for (String s : tokens) {
				sb.append(s);
			}
			return sb.toString();
		} catch (ParserException e) {
			throw e;
		} catch (Exception e) {
			throw new ParserException(ErrorMessage.LITERAL_BOOLEAN_FUNCTION_COMPONENT_MISMATCH,
					new String[] { originalStr }, e);
		}
	}

	private static final String DUMMY_START = "(.*)(";
	private static final String DUMMY_END = ")(.*)";

	private static final String PATTERN_BRACKETS = "(\\$(.*)\\$|\\((.*)\\)|\\[(.*)]|\\{(.*)})";
	private static final Pattern patternOutterBrackets = Pattern.compile("^" + PATTERN_BRACKETS + "$");

	private static final String PATTERN_OR_AND_COMPARATORS = "\\|\\||&&|==|!=|\\>=|\\<=";
	private static final Pattern patternOutterOrAndComparators = Pattern.compile(DUMMY_START
			+ PATTERN_OR_AND_COMPARATORS + DUMMY_END);

	private static final String PATTERH_MATH_OPERATORS1 = "([^\\>\\<]*)([\\>\\<&&[^,-]])(.*)";
	private static final Pattern patternMathOperators1 = Pattern.compile(PATTERH_MATH_OPERATORS1);

	private static final String PATTERH_MATH_OPERATORS2 = "([^\\+/\\*\\|!]*)([&\\+/\\*\\|!%&&[^,]])(.*)";
	private static final Pattern patternMathOperators2 = Pattern.compile(PATTERH_MATH_OPERATORS2);

	private static final String PATTERH_MATH_OPERATORS3 = "([^-]*)(-)(.*)";
	private static final Pattern patternMathOperators3 = Pattern.compile(PATTERH_MATH_OPERATORS3);

	private static final String LEFT_ITEM_EXCEMPTED_OPERATORS = "-!";

	public synchronized List<String> tokenizeBooleanFunctionString(final String origStr, int startLoc,
			Map<LiteralVariable, LiteralVariable> literalVariableMapping,
			Map<LiteralVariable, String> literalBooleanFunctionAnswers, boolean isSubstituteWithJavaCode, int level)
			throws ParserException {
		List<String> tokens = new ArrayList<String>();
		if (null == origStr || "".equals(origStr)) return tokens;
		List<String> tTokens = null, ttTokens = null;
		if ((tTokens = extractPatternString(origStr, patternOutterBrackets)) != null) {
			if ((ttTokens = verifyBracketTokens(tTokens)) != null) {
				tokens.add(ttTokens.get(0));
				String item = ttTokens.get(1);
				if ("".equals(item)) throw new ParserException(
						ErrorMessage.LITERAL_BOOLEAN_FUNCTION_COMPONENT_MISMATCH, new Object[] { origStr });
				tokens.addAll(tokenizeBooleanFunctionString(item, startLoc + tTokens.get(0).length(),
						literalVariableMapping, literalBooleanFunctionAnswers, isSubstituteWithJavaCode, level + 1));
				tokens.add(ttTokens.get(2));
			} else throw new ParserException(ErrorMessage.LITERAL_BOOLEAN_FUNCTION_RIGHT_ITEM_MISSING, new Object[] {
					"" + origStr.charAt(0), "" + (startLoc + 1) });
		} else {
			if ((tTokens = extractPatternString(origStr, patternOutterOrAndComparators)) != null) {
				// System.out.println("p1");
			} else if ((tTokens = extractPatternString(origStr, patternMathOperators1)) != null) {
				// System.out.println("p2");
			} else if ((tTokens = extractPatternString(origStr, patternMathOperators2)) != null) {
				// System.out.println("p3");
			} else {
				// System.out.println("p4");
				tTokens = extractPatternString(origStr, patternMathOperators3);
			}
			if (null != tTokens) {
				String leftItem = tTokens.get(1);
				String operator = tTokens.get(2);
				String rightItem = tTokens.get(3);

				if ("".equals(leftItem)) {
					if (LEFT_ITEM_EXCEMPTED_OPERATORS.contains(operator)) {
						if (operator.equals("-")) {
							operator = "";
							rightItem = DflTheoryConst.SYMBOL_NEGATION + rightItem;
						}
					} else {
						throw new ParserException(ErrorMessage.LITERAL_BOOLEAN_FUNCTION_LEFT_ITEM_MISSING,
								new Object[] { operator, "" + startLoc });
					}
				}
				int lLoc = startLoc;
				int rLoc = startLoc + leftItem.length() + operator.length();
				if ("".equals(rightItem)) throw new ParserException(
						ErrorMessage.LITERAL_BOOLEAN_FUNCTION_RIGHT_ITEM_MISSING, new Object[] { operator, "" + rLoc });
				tokens.addAll(tokenizeBooleanFunctionString(leftItem, lLoc, literalVariableMapping,
						literalBooleanFunctionAnswers, isSubstituteWithJavaCode, level + 1));
				if ("".equals(operator)) {
					try {
						tokens.add(generateFunctionString(rightItem, literalVariableMapping));
					} catch (ParserException e) {
						throw e;
					} catch (Exception e) {
						tokens.add(rightItem);
					}
				} else {
					tokens.add(operator);
					tokens.addAll(tokenizeBooleanFunctionString(rightItem, rLoc, literalVariableMapping,
							literalBooleanFunctionAnswers, isSubstituteWithJavaCode, level + 1));
				}
			} else {
				try {
					tokens.add(generateFunctionString(origStr, literalVariableMapping));
				} catch (ParserException e) {
					throw e;
				} catch (Exception e) {
					tokens.add(origStr);
				}
			}
		}
		return tokens;
	}

	private String generateFunctionString(String origStr,//
			Map<LiteralVariable, LiteralVariable> literalVariableMapping) throws ParserException {
		try {
			LiteralVariable lv = DflTheoryParser2.extractLiteralVariable(origStr);
			if (null != literalVariableMapping && literalVariableMapping.containsKey(lv)) {
				lv = literalVariableMapping.get(lv);
			} else if (getAppConstants().isAppConstant(lv)) lv = getAppConstants().getAppConstantAsLiteralVariable(lv);

			return lv.toString();
		} catch (ParserException e) {
			throw e;
		} catch (Exception e) {
			throw new ParserException(e);
		}
	}

	private List<String> verifyBracketTokens(List<String> tokens) throws ParserException {
		List<String> newTokens = new ArrayList<String>();
		int bracketType = -1;
		String[] brackets = null;
		for (int i = 2; i < tokens.size() && bracketType < 0; i++) {
			if (!"".equals(tokens.get(i))) bracketType = i;
		}
		switch (bracketType) {
		case 2:
			brackets = new String[] { "$", "$" };
			break;
		case 3:
			brackets = new String[] { "(", ")" };
			break;
		case 4:
			brackets = new String[] { "[", "]" };
			break;
		case 5:
			brackets = new String[] { "{", "}" };
			break;
		default:
			return null;
		}
		String tokenString = tokens.get(bracketType);
		newTokens.add(brackets[0]);
		newTokens.add(tokenString);
		newTokens.add(brackets[1]);
		return newTokens;
	}

	private List<String> extractPatternString(String str, Pattern pattern) {
		Matcher matcher = pattern.matcher(str);
		if (!matcher.find()) return null;
		List<String> tokens = new ArrayList<String>();
		do {
			for (int i = 0; i < matcher.groupCount() + 1; i++) {
				if (null == matcher.group(i)) tokens.add("");
				else tokens.add(matcher.group(i));
			}
		} while (matcher.find());
		return tokens;
	}

	/**
	 * @param str
	 * @return a formatted string for theory application constant if the inputed string represent a system defined
	 *         application constant; or the original string otherwise
	 * @throws InvalidArgumentException
	 */
	public String formatLiteralVariableString(final String str) throws InvalidArgumentException {
		if (null == str || "".equals(str.trim())) throw new InvalidArgumentException(
				ErrorMessage.LITERAL_VARIABLE_DEFINITION_NOT_FOUND);
		if (str.charAt(0) == DomConst.Literal.LITERAL_VARIABLE_PREFIX
				|| str.charAt(0) == DomConst.Literal.LITERAL_BOOLEAN_FUNCTION_PREFIX) return str;

		AppConstant appConstant = null;
		try {
			Integer.parseInt(str);
			appConstant = getAppConstants().getAppConstant(Val.LABEL);
		} catch (Exception e) {
			try {
				Converter.timeString2long(str);
				appConstant = getAppConstants().getAppConstant(Duration.LABEL);
			} catch (Exception e1) {
			}
		}
		if (null == appConstant) return str;
		String newStr = appConstant.getLabel() + Literal.PREDICATE_START + str + Literal.PREDICATE_END;
		return newStr;
	}
}
