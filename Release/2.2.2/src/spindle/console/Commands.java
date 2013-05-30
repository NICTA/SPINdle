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
package spindle.console;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import spindle.console.impl.*;
import spindle.core.dom.DomConst;
import spindle.sys.Messages;
import spindle.sys.NullValueException;
import spindle.sys.message.ErrorMessage;
import spindle.sys.message.SystemMessage;

import com.app.utils.FileManager;
import com.app.utils.TextUtilities;

public class Commands {
	public static final int TEXT_WIDTH = 78;
	public static final int SHORT_CUT_CHARACTORS_LENGTH = 4;
	public static final int COMMAND_ARGS_DESCRIPTION_SEP = 2;
	protected static String LINE_SEPARATOR = FileManager.LINE_SEPARATOR;

	private Map<String, Command> commands = null;
	private Map<String, String> commandsMap = null;
	private String commandStr = null;

	private PrintStream out = null;

	public Commands() {
		this(System.out);
	}

	public Commands(PrintStream out) {
		commands = new TreeMap<String, Command>();
		commandsMap = new HashMap<String, String>();
		setCommands();
		setPrintStream(out);
	}

	private void setCommands() {
		addCommand(new Add());
		addCommand(new AppConstants());
		addCommand(new Clear());
		addCommand(new Conclusions());
		addCommand(new Help());
		addCommand(new History());
		addCommand(new Load());
		addCommand(new Quit());
		addCommand(new Remove());
		addCommand(new Save());
		addCommand(new Set());
		addCommand(new Show());
		addCommand(new Transform());
		addCommand(new Unknown());
	}

	public void addCommand(Command command) {
		String name = command.getName();
		commands.put(name, command);
		String shortcut = (name.length() > SHORT_CUT_CHARACTORS_LENGTH) ? command.getName().substring(0,
				SHORT_CUT_CHARACTORS_LENGTH) : name;
		commandsMap.put(name, name);
		commandsMap.put(shortcut, name);
	}

	public Command getCommand(String commandName) throws UnrecognizedCommandException, NullValueException {
		if (null == commandName || "".equals(commandName.trim())) throw new NullValueException(
				ErrorMessage.CONSOLE_NULL_COMMAND_NAME);
		String commandMappedName = commandsMap.get(commandName.trim());
		if (null == commandMappedName) throw new UnrecognizedCommandException(commandName);
		return commands.get(commandMappedName);
	}

	public Command getUnknownCommand() {
		return commands.get(Unknown.COMMAND_NAME);
	}

	public void setPrintStream(PrintStream out) {
		this.out = (null == out) ? new PrintStream(System.out) : out;
		for (Command command : commands.values()) {
			command.setPrintStream(this.out);
		}
	}

	public void printUsage(List<String> commandList) throws UnrecognizedCommandException, NullValueException {
		String cmdStr = commandList.get(0);
		if (cmdStr.charAt(0) == DomConst.Literal.LITERAL_VARIABLE_PREFIX) {
			commandList.set(0, AppConstants.COMMAND_NAME);
			commandList.add(1, cmdStr);
		}
		Command command = getCommand(commandList.get(0));

		if (command instanceof AppConstants && commandList.size() > 1) {
			try {
				command.execute(commandList.get(1), null, null, null);
			} catch (Exception e) {
				out.println(e.getMessage());
			}
		} else {
			out.print(command.getUsage());
		}
	}

	public void listCommands(String commandName) throws UnrecognizedCommandException, NullValueException {
		List<String> cmdStr = TextUtilities.trimTextArray(commandName, " ");
		if (cmdStr.size() > 0) {
			printUsage(cmdStr);
			return;
		}
		if (null == commandStr) {
			List<List<String>> userCommandList = new ArrayList<List<String>>();
			List<List<String>> miscCommandList = new ArrayList<List<String>>();
			int commandWidth = 0, optionWidth = 0, l = 0;
			for (Command command : commands.values()) {
				if (command.isUserCommand()) {
					List<String> commandText = new ArrayList<String>();
					commandText.add(command.getName());
					l = command.getName().length();
					if (l > commandWidth) commandWidth = l;

					java.util.Set<CommandOption> options = command.getOptions();
					if (command.isMiscellaneous()) {
						if (options.size() == 0) {
							commandText.add("");
							commandText.add(command.getDescription());
							miscCommandList.add(commandText);
						} else {
							int c = 0;
							for (CommandOption option : options) {
								if (c++ > 0) commandText.add("");
								commandText.add(option.getOptionWithArgs());
								commandText.add(option.getDescription());
								miscCommandList.add(commandText);
								commandText = new ArrayList<String>();

								l = option.getOptionWithArgs().length();
								if (l > optionWidth) optionWidth = l;
							}
						}
					} else {
						if (options.size() == 0) {
							commandText.add("");
							commandText.add(command.getDescription());
							userCommandList.add(commandText);
						} else {
							int c = 0;
							for (CommandOption option : options) {
								if (c++ > 0) commandText.add("");
								commandText.add(option.getOptionWithArgs());
								commandText.add(option.getDescription());
								userCommandList.add(commandText);
								commandText = new ArrayList<String>();

								l = option.getOptionWithArgs().length();
								if (l > optionWidth) optionWidth = l;
							}
						}
					}
				}
			}

			// String[] header1 = { "Command", "", "Description" };
			String[] header1 = { Messages.getSystemMessage(SystemMessage.CONSOLE_COMMANDS_COMMAND), //
					"",//
					Messages.getSystemMessage(SystemMessage.CONSOLE_COMMANDS_DESCRIPTION) //
			};

			int[] sep = { 1, COMMAND_ARGS_DESCRIPTION_SEP };
			int descriptionWidth = TEXT_WIDTH - commandWidth - optionWidth;
			for (int s : sep)
				descriptionWidth -= s;
			int[] columnWidth = { commandWidth, optionWidth, descriptionWidth };
			String[][] text = new String[userCommandList.size()][3];
			for (int i = 0; i < userCommandList.size(); i++) {
				userCommandList.get(i).toArray(text[i]);
			}
			commandStr = TextUtilities.generateColumnText(header1, text, columnWidth, sep, "", "");

			if (miscCommandList.size() > 0) {
				text = new String[miscCommandList.size()][3];
				for (int i = 0; i < miscCommandList.size(); i++) {
					miscCommandList.get(i).toArray(text[i]);
				}

				String[] header2 = { "", "", "" };

				if (!"".equals(commandStr)) commandStr += LINE_SEPARATOR;
				commandStr += (LINE_SEPARATOR + TextUtilities.generateColumnText(header2, text, columnWidth, sep, "",
						""));
			}
		}
		out.println(commandStr);
	}
}
