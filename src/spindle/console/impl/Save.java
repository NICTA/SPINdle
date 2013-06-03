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

import java.io.File;
import java.util.List;
import java.util.Scanner;

import spindle.console.CommandOption;
import spindle.console.ConsoleException;
import spindle.core.dom.Conclusion;
import spindle.core.dom.Theory;
import spindle.io.IOManager;
import spindle.io.OutputterException;
import spindle.sys.ConfigurationException;
import spindle.sys.Messages;
import spindle.sys.message.ErrorMessage;
import spindle.sys.message.SystemMessage;

/**
 * Console command: save.
 * <p>
 * Save the theory or conclusions generated to a file
 * </p>
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.0.0
 * @since 2011.07.27
 * @version Last modified 2012.07.30
 */
public class Save extends CommandBase {
	public static final String COMMAND_NAME = "save";
	public static final String DESCRIPTION = "Save current theory and conclusions.";
	public static final String USAGE = "save theory [filename]\nsave conclusions [filename]";

	public Save() {
		super(COMMAND_NAME, DESCRIPTION, USAGE);
		addOption(new CommandOption("theory", "", "Save theory"));
		addOption(new CommandOption("conclusions", "", "Save conclusions"));
	}

	@Override
	public Object execute(Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		throw new ConsoleException(COMMAND_NAME, ErrorMessage.CONSOLE_NOTHING_TO_EXECUTE);
	}

	@Override
	public Object execute(String option, Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		String o = getOptionName(option);
		if ("theory".equals(o)) {
			if (null == theory || theory.isEmpty()) {
				out.println("theory is empty!  save cancelled");
			} else {
				if (args.size() < 1) throw new ConsoleException(COMMAND_NAME, ErrorMessage.IO_EMPTY_FILENAME);
				saveTheory(args.get(0), theory);
			}
		} else if ("conclusions".equals(o)) {
			if (null == conclusions || conclusions.size() == 0) {
				out.println("conclusions is empty!  save cancelled");
			} else {
				if (args.size() < 1) throw new ConsoleException(COMMAND_NAME, ErrorMessage.IO_EMPTY_FILENAME);
				saveConclusions(args.get(0), conclusions);
			}
		} else {
			throw new ConsoleException(COMMAND_NAME, ErrorMessage.CONSOLE_COMMAND_OPTION_NOT_AVAILABLE, option);
		}
		return null;
	}

	private void saveTheory(String filename, Theory theory) throws ConsoleException {
		File file = new File(filename);
		try {
			if (canSave(file)) {
				out.print(Messages.getSystemMessage(SystemMessage.IO_SAVE_THEORY, file));
//				out.print("save theory to [" + file + "]...");
				IOManager.save(file, theory, null);
				out.println(Messages.getSystemMessage(SystemMessage.APPLICATION_OPERATION_SUCCESS));
				//out.println("...success");
			} else {
				out.println("theory save cancelled");
			}
		} catch (OutputterException e) {
			out.println(Messages.getSystemMessage(SystemMessage.APPLICATION_OPERATION_FAILED));
//			out.println("...failed");
			out.println(e.getMessage());
			throw new ConsoleException(COMMAND_NAME, ErrorMessage.CONSOLE_ERROR_MESSAGE, e.getMessage());
		}
	}

	private void saveConclusions(String filename, List<Conclusion> conclusions) throws ConsoleException {
		File file = new File(filename);
		try {
			if (canSave(file)) {
				out.print(Messages.getSystemMessage(SystemMessage.IO_SAVE_CONCLUSIONS, file));
//				out.print("save conclusions to [" + file + "]...");
				IOManager.save(file, conclusions, null);
				out.println(Messages.getSystemMessage(SystemMessage.APPLICATION_OPERATION_SUCCESS));
				//out.println("...success");
			} else {
				out.println("conclusions save cancelled");
			}
		} catch (OutputterException e) {
			out.println(Messages.getSystemMessage(SystemMessage.APPLICATION_OPERATION_FAILED));
//			out.println("...failed");
			out.println(e.getMessage());
			throw new ConsoleException(COMMAND_NAME, ErrorMessage.CONSOLE_ERROR_MESSAGE, e.getMessage());
		}
	}

	private boolean canSave(File file) {
		if (!file.exists()) return true;
		java.io.Console console = System.console();
		Scanner scanner = new Scanner(System.in);
		console.printf(Messages.getSystemMessage(SystemMessage.IO_OVERWRITE_EXISTING_FILE, file));
		//console.printf("> overwrite existing file? (Y/N) ");
		String isOverwrite = scanner.next().trim();
		scanner.reset();
		if ("y".equalsIgnoreCase(isOverwrite) || "yes".equalsIgnoreCase(isOverwrite)) return true;
		return false;
	}
}
