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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.app.utils.FileManager;
import com.app.utils.TextUtilities;

import spindle.console.Command;
import spindle.console.CommandOption;
import spindle.console.Commands;
import spindle.console.ConsoleException;
import spindle.core.dom.Conclusion;
import spindle.core.dom.Theory;
import spindle.sys.AppConst;
import spindle.sys.ConfigurationException;
import spindle.sys.Messages;
import spindle.sys.message.ErrorMessage;

/**
 * Base class for command supported in console
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.0.0
 * @since 2011.07.27
 * @version Last modified 2012.07.30
 */
public abstract class CommandBase implements Command, Comparable<Object> {
	protected static String LINE_SEPARATOR = FileManager.LINE_SEPARATOR;
	private static final int TEXT_WIDTH = Commands.TEXT_WIDTH;

	private String name = null;
	private String description = null;
	private boolean isMiscellaneous = false;
	private boolean isUserCommand = true;
	private Set<CommandOption> options = null;
	private Map<String, String> optionsMap = null;
	private String usage = null;
	private String cmdStr = null;
	protected PrintStream out = null;

	public CommandBase(final String name, final String description, final String usage) {
		this(name, description, usage, false, true);
	}

	public CommandBase(final String name, final String description, final String usage, final boolean isMiscellaneous) {
		this(name, description, usage, isMiscellaneous, true);
	}

	public CommandBase(final String name, final String description, final String usage, final boolean isMiscellaneous, final boolean isUserCommand) {
		if (null == name || "".equals(name.trim())) throw new IllegalArgumentException(Messages.getErrorMessage(ErrorMessage.CONSOLE_NULL_COMMAND_NAME));
		this.name = name.trim();
		this.isUserCommand = isUserCommand;
		this.isMiscellaneous = isMiscellaneous;
		setDescription(description);
		setUsage(usage);
		options = new TreeSet<CommandOption>();
		optionsMap = new HashMap<String, String>();
		setPrintStream(System.out);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isUserCommand() {
		return isUserCommand;
	}

	@Override
	public boolean isMiscellaneous() {
		return isMiscellaneous;
	}

	@Override
	public void setDescription(String description) {
		this.description = (null == description) ? "" : description.trim();
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setUsage(String usage) {
		this.usage = usage;
	}

	@Override
	public String getUsage() {
		if (null == cmdStr) {
			StringBuilder sb = new StringBuilder();
			sb.append("Command: ").append(name).append(LINE_SEPARATOR);
			if (!"".equals(usage)) {
				int c = 0;
				sb.append(LINE_SEPARATOR).append("Usage: ");
				for (String s : usage.split("\n")) {
					if (c++ > 0) sb.append("       ");
					sb.append(s.trim()).append(LINE_SEPARATOR);
				}
			}
			if (!"".equals(description)) {
				sb.append(LINE_SEPARATOR)//
						.append("Description").append(LINE_SEPARATOR).append("-----------")//
						.append(LINE_SEPARATOR).append(TextUtilities.trimTextWithWidth(description, TEXT_WIDTH)).append(LINE_SEPARATOR);
			}
			if (options.size() > 0) {
				List<List<String>> optionTextList = new ArrayList<List<String>>();
				int maxArgsLen = 0, l = 0;
				for (CommandOption option : options) {
					if (!"".equals(option.getName())) {
						List<String> optionText = new ArrayList<String>();
						optionText.add("");
						optionText.add(option.getOptionWithArgs());
						optionText.add(option.getDescription());
						optionTextList.add(optionText);
						l = option.getOptionWithArgs().length();
						if (l > maxArgsLen) maxArgsLen = l;
					}
				}
				if (optionTextList.size() > 0) {
					String[][] text = new String[optionTextList.size()][3];
					for (int i = 0; i < optionTextList.size(); i++) {
						optionTextList.get(i).toArray(text[i]);
					}
					String[] header = { "", "", "" };
					int[] sep = { 0, Commands.COMMAND_ARGS_DESCRIPTION_SEP };
					int descriptionWidth = TEXT_WIDTH - AppConst.IDENTATOR.length() - maxArgsLen;
					for (int s : sep)
						descriptionWidth -= s;
					int[] columnWidth = { AppConst.IDENTATOR.length(), maxArgsLen, descriptionWidth };
					sb.append(LINE_SEPARATOR).append("Option(s)").append(LINE_SEPARATOR).append("---------").append(LINE_SEPARATOR);
					sb.append(TextUtilities.generateColumnText(header, text, columnWidth, sep, "", ""));
					sb.append(LINE_SEPARATOR);
				}
			}
			cmdStr = sb.toString();
		}
		return cmdStr;
	}

	@Override
	public Set<CommandOption> getOptions() {
		return options;
	}

	@Override
	public int compareTo(Object o) {
		if (this == o) return 0;
		if (o instanceof CommandBase) {
			CommandBase cmd = (CommandBase) o;

			int c = (name.compareTo(cmd.name));
			if (c != 0) return c;

			if (options.size() != cmd.options.size()) return options.size() - cmd.options.size();
			for (CommandOption commandOption : options) {
				if (!cmd.options.contains(commandOption)) return Integer.MAX_VALUE;
			}
			return 0;
		}
		return toString().compareTo(o.toString());
	}

	protected void addOption(CommandOption option) {
		options.add(option);
		if ("".equals(option.getName())) return;
		String name = option.getName();
		String shortcut = (name.length() > Commands.SHORT_CUT_CHARACTORS_LENGTH) ? name.substring(0, Commands.SHORT_CUT_CHARACTORS_LENGTH) : name;
		optionsMap.put(name, name);
		optionsMap.put(shortcut, name);
	}

	public String getOptionName(String name) {
		return optionsMap.get(name);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		if (options.size() == 0) {
			sb.append("\t").append(description);
		} else {
			int maxLen = 0;
			for (CommandOption option : options) {
				if (!"".equals(option.getName()) && option.getName().length() > maxLen) maxLen = option.getName().length();
			}
			for (CommandOption option : options) {
				if (!"".equals(option.getName())) {
					sb.append(LINE_SEPARATOR).append(AppConst.IDENTATOR).append(option.getName()).append(" \t").append(option.getDescription());
				}
			}
		}
		return sb.toString();
	}

	public void setPrintStream(final PrintStream out) {
		if (null != out) this.out = out;
	}

	protected void printConsoleMessage(String messageTag) {
		printConsoleMessage(messageTag, null, true);
	}

	protected void printConsoleMessage(String messageTag, Object[] args) {
		printConsoleMessage(messageTag, args, true);
	}

	protected void printConsoleMessage(String messageTag, Object[] args, boolean withLineBreak) {
		if (null == out) return;
		if (withLineBreak) out.println(Messages.getSystemMessage(messageTag, args));
		else out.print(Messages.getSystemMessage(messageTag, args));
	}

	public abstract Object execute(Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException;

	public abstract Object execute(String option, Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException;

}
