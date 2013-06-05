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

import com.app.utils.Converter;

import spindle.Reasoner;
import spindle.console.ConsoleException;
import spindle.core.ReasonerException;
import spindle.core.dom.Conclusion;
import spindle.core.dom.Theory;
import spindle.engine.ReasoningEngineFactoryException;
import spindle.sys.AppConst;
import spindle.sys.Conf;
import spindle.sys.ConfigurationException;
import spindle.sys.Messages;
import spindle.sys.message.ErrorMessage;
import spindle.sys.message.SystemMessage;

/**
 * Console application command: conclusion.
 * <p>
 * Derive conclusions from defeasible theory
 * </p>
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.0.0
 * @since 2011.07.27
 * @version Last modified 2012.07.30
 */
public class Conclusions extends CommandBase {
	public static final String COMMAND_NAME = "conclusions";
	public static final String DESCRIPTION = "Derive conclusions from theory";
	public static final String USAGE = "conclusions";

	public Conclusions() {
		super(COMMAND_NAME, DESCRIPTION, USAGE);
	}

	@Override
	public Object execute(Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		if (null == theory) throw new ConsoleException(COMMAND_NAME, ErrorMessage.THEORY_NULL_THEORY);
		if (theory.isEmpty()) throw new ConsoleException(COMMAND_NAME, ErrorMessage.THEORY_EMPTY_THEORY);
		if (theory.getFactsCount() > 0) throw new ConsoleException(COMMAND_NAME,
				ErrorMessage.THEORY_NOT_IN_REGULAR_FORM_CONTAINS_FACT);
		if (theory.getDefeatersCount() > 0) throw new ConsoleException(COMMAND_NAME,
				ErrorMessage.THEORY_NOT_IN_REGULAR_FORM_CONTAINS_DFEATER);

		switch (Conf.getReasonerVersion()) {
		case 1:
			if (theory.getSuperiorityCount() > 0) throw new ConsoleException(COMMAND_NAME,
					ErrorMessage.THEORY_NOT_IN_REGULAR_FORM_CONTAINS_SUPERIORITY_RELATION);
			break;
		default:
		}
		try {
			return generateConclusions(theory.clone());
		} catch (Exception e) {
			throw new ConsoleException(COMMAND_NAME, ErrorMessage.CONSOLE_ERROR_MESSAGE, e.getMessage());
		}
	}

	@Override
	public Object execute(String option, Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		return execute(theory, conclusions, args);
	}

	private List<Conclusion> generateConclusions(final Theory theory) throws ReasoningEngineFactoryException,
			ReasonerException {
		long startTime = 0, endTime = 0;
		List<Conclusion> conclusions = null;
		try {
			out.println("derive conclusions from theory...");
			Reasoner reasoner = new Reasoner();
			reasoner.loadTheory(theory);
			startTime = System.currentTimeMillis();
			reasoner.getConclusions();
			endTime = System.currentTimeMillis();
			conclusions = reasoner.getConclusionsAsList();
			out.println("derive conclusions from theory......success");
		} catch (ReasonerException e) {
			out.println(Messages.getSystemMessage(SystemMessage.APPLICATION_OPERATION_FAILED));
			out.println(e.getMessage());
			throw e;
		}
		if (Conf.isShowProgress()) {
			StringBuilder sb = new StringBuilder();
			sb.append("\n").append(Messages.getSystemMessage(SystemMessage.REASONER_CONCLUSIONS_GENERATED))
					.append("\n===========");
			for (Conclusion conclusion : conclusions) {
				sb.append(LINE_SEPARATOR).append(AppConst.IDENTATOR).append(conclusion.toString());
			}
			out.println(sb.toString());
		}

		out.println(Messages.getSystemMessage(SystemMessage.REASONER_TIME_USED)
				+ Converter.long2TimeString(endTime - startTime));
		return conclusions;
	}

}
