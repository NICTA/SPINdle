/**
 * SPINdle (version 2.2.2)
 * Copyright (C) 2009-2013 NICTA Ltd.
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
import spindle.console.ConsoleException;
import spindle.console.UnrecognizedCommandException;
import spindle.core.dom.Conclusion;
import spindle.core.dom.LiteralVariable;
import spindle.core.dom.RuleType;
import spindle.core.dom.Superiority;
import spindle.core.dom.Theory;
import spindle.core.dom.TheoryException;
import spindle.io.ParserException;
import spindle.io.outputter.DflTheoryConst;
import spindle.io.parser.DflTheoryParser2;
import spindle.sys.ConfigurationException;
import spindle.sys.IncorrectNoOfArgumentsException;
import spindle.sys.message.ErrorMessage;

/**
 * Console command: remove.
 * <p>
 * Remove a fact, rule, superiority relation or mode conversion/conflict rules from the theory.
 * </p>
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.0.0
 * @since 2011.07.27
 * @version Last modified 2012.07.30
 */
public class Remove extends CommandBase {
	public static final String COMMAND_NAME = "remove";
	public static final String DESCRIPTION = "Remove a rule, superiority relation, or mode conversion rule from theory";
	public static final String USAGE = "remove rule [rule id]\nremove superiority [superiority]\nremove mode [mode rule]";

	public Remove() {
		super(COMMAND_NAME, DESCRIPTION, USAGE);
		addOption(new CommandOption("literalVariabel", "[literal variable name]",
				"Remove a literal variable from theory"));
		addOption(new CommandOption("fact", "[rule id]", "Remove a fact from theory"));
		addOption(new CommandOption("rule", "[rule id]", "Remove a rule from theory"));
		addOption(new CommandOption("superiority", "[superiority]", "Remove a superiority relation from theory"));
		addOption(new CommandOption("mode", "[mode rule]", "Remove a mode conversion/conflict rule from theory"));
	}

	@Override
	public Object execute(Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		throw new IncorrectNoOfArgumentsException(COMMAND_NAME, 1);
	}

	@Override
	public Object execute(String option, Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		if (null == theory) throw new ConsoleException(COMMAND_NAME, ErrorMessage.THEORY_NULL_THEORY, ".  Do nothing!");
		if (theory.isEmpty()) throw new ConsoleException(COMMAND_NAME, ErrorMessage.THEORY_EMPTY_THEORY,
				".  Do nothing!");

		if (args.size() == 0) throw new IncorrectNoOfArgumentsException(COMMAND_NAME, 1);
		String o = getOptionName(option);

		StringBuilder sb = new StringBuilder();
		for (String arg : args) {
			sb.append(arg).append(" ");
		}
		String arg = sb.toString().trim();
		try {
			if ("literalVariable".equals(o)) {
				removeLiteralVariable(theory, arg);
			} else if ("rule".equals(o) || "fact".equals(o)) {
				removeRule(theory, arg);
			} else if ("superiority".equals(o)) {
				removeSuperiority(theory, arg);
			} else if ("mode".equals(o)) {
				removeMode(theory, arg);
			} else {
				throw new UnrecognizedCommandException(COMMAND_NAME + ", option [" + option + "] not found!");
			}
		} catch (UnrecognizedCommandException e) {
			throw e;
		} catch (Exception e) {
			out.println(e.getMessage());
			throw new ConsoleException(COMMAND_NAME, ErrorMessage.CONSOLE_ERROR_MESSAGE, e);
		}
		return theory;
	}

	private void removeLiteralVariable(final Theory theory, final String literalVariableName) throws ParserException,
			TheoryException {
		LiteralVariable literalVariable = DflTheoryParser2.extractLiteralVariable(literalVariableName);
		if (theory.getLiteralVariables().containsKey(literalVariable)) theory.removeLiteralVariable(literalVariable);
	}

	private void removeRule(final Theory theory, final String ruleLabel) throws TheoryException {
		if (theory.containsRuleLabel(ruleLabel)) theory.removeRule(ruleLabel);
		else throw new TheoryException("rule [" + ruleLabel + "] does not exist in theory");
	}

	private void removeMode(final Theory theory, final String str) throws ParserException {
		int l = -1;
		if ((l = str.indexOf(DflTheoryConst.SYMBOL_MODE_CONVERSION)) > 0) {
			try {
				String o = str.substring(0, l).trim();
				String[] c = str.substring(l + DflTheoryConst.SYMBOL_MODE_CONVERSION.length()).split(
						"" + DflTheoryConst.LITERAL_SEPARATOR);
				for (String convertMode : c) {
					out.print("remove mode conversion rule: [" + o + "]" + DflTheoryConst.SYMBOL_MODE_CONVERSION + " "
							+ convertMode + " ...");
					theory.removeModeConversionRule(o, convertMode);
					out.println("...success");
				}
			} catch (Exception e) {
				throw new ParserException(e);
			}
		} else if ((l = str.indexOf(DflTheoryConst.SYMBOL_MODE_CONFLICT)) > 0) {
			try {
				String o = str.substring(0, l).trim();
				String[] c = str.substring(l + DflTheoryConst.SYMBOL_MODE_CONVERSION.length()).split(
						"" + DflTheoryConst.LITERAL_SEPARATOR);
				for (String conflictMode : c) {
					out.print("remove mode conflict rule: [" + o + "]" + DflTheoryConst.SYMBOL_MODE_CONFLICT + " "
							+ conflictMode + " ...");
					theory.removeModeConflictRule(o, conflictMode);
					out.println("...success");
				}
			} catch (Exception e) {
				throw new ParserException(e);
			}
		} else {
			throw new ParserException(ErrorMessage.RULE_UNRECOGNIZED_RULE_TYPE, new Object[] { str });
		}
	}

	private void removeSuperiority(final Theory theory, final String str) throws ParserException {
		int l = str.indexOf(RuleType.SUPERIORITY.getSymbol());
		String superior = str.substring(0, l).trim();
		String inferior = str.substring(l + RuleType.SUPERIORITY.getSymbol().length()).trim();

		if ("".equals(superior)) throw new ParserException(ErrorMessage.SUPERIORITY_SUPERIOR_RULE_NOT_DEFINED, str);
		if ("".equals(inferior)) throw new ParserException(ErrorMessage.SUPERIORITY_INFERIOR_RULE_NOT_DEFINED, str);

		Superiority sup = new Superiority(superior, inferior);
		out.print("remove superiority relation: " + sup + " from theory...");
		theory.remove(sup);
		out.println("..success");
	}

}
