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
package spindle.core.dom;

import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import com.app.utils.TextUtilities;

import spindle.console.UnrecognizedCommandException;
import spindle.core.dom.impl.*;
import spindle.io.outputter.DflTheoryConst;
import spindle.sys.NullValueException;
import spindle.sys.message.ErrorMessage;

/**
 * Utilities class for application constants.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since 2011.07.26
 * @since version 2.1.0
 */
public class AppConstants {
	private static final int TEXT_WIDTH = 78;
	private static final int LABEL_SEP = 2;

	private static AppConstants INSTANCE = null;

	private static java.util.Set<String> contantsWithAbstractLiteralPredicatesSet = new TreeSet<String>() {
		private static final long serialVersionUID = 1L;
		{
			add(Set.LABEL + DflTheoryConst.PREDICATE_START);
			add(Contains.LABEL + DflTheoryConst.PREDICATE_START);
		}
	};

	public static AppConstants getInstance(PrintStream out) {
		if (null == INSTANCE) INSTANCE = new AppConstants(out);
		INSTANCE.setOutputStream(out);
		return INSTANCE;
	}

	private Map<String, AppConstant> appConstants = null;

	private PrintStream out = null;

	public AppConstants() {
		this(System.out);
	}

	public AppConstants(PrintStream out) {
		setOutputStream(out);
		appConstants = new TreeMap<String, AppConstant>();
		setConstants();
	}

	private void setConstants() {
		addConstant(new Now());
		addConstant(new Today());
		addConstant(new Date());
		addConstant(new Val());
		addConstant(new Duration());
		addConstant(new Set());
		addConstant(new Contains());
	}

	private void addConstant(AppConstant appConstant) {
		appConstants.put(appConstant.getLabel(), appConstant);
	}

	public boolean isAppConstant(String appConstantLabel) throws NullValueException {
		return getAppConstantByName(appConstantLabel.toUpperCase()) != null;
	}

	public boolean isAppConstant(LiteralVariable literalVariable) throws NullValueException {
		return isAppConstant(null == literalVariable ? null : literalVariable.getName());
	}

	public boolean containsAbstractLiteralInPredicate(String literalName) {
		if (null == literalName) return false;
		String ln = literalName.trim().toUpperCase() + DflTheoryConst.PREDICATE_START;
		for (String appConstName : contantsWithAbstractLiteralPredicatesSet) {
			if (ln.startsWith(appConstName)) return true;
		}
		return false;
	}

	private AppConstant getAppConstantByName(String appConstantName) throws NullValueException {
		if (null == appConstantName || "".equals(appConstantName.trim())) throw new NullValueException(
				ErrorMessage.LITERAL_VARIABLE_NULL_THEORY_VARIABLE_NAME);
		return appConstants.get(appConstantName.trim().toUpperCase());
	}

	public AppConstant getAppConstant(String appConstantName) throws UnrecognizedCommandException, NullValueException {
		AppConstant appConstant = getAppConstantByName(appConstantName.toUpperCase());
		if (null == appConstant) throw new UnrecognizedCommandException(appConstantName);
		return appConstant;
	}

	public AppConstant getAppConstant(LiteralVariable literalVariable) throws UnrecognizedCommandException,
			NullValueException {
		return getAppConstant(null == literalVariable ? null : literalVariable.getName());
	}

	public LiteralVariable getAppConstantAsLiteralVariable(LiteralVariable literalVariable)
			throws UnrecognizedCommandException, NullValueException, AppConstantException {
		AppConstant appConstant = getAppConstant(literalVariable);
		if (null == appConstant) throw new UnrecognizedCommandException(literalVariable.getName());
		try {
			return appConstant.getLiteralVariable(literalVariable.isNegation, literalVariable.getPredicates());
		} catch (Exception e) {
			throw new AppConstantException(e);
		}
	}

	public void setOutputStream(PrintStream out) {
		this.out = (null == out) ? System.out : out;
	}

	/**
	 * print application constant usage on the console
	 * 
	 * @param constantLabel application constant name
	 * @throws UnrecognizedCommandException if the application constant name cannot be identified
	 * @throws NullValueException if the application constant value is null
	 */
	public void printUsage(String constantLabel) throws UnrecognizedCommandException, NullValueException {
		if (null == constantLabel || "".equals(constantLabel.trim())) return;
		AppConstant appConstant = getAppConstant(constantLabel);
		String label = appConstant.getLabel();
		String l = TextUtilities.repeatStringPattern("=", label.length());
		StringBuilder sb = new StringBuilder();
		sb.append(l).append("\n").append(label).append("\n").append(l);
		sb.append("\nUsage: ").append(appConstant.getUsage());
		sb.append("\n\nDescription:\n").append(
				TextUtilities.trimTextWithWidth(appConstant.getDescription(), TEXT_WIDTH));
		System.out.println(sb.toString());
	}

	/**
	 * show the usage of the application constant specified<br/>
	 * or all application constants information when the input application constant name is null
	 * 
	 * @param constantLabel application constant name
	 * @throws UnrecognizedCommandException if the application constant name cannot be identified
	 * @see #printUsage(String constantLabel)
	 */
	public void listAppConstant(String constantLabel) throws UnrecognizedCommandException {
		if (null != constantLabel && !"".equals(constantLabel.trim())) {
			try {
				printUsage(constantLabel);
			} catch (NullValueException e) {
			}
		}
		if (null == constantLabel) {
			String[] header = { "Constant", "Description" };
			int labelWidth = header[0].length();
			String[][] text = new String[appConstants.size()][2];
			int i = 0;
			for (Entry<String, AppConstant> entry : appConstants.entrySet()) {
				String label = entry.getKey();
				if (label.length() > labelWidth) labelWidth = label.length();
				text[i][0] = label;
				text[i++][1] = entry.getValue().getDescription();
			}
			int descriptionWidth = TEXT_WIDTH - labelWidth - LABEL_SEP;

			int[] columnWidth = new int[] { labelWidth, descriptionWidth };
			int[] sep = new int[] { LABEL_SEP };
			String str = TextUtilities.generateColumnText(header, text, columnWidth, sep, "", "");
			out.println(str);
		}
	}

	public String generateCode(String constantLabel) {
		return constantLabel;
	}

}
