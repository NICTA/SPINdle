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
import java.util.Properties;

import com.app.utils.TextUtilities;

import spindle.console.CommandOption;
import spindle.console.ConsoleException;
import spindle.console.UnrecognizedCommandException;
import spindle.core.dom.Conclusion;
import spindle.core.dom.Theory;
import spindle.sys.AppConst;
import spindle.sys.Conf;
import spindle.sys.ConfigurationException;
import spindle.sys.IncorrectNoOfArgumentsException;

/**
 * Console command: show.
 * <p>
 * Display the theory, conclusion generated and system properties values.
 * </p>
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.0.0
 * @since 2011.07.27
 * @version Last modified 2012.07.30
 */
public class Show extends CommandBase {
	public static final String COMMAND_NAME = "show";
	public static final String DESCRIPTION = "Display the current theory, conclusions derived and system properties";
	public static final String USAGE = "show [theory | conclusions | properties | property name]";

	public Show() {
		super(COMMAND_NAME, DESCRIPTION, USAGE);
		addOption(new CommandOption("theory", "", "Show theory"));
		addOption(new CommandOption("conclusions", "", "Show conclusion"));
		addOption(new CommandOption("properties", "", "Show all properties"));
		addOption(new CommandOption("", "[property name]", "Show system property by name"));
	}

	@Override
	public Object execute(Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		if (args.size() == 0) throw new IncorrectNoOfArgumentsException(COMMAND_NAME, 1);
		// if (args.size() == 0) throw new ConsoleException(COMMAND_NAME, ErrorMessage.INCORRECT_NO_OF_ARGUMENTS, 1);
		for (int i = 0; i < args.size(); i++) {
			if (i > 0) out.println("");
			execute((String) args.get(i), theory, conclusions, args);
		}
		return null;
	}

	@Override
	public Object execute(String option, Theory theory, List<Conclusion> conclusions, List<String> args) //
			throws ConfigurationException, ConsoleException {
		String o = getOptionName(option);
		if ("theory".equals(o)) {
			showTheory(theory);
		} else if ("conclusions".equals(o)) {
			showConclusions(conclusions);
		} else if ("properties".equals(o)) {
			showProperty(null);
		} else {
			for (String propertyName : args) {
				showProperty(propertyName);
			}
		}
		return null;
	}

	private void showTheory(Theory theory) {
		if (null == theory) {
			out.println("theory is null");
		} else {
			out.println(theory.toString());
		}
	}

	private void showConclusions(List<Conclusion> conclusions) {
		if (null == conclusions || conclusions.size() == 0) {
			out.println("no conclusions available");
		} else {
			out.println("Conclusions");
			out.println("-----------");
			for (Conclusion conclusion : conclusions) {
				out.println(AppConst.IDENTATOR + conclusion);
			}
		}
	}

	private void showProperty(final String propertyName) throws UnrecognizedCommandException {
		java.util.Set<String> properties = Set.properties;
		Properties systemProp = Conf.getSystemProperties();
		StringBuilder sb = new StringBuilder();
		if (null == propertyName) {
			int len1 = 0;
			for (String name : properties) {
				if (name.length() > len1) len1 = name.length();
			}
			len1 += 2;
			int c = 0;
			for (String name : Set.properties) {
				sb.append(name).append(TextUtilities.repeatStringPattern(" ", len1 - name.length())).append(systemProp.get(name));
				if (++c < Set.properties.size()) sb.append(LINE_SEPARATOR);
			}
		} else {
			if (!properties.contains(propertyName)) throw new UnrecognizedCommandException(COMMAND_NAME + ", property [" + propertyName + "] not found");
			sb.append(propertyName).append("  ").append(systemProp.get(propertyName));
		}
		out.println(sb.toString());
	}
}
