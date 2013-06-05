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
package spindle;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import spindle.console.Command;
import spindle.console.CommandOptionException;
import spindle.console.Commands;
import spindle.console.ConsoleException;
import spindle.console.UnrecognizedCommandException;
import spindle.console.impl.*;
import spindle.core.ReasonerException;
import spindle.core.dom.AppConstants;
import spindle.core.dom.Conclusion;
import spindle.core.dom.Theory;
import spindle.sys.AppConst;
import spindle.sys.Conf;
import spindle.sys.ConfigurationException;
import spindle.sys.ConfTag;
import spindle.sys.Messages;
import spindle.sys.NullValueException;
import spindle.sys.message.ErrorMessage;
import spindle.sys.message.SystemMessage;

/**
 * Console user interface for SPINdle
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since 2011.03.25
 * @since version 2.0.0
 */
public class Console {
	private static final int HISTORY_SIZE = 50;

	private java.io.Console console = null;
	private Scanner scanner = null;
	private Commands commands = null;
	private List<Command> history = null;
	private List<String> historyArgs = null;

	private Theory theory = null;
	private boolean isTheoryModified = false;
	private List<Conclusion> conclusions = null;

	private PrintStream out = null;
	private AppConstants appConstants = null;

	public Console(Map<String, String> args) {
		config(args);
	}

	private void config(Map<String, String> args) {
		try {
			console = System.console();
			scanner = new Scanner(System.in);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		history = new ArrayList<Command>();
		historyArgs = new ArrayList<String>();

		commands = new Commands();
		setOutputStream(System.out);

		Map<String, String> config = new HashMap<String, String>();
		if (null != args) config.putAll(args);
		config.put(ConfTag.IS_CONSOLE_MODE, "true");

		Conf.initializeApplicationContext(config);
	}

	public void setOutputStream(OutputStream out) {
		if (null != out) {
			PrintStream ps = new PrintStream(out);
			setOutputStream(ps);
			getAppConstants().setOutputStream(ps);
		}
	}

	private AppConstants getAppConstants() {
		if (null == appConstants) appConstants = AppConstants.getInstance(null);
		return appConstants;
	}

	public void setOutputStream(PrintStream out) {
		if (null != out) {
			this.out = out;
			commands.setPrintStream(out);
		}
	}

	private void addHistory(Command command, String args) {
		history.add(command);
		historyArgs.add(args);
		while (history.size() > HISTORY_SIZE + 1) {
			history.remove(0);
			historyArgs.remove(0);
		}
	}

	private void showHistory() {
		for (int i = 0; i < history.size(); i++) {
			out.println((i + 1) + "\t" + history.get(i).getName() + " " + historyArgs.get(i));
		}
	}

	private void reset() {
		out.print(Messages.getSystemMessage(SystemMessage.CONSOLE_CLEAR_SYSTEM_ENVIRONMENT));
		theory = null;
		conclusions = null;
		isTheoryModified = false;
		out.println(Messages.getSystemMessage(SystemMessage.APPLICATION_OPERATION_SUCCESS));
	}

	@SuppressWarnings("unchecked")
	public void start(List<URL> filenames) throws ReasonerException, ConsoleException {
		if (null == scanner) throw new ConsoleException(ErrorMessage.CONSOLE_NULL_SCANNER);
		reset();
		out.println("\n" + Messages.getSystemMessage(SystemMessage.CONSOLE_SHOW_COMMAND_MANUAL) + "\n");

		Command cmd = null;
		String args = null;
		List<String> argsList = null;
		String cmdStr = null;
		int historyNo = -1;
		boolean isTheoryUpdated = false;

		scanner.reset();

		if (null != filenames) {
			try {
				cmd = commands.getCommand(Load.COMMAND_NAME);
				argsList = new ArrayList<String>();
				StringBuilder sb = new StringBuilder();
				for (URL filename : filenames) {
					argsList.add(filename.toString());
					sb.append(filename.toString()).append(" ");
				}
				Object result = cmd.execute(theory, conclusions, argsList);
				if (null != result) {
					theory = (Theory) result;
					isTheoryModified = true;
				}
				addHistory(cmd, sb.toString());
			} catch (UnrecognizedCommandException e) {
				out.println(e.getMessage());
			} catch (ConsoleException e) {
				out.println(e.getMessage());
			} catch (Exception e) {
				if (AppConst.isDeploy) e.printStackTrace(out);
			}
		}

		do {
			console.printf("> ");
			if (scanner.hasNext()) {
				cmdStr = scanner.next();
				args = scanner.nextLine();
				args = (null == args) ? "" : args.trim();
			}
			try {
				historyNo = Integer.parseInt(cmdStr);
				cmd = history.get(historyNo - 1);
				args = historyArgs.get(historyNo - 1);
				out.println(cmd.getName() + " " + args);
			} catch (Exception e1) {
				try {
					cmd = commands.getCommand(cmdStr);
				} catch (Exception e2) {
					cmd = commands.getUnknownCommand();
				}
			}

			Object result = null;
			try {
				if (cmd instanceof Unknown) {
					throw new UnrecognizedCommandException(cmdStr);
				} else if (cmd instanceof Quit) {
				} else {
					argsList = new ArrayList<String>();
					for (String s : args.split("\\s")) {
						if (!"".equals(s)) argsList.add(s);
					}

					addHistory(cmd, args);
					if (cmd instanceof Help) {
						commands.listCommands(args);
					} else if (cmd instanceof History) {
						showHistory();
					} else if (cmd instanceof Clear) {
						reset();
					} else if (cmd instanceof Conclusions) {
						if (isTheoryModified) {
							transformToRegularForm("");
							isTheoryModified = false;
						}
						result = cmd.execute(theory, conclusions, argsList);
						if (null != result) conclusions = (List<Conclusion>) result;
					} else if (cmd instanceof Show || cmd instanceof Set || cmd instanceof Load || cmd instanceof Add) {
						result = cmd.execute(theory, conclusions, argsList);
						if (cmd instanceof Load || cmd instanceof Add) {
							if (null != result) isTheoryUpdated = true;
						}
					} else {
						String option = (argsList.size() == 0) ? null : argsList.get(0);
						if (null == option || "".equals(option.trim())) throw new CommandOptionException(cmd.getName(),
								ErrorMessage.CONSOLE_COMMAND_NULL_OPTION_INFORMATION);
						option = option.trim();

						argsList.remove(0);

						if (cmd instanceof Transform) transformToRegularForm(option);

						result = cmd.execute(option, theory, conclusions, argsList);

						if (cmd instanceof Remove) {
							isTheoryUpdated = true;
						} else if (cmd instanceof Transform) {
							if (null != option && option.startsWith("regu")) isTheoryModified = false;
						}
					}
				}

				if (isTheoryUpdated && null != result && result instanceof Theory) {
					theory = (Theory) result;
					isTheoryModified = true;
				}
			} catch (UnrecognizedCommandException e) {
				out.println(e.getMessage());
			} catch (ConsoleException e) {
				out.println(e.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
				if (AppConst.isDeploy) e.printStackTrace(out);
			}

			out.println("");
			scanner.reset();
		} while (!(cmd instanceof Quit));
	}

	private void transformToRegularForm(String option) throws ConfigurationException, ConsoleException,
			UnrecognizedCommandException, NullValueException {
		if (!isTheoryModified) return;

		Command transform = commands.getCommand(Transform.COMMAND_NAME);
		String opt = "".equals(option.trim()) ? "" : transform.getOptionName(option);
		if (null == opt) throw new UnrecognizedCommandException("option [" + option + "] not found");
		if (opt.startsWith("regu")) return;

		console.printf("theory is modified, perform regular form transformation? (Y/N) ");
		String isTransform = scanner.next().trim();
		if ("y".equalsIgnoreCase(isTransform) || "yes".equalsIgnoreCase(isTransform)) {
			Object result = transform.execute("regu", theory, conclusions, null);
			if (null != result) theory = (Theory) result;
			isTheoryModified = false;
		}
	}

	public void terminate() {
		out.println(Messages.getSystemMessage(SystemMessage.CONSOLE_CONSOLE_MODE_TERMINATE));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Console console = new Console(null);
		try {
			console.start(null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			console.terminate();
		}
	}

}
