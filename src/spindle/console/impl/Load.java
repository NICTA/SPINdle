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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import com.app.utils.Converter;

import spindle.console.CommandOption;
import spindle.console.ConsoleException;
import spindle.core.ReasonerUtilities;
import spindle.core.dom.Conclusion;
import spindle.core.dom.Theory;
import spindle.io.IOManager;
import spindle.sys.ConfigurationException;
import spindle.sys.message.ErrorMessage;

/**
 * Console command: load.
 * <p>
 * Load a theory from a file or an URI.
 * </p>
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.0.0
 * @since 2011.07.27
 * @version Last modified 2012.07.30
 */
public class Load extends CommandBase {
	public static final String COMMAND_NAME = "load";
	public static final String DESCRIPTION = "Load theory from file";
	public static final String USAGE = "load [filename]";

	public Load() {
		super(COMMAND_NAME, DESCRIPTION, USAGE);
		addOption(new CommandOption("", "[filename]", "Load theory from file"));
	}

	@Override
	public Object execute(Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		if (args.size() == 0) throw new ConsoleException(COMMAND_NAME, ErrorMessage.IO_EMPTY_FILENAME);
		try {
			String filenameStr = args.get(0);
			List<URL> filenames = ReasonerUtilities.getFilenames(filenameStr);

			if (args.size() > 1 || filenames.size() > 1) out.println("arguments contained more than one files!\nonly the first theory is loaded");
			out.println("load file: " + filenames.get(0));
			long startTime = System.currentTimeMillis();
			Theory newTheory = IOManager.getTheory(filenames.get(0), null);
			long endTime = System.currentTimeMillis();
			out.println(newTheory.toString());
			out.println("theory loaded successfully");
			out.println("time used=" + (Converter.long2TimeString(endTime - startTime)));
			return newTheory;
		} catch (IOException e) {
			out.println("IO Exception: " + e.getMessage());
			throw new ConsoleException(COMMAND_NAME, ErrorMessage.CONSOLE_ERROR_MESSAGE, e);
		} catch (URISyntaxException e) {
			out.println("URI Syntax Exception: " + e.getMessage());
			throw new ConsoleException(COMMAND_NAME, ErrorMessage.CONSOLE_ERROR_MESSAGE, e);
		} catch (Exception e) {
			out.println(e.getMessage());
			throw new ConsoleException(COMMAND_NAME, ErrorMessage.CONSOLE_ERROR_MESSAGE, e);
		}
	}

	@Override
	public Object execute(String option, Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		return execute(theory, conclusions, args);
	}

}
