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
package spindle.console.impl;

import java.util.List;

import com.app.utils.Converter;

import spindle.console.CommandOption;
import spindle.console.ConsoleException;
import spindle.console.UnrecognizedCommandException;
import spindle.core.dom.Conclusion;
import spindle.core.dom.Theory;
import spindle.engine.ReasoningEngineFactory;
import spindle.engine.ReasoningEngineFactoryException;
import spindle.engine.TheoryNormalizer;
import spindle.engine.TheoryNormalizerException;
import spindle.sys.Conf;
import spindle.sys.ConfigurationException;
import spindle.sys.message.ErrorMessage;

/**
 * Console command: transform.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.0.0
 * @since 2011.07.27
 * @version Last modified 2012.07.30
 */
public class Transform extends CommandBase {
	public static final String COMMAND_NAME = "transform";
	public static final String DESCRIPTION = "Transform theory to regular form, or to an equivalent theory without superiority relations or defeaters.";
	public static final String USAGE = "transform regular\ntransform defeater\ntransform superiority";

	public Transform() {
		super(COMMAND_NAME, DESCRIPTION, USAGE);
		addOption(new CommandOption("regular", "", "Transform theory to regular form"));
		addOption(new CommandOption("defeater", "", "Remove defeaters from theory"));
		addOption(new CommandOption("superiority", "", "Remove superiority relations from theory"));
	}

	@Override
	public Object execute(Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		throw new ConsoleException(COMMAND_NAME, ErrorMessage.CONSOLE_NOTHING_TO_EXECUTE);
	}

	@Override
	public Object execute(String option, Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		if (null == theory) throw new ConsoleException(COMMAND_NAME, ErrorMessage.THEORY_NULL_THEORY);
		if (theory.isEmpty()) throw new ConsoleException(COMMAND_NAME, ErrorMessage.THEORY_EMPTY_THEORY);

		String o = getOptionName(option);
		long timeUsed = 0;
		try {
			if ("regular".equals(o)) {
				out.print("transform theory to regular form...");
				timeUsed = transformToRegularForm(theory);
			} else if ("defeater".equals(o)) {
				if (theory.getDefeatersCount() == 0) {
					out.println("no defeater exist in theory.\n=> command cancelled");
				} else {
					out.print("remove defeaters from theory...");
					timeUsed = removeDefeater(theory);
				}
			} else if ("superiority".equals(o)) {
				if (theory.getSuperiorityCount() == 0) {
					out.println("no defeater exist in theory.\n=> command cancelled");
				} else {
					out.print("remove superiority relations from theory...");
					timeUsed = removeSuperiorityRelations(theory);
				}
			} else {
				throw new UnrecognizedCommandException(COMMAND_NAME + ", option [" + option + "] not found!");
			}
			out.println("...success");
			if (Conf.isShowProgress()) out.println(theory.toString());
		} catch (UnrecognizedCommandException e) {
			throw e;
		} catch (Exception e) {
			out.println("...failed");
			out.println(e.getMessage());
			throw new ConsoleException(COMMAND_NAME, ErrorMessage.CONSOLE_ERROR_MESSAGE, e.getMessage());
		}
		out.println("time used=" + Converter.long2TimeString(timeUsed));
		return theory;
	}

	private long transformToRegularForm(final Theory theory) throws ReasoningEngineFactoryException,
			TheoryNormalizerException {
		TheoryNormalizer normalizer = ReasoningEngineFactory.getTheoryNormalizer(theory.getTheoryType());
		normalizer.setTheory(theory);
		long startTime = System.currentTimeMillis();
		normalizer.transformTheoryToRegularForm();
		return (System.currentTimeMillis() - startTime);
	}

	private long removeDefeater(final Theory theory) throws ReasoningEngineFactoryException, TheoryNormalizerException {
		TheoryNormalizer normalizer = ReasoningEngineFactory.getTheoryNormalizer(theory.getTheoryType());
		normalizer.setTheory(theory);
		long startTime = System.currentTimeMillis();
		normalizer.removeDefeater();
		return (System.currentTimeMillis() - startTime);
	}

	private long removeSuperiorityRelations(final Theory theory) throws ReasoningEngineFactoryException,
			TheoryNormalizerException {
		TheoryNormalizer normalizer = ReasoningEngineFactory.getTheoryNormalizer(theory.getTheoryType());
		normalizer.setTheory(theory);
		long startTime = System.currentTimeMillis();
		normalizer.removeSuperiority();
		return (System.currentTimeMillis() - startTime);
	}
}
