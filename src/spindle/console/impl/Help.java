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
import spindle.core.dom.Conclusion;
import spindle.core.dom.Theory;
import spindle.sys.ConfigurationException;
import spindle.sys.message.ErrorMessage;

/**
 * Console command: help.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.0.0
 * @since 2011.07.27
 * @version Last modified 2012.07.30
 */
public class Help extends CommandBase {
	public static final String COMMAND_NAME = "help";
	public static final String DESCRIPTION = "Show system/command menu";
	public static final String USAGE = "help [command name]";

	public Help() {
		super(COMMAND_NAME, DESCRIPTION, USAGE, true);
		addOption(new CommandOption("", "[command name]", "Show command details"));
	}

	@Override
	public Object execute(Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		throw new ConsoleException(COMMAND_NAME, ErrorMessage.CONSOLE_NOTHING_TO_EXECUTE);
	}

	@Override
	public Object execute(String option, Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		throw new ConsoleException(COMMAND_NAME, ErrorMessage.CONSOLE_NOTHING_TO_EXECUTE);
	}
}
