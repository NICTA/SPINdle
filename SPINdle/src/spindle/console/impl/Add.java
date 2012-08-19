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
package spindle.console.impl;

import java.util.List;

import spindle.console.CommandOption;
import spindle.console.CommandOptionException;
import spindle.console.ConsoleException;
import spindle.core.dom.Conclusion;
import spindle.core.dom.LiteralVariable;
import spindle.core.dom.Rule;
import spindle.core.dom.RuleType;
import spindle.core.dom.Superiority;
import spindle.core.dom.Theory;
import spindle.core.dom.TheoryException;
import spindle.io.ParserException;
import spindle.io.outputter.DflTheoryConst;
import spindle.io.parser.DflTheoryParser2;
import spindle.sys.ConfigurationException;
import spindle.sys.IncorrectNoOfArgumentsException;
import spindle.sys.Messages;
import spindle.sys.message.ErrorMessage;
import spindle.sys.message.SystemMessage;

/**
 * Console command: add.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.0.0
 * @since 2011.07.27
 * @version Last modified 2012.07.30
 */
public class Add extends CommandBase {
	public static final String COMMAND_NAME = "add";
	public static final String DESCRIPTION = "Add new fact, rule, superiority relation and mode conversion/conflict rule to theory.";
	public static final String USAGE = "add [fact | rule | superiority relation | mode rule | literal variable | boolean function]";

	public Add() {
		super(COMMAND_NAME, DESCRIPTION, USAGE);
		addOption(new CommandOption("", "[fact]", "Add new fact to theory"));
		addOption(new CommandOption("", "[rule]", "Add new rule to theory"));
		addOption(new CommandOption("", "[superiority]", "Add new superiority relations to theory"));
		addOption(new CommandOption("", "[mode rule]", "Add new mode conversion/conflict rule to theory"));
		addOption(new CommandOption("", "[literal variable]", "Add literal variable to theory"));
		addOption(new CommandOption("", "[literal boolean function]", "Add literal boolean function to theory"));
	}

	@Override
	public Object execute(Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		if (args.size() == 0) throw new IncorrectNoOfArgumentsException(0);
		Theory newTheory = (null == theory) ? new Theory() : theory;
		StringBuilder sb = new StringBuilder();
		for (String arg : args) {
			sb.append(arg).append(" ");
		}
		String arg = sb.toString().trim();
		try {
			RuleType ruleType = getRuleType_dfl(arg);
			System.out.println(ruleType.getLabel());
			switch (ruleType) {
			case LITERAL_VARIABLE_SET:
				addLiteralVariable(newTheory, arg);
				break;
			case FACT:
			case STRICT:
			case DEFEASIBLE:
			case DEFEATER:
				addRule(newTheory, arg);
				break;
			case SUPERIORITY:
				addSuperiority(newTheory, arg);
				break;
			case MODE_CONVERSION:
			case MODE_CONFLICT:
				addModeConversionAndModeRuleConflict(newTheory, arg);
				break;
			default:
				throw new CommandOptionException(COMMAND_NAME,ErrorMessage.CONSOLE_COMMAND_NULL_OPTION_INFORMATION);
			}
			return newTheory;
		}catch (ConsoleException e){throw e;
		} catch (Exception e) {
			throw new ConsoleException(COMMAND_NAME, ErrorMessage.CONSOLE_ERROR_MESSAGE, e.getMessage());
		}
	}

	@Override
	public Object execute(String option, Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		return execute(theory, conclusions, args);
	}

	private void addLiteralVariable(final Theory theory, final String str) throws ParserException {
		String code = str.substring(RuleType.LITERAL_VARIABLE_SET.getSymbol().length()).replaceAll("\\s", "");
		int l = code.indexOf(DflTheoryConst.THEORY_EQUAL_SIGN);
		if (l < 0) throw new ParserException(ErrorMessage.LITERAL_VARIABLE_DEFINITION_NOT_FOUND);

		try {
			out.print("set literal variable: " + code + " ...");
			String nameStr = code.substring(0, l).trim();
			String valueStr = code.substring(l + 1);

			LiteralVariable lvName = DflTheoryParser2.extractLiteralVariable(nameStr);
			LiteralVariable lvValue = DflTheoryParser2.extractLiteralVariable(valueStr);
			theory.addLiteralVariable(lvName, lvValue);
			printConsoleMessage(SystemMessage.APPLICATION_OPERATION_SUCCESS);
		} catch (ParserException e) {
			printConsoleMessage(SystemMessage.APPLICATION_OPERATION_FAILED);
			throw e;
		} catch (Exception e) {
			printConsoleMessage(SystemMessage.APPLICATION_OPERATION_FAILED);
			throw new ParserException(e);
		}
	}

	private void addSuperiority(final Theory theory, final String str) throws ParserException {
		try {
			Superiority sup = DflTheoryParser2.extractSuperiorityStr(str);
			out.print("add superiority relation: " + sup + " to theory...");
			theory.add(sup);
			printConsoleMessage(SystemMessage.APPLICATION_OPERATION_SUCCESS);
		} catch (ParserException e) {
			printConsoleMessage(SystemMessage.APPLICATION_OPERATION_FAILED);
			throw e;
		} catch (Exception e) {
			printConsoleMessage(SystemMessage.APPLICATION_OPERATION_FAILED);
			throw new ParserException(e);
		}
	}

	private void addModeConversionAndModeRuleConflict(final Theory theory, final String str) throws ParserException {
		int l = -1;
		if ((l = str.indexOf(DflTheoryConst.SYMBOL_MODE_CONVERSION)) > 0) {
			String o = str.substring(0, l).trim();
			String c = str.substring(l + DflTheoryConst.SYMBOL_MODE_CONVERSION.length()).trim();
			if ("".equals(c)) throw new ParserException(ErrorMessage.CONSOLE_EMPTY_CONVERT_MODE, str);
			out.print(Messages.getSystemMessage(SystemMessage.CONSOLE_ADD_MODE_CONVERSION_RULE, new Object[]{o,c}));
//			out.print("add mode conversion rule: [" + o + "]" + DflTheoryConst.SYMBOL_MODE_CONVERSION + "[" + c
//					+ "] ...");
			theory.addModeConversionRules(o, c.split("" + DflTheoryConst.LITERAL_SEPARATOR));
			printConsoleMessage(SystemMessage.APPLICATION_OPERATION_SUCCESS);
		} else if ((l = str.indexOf(DflTheoryConst.SYMBOL_MODE_CONFLICT)) > 0) {
			String o = str.substring(0, l).trim();
			String c = str.substring(l + DflTheoryConst.SYMBOL_MODE_CONVERSION.length()).trim();
			if ("".equals(c)) throw new ParserException(ErrorMessage.CONSOLE_EMPTY_CONFLICT_MODE, str);
			out.print(Messages.getSystemMessage(SystemMessage.CONSOLE_ADD_MODE_CONFLICT_RULE, new Object[]{o,c}));
//			out.print("add mode conflict rule: [" + o + "]" + DflTheoryConst.SYMBOL_MODE_CONFLICT + "[" + c + "] ...");
			theory.addModeConflictRules(o, c.split("" + DflTheoryConst.LITERAL_SEPARATOR));
			printConsoleMessage(SystemMessage.APPLICATION_OPERATION_SUCCESS);
		} else {
			out.println(Messages.getSystemMessage(SystemMessage.APPLICATION_OPERATION_FAILED));
			throw new ParserException(ErrorMessage.RULE_UNRECOGNIZED_RULE_TYPE, null == str ? "" : str.trim());
		}
	}

	private void addRule(final Theory theory, final String str) throws ParserException, TheoryException {
		Rule rule = DflTheoryParser2.extractRuleStr(str);
		String ruleLabel = rule.getLabel();
		if (ruleLabel.startsWith(Theory.DEFAULT_RULE_LABEL_PREFIX)) {
			ruleLabel = theory.getUniqueRuleLabel();
			rule.setLabel(ruleLabel);
			printConsoleMessage(SystemMessage.THEORY_ADD_NEW_RULE, new Object[] { rule }, false);
		} else if (theory.containsRuleLabel(ruleLabel)) {
			printConsoleMessage(SystemMessage.THEORY_REPLACE_RULE_IN_THEORY, new Object[] { ruleLabel, rule }, false);
			theory.removeRule(ruleLabel);
		}
		theory.addRule(rule);
		printConsoleMessage(SystemMessage.APPLICATION_OPERATION_SUCCESS);
	}

	protected RuleType getRuleType_dfl(String str) throws ParserException {
		for (RuleType ruleType : RuleType.values()) {
			if (str.indexOf(ruleType.getSymbol()) >= 0) return ruleType;
		}
		throw new ParserException(ErrorMessage.RULE_UNRECOGNIZED_RULE_TYPE, null == str ? "" : str.trim());
	}
}
