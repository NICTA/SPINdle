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
import java.util.TreeSet;
import java.util.logging.Level;

import spindle.console.CommandOption;
import spindle.console.ConsoleException;
import spindle.console.UnrecognizedCommandException;
import spindle.core.dom.Conclusion;
import spindle.core.dom.Theory;
import spindle.sys.Conf;
import spindle.sys.ConfTag;
import spindle.sys.ConfigurationException;
import spindle.sys.IncorrectNoOfArgumentsException;
import spindle.sys.message.ErrorMessage;

/**
 * Console command: set.
 * <p>
 * Set the reasoner properties values
 * </p>
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.0.0
 * @since 2011.07.27
 * @version Last modified 2012.07.30
 */
public class Set extends CommandBase {
	public static final String COMMAND_NAME = "set";
	public static final String DESCRIPTION = "Set the value of the following properties:" //
			+ "\n" + ConfTag.IS_SHOW_PROGRESS //
			+ "\n" + ConfTag.IS_SHOW_RESULT //
			+ "\n" + ConfTag.IS_SAVE_RESULT //
			+ "\n" + ConfTag.LOG_FOLDER //
			+ "\n" + ConfTag.LOG_LEVEL //
			+ "\n" + ConfTag.REASONER_VERSION //
			+ "\n" + ConfTag.REASONER_AMBIGUOUS_PROPAGATION //
			+ "\n" + ConfTag.REASONER_WELL_FOUNDED_SEMANTICS //
	;
	public static final String USAGE = "set [property name]=[property value]";

	public static final java.util.Set<String> properties = new TreeSet<String>();
	static {
		properties.add(ConfTag.IS_SHOW_PROGRESS);
		properties.add(ConfTag.IS_SHOW_RESULT);
		properties.add(ConfTag.IS_SAVE_RESULT);
		properties.add(ConfTag.LOG_FOLDER);
		properties.add(ConfTag.LOG_LEVEL);
		properties.add(ConfTag.REASONER_VERSION);
		properties.add(ConfTag.REASONER_AMBIGUOUS_PROPAGATION);
		properties.add(ConfTag.REASONER_WELL_FOUNDED_SEMANTICS);
	}

	public Set() {
		super(COMMAND_NAME, DESCRIPTION, USAGE);
		addOption(new CommandOption("", "[name]=[value]", "Set environment variables"));
	}

	@Override
	public Object execute(Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		if (args.size() < 1) throw new IncorrectNoOfArgumentsException(COMMAND_NAME, 1);
		StringBuilder sb = new StringBuilder();
		for (String arg : args) {
			sb.append(arg).append(" ");
		}
		String arg = sb.toString();
		int l = arg.indexOf("=");
		if (l <= 0) throw new IncorrectNoOfArgumentsException(COMMAND_NAME, ">0");
		String propertyName = arg.substring(0, l).trim();
		String propertyValue = arg.substring(l + 1).trim();

		String v = setProperty(propertyName, propertyValue);
		out.println("property [" + propertyName + "]=[" + v + "]");
		return null;
	}

	@Override
	public Object execute(String option, Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		return execute(theory, conclusions, args);
	}

	private String setProperty(final String propertyName, final String propertyValue) throws ConfigurationException,
			ConsoleException {
		if (!properties.contains(propertyName)) throw new UnrecognizedCommandException(COMMAND_NAME + ", property ["
				+ propertyName + "] not found");
		if (ConfTag.LOG_LEVEL.equals(propertyName)) {
			try {
				Level level = Level.parse(propertyValue);
				Conf.setLogLevel(level);
				return level.toString();
			} catch (Exception e) {
				out.println(e.getMessage());
				throw new ConsoleException(COMMAND_NAME, ErrorMessage.CONSOLE_ERROR_MESSAGE, e.getMessage());
			}
		} else if (ConfTag.REASONER_VERSION.equals(propertyName)) {
			try {
				int v = Integer.parseInt(propertyValue);
				Conf.setReasonerVersion(v);
				return "" + Conf.getReasonerVersion();
			} catch (Exception e) {
				out.println(e.getMessage());
				throw new ConfigurationException(e.getMessage());
			}
		} else if (ConfTag.IS_SHOW_PROGRESS.equals(propertyName) //
				|| ConfTag.IS_SAVE_RESULT.equals(propertyName) //
				|| ConfTag.IS_SHOW_RESULT.equals(propertyName)) {
			try {
				boolean b = Boolean.valueOf(propertyValue);
				String retn = Boolean.toString(b);
				Conf.getSystemProperties().put(propertyName, retn);
				return retn;
			} catch (Exception e) {
				out.println(e.getMessage());
				throw new ConfigurationException("invalid property value: " + propertyValue);
			}
		} else {
			Conf.getSystemProperties().put(propertyName, propertyValue);
			return propertyValue;
		}
	}
}
