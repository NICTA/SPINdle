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
package spindle.core.dom.impl;

import spindle.core.dom.AppConstant;
import spindle.core.dom.AppConstantException;
import spindle.core.dom.DomUtilities;
import spindle.core.dom.Literal;
import spindle.core.dom.LiteralVariable;
import spindle.sys.AppConst;
import spindle.sys.IncorrectNoOfArgumentsException;
import spindle.sys.InvalidArgumentException;
import spindle.sys.Messages;
import spindle.sys.message.ErrorMessage;

/**
 * Base class for all application constants in SPINdle.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since 2011.07.26
 * @since version 2.1.0
 */
public abstract class AppConstantBase implements AppConstant {

	protected String label;
	protected String description;
	protected String usage;
	protected int minNoOfArguments = 0;
	protected String codebase;

	public AppConstantBase(final String label, //
			final String description, final String usage, //
			final int minNoOfArguments, final String codebase) {
		setLabel(label);
		setDescription(description);
		setUsage(usage);
		setMinNoOfArguments(minNoOfArguments);
		setCodebase(codebase);
	}

	@Override
	public String getLabel() {
		return label;
	}

	protected void setLabel(final String label) {
		if (null == label || "".equals(label.trim())) throw new IllegalArgumentException(Messages.getErrorMessage(ErrorMessage.APPLICATION_CONSTANT_LABEL_MISSING));
		this.label = label.trim();
	}

	@Override
	public String getDescription() {
		return description;
	}

	protected void setDescription(final String description) {
		this.description = (null == description) ? "" : description.trim();
	}

	@Override
	public String getUsage() {
		return usage;
	}

	protected void setUsage(final String usage) {
		this.usage = (null == usage) ? "" : usage.trim();
	}

	@Override
	public int getMinNoOfArguments() {
		return minNoOfArguments;
	}

	protected void setMinNoOfArguments(final int minNoOfArguments) {
		this.minNoOfArguments = (minNoOfArguments < 0) ? 0 : minNoOfArguments;
	}

	@Override
	public boolean hasArguments() {
		return minNoOfArguments > 0;
	}

	protected LiteralVariable getLiteralVariableWithNoPredicate(boolean isNegation) {
		return DomUtilities.getLiteralVariableWithNoArgument(label, isNegation);
	}

	@Override
	public LiteralVariable getLiteralVariable(boolean isNegation) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException {
		_verifyArguments(isNegation, null);
		return getLiteralVariableWithNoPredicate(isNegation);
	}

	@Override
	public LiteralVariable getLiteralVariable(boolean isNegation, String[] args) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException {
		if (null == args || args.length == 0) return getLiteralVariable(isNegation);
		String[] predicates = extractPredicates(args, getNoOfArgumentsToExtract(args));
		_verifyArguments(isNegation, predicates);

		LiteralVariable lv = getLiteralVariableWithNoPredicate(isNegation);
		lv.setPredicates(predicates);
		return lv;
	}

	@Override
	public LiteralVariable getLiteralVariable(boolean isNegation, Literal[] args) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException {
		if (null == args || args.length == 0) return getLiteralVariable(isNegation);
		throw new AppConstantException(ErrorMessage.CONSOLE_COMMAND_NOT_YET_IMPLEMENTED);
	}

	protected void setCodebase(final String codebase) {
		this.codebase = (null == codebase) ? "" : codebase;
	}

	@Override
	public String getCodeBase(boolean isNegation) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException {
		_verifyArguments(isNegation, null);
		try {
			return generateCodeBase(isNegation, null);
		} catch (Exception e) {
			throw new AppConstantException(e);
		}
	}

	@Override
	public String getCodeBase(boolean isNegation, String[] args) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException {
		if (null == args || args.length == 0) return getCodeBase(isNegation);
		String[] predicates = extractPredicates(args, getNoOfArgumentsToExtract(args));
		_verifyArguments(isNegation, predicates);
		return generateCodeBase(isNegation, predicates);
	}

	@Override
	public String getCodeBase(boolean isNegation, Literal[] args) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException {
		throw new AppConstantException(ErrorMessage.CONSOLE_COMMAND_NOT_YET_IMPLEMENTED);
//		throw new AppConstantException("not yet implemented");
	}

	protected String[] extractPredicates(String[] args, int noOfArguments) //
			throws IncorrectNoOfArgumentsException {
		if (noOfArguments > args.length) throw new IncorrectNoOfArgumentsException(minNoOfArguments);
		String[] predicates = new String[noOfArguments];
		for (int i = 0; i < noOfArguments; i++) {
			predicates[i] = args[i];
		}
		return predicates;
	}

	protected int getNoOfArgumentsToExtract(Object[] args) //
			throws IncorrectNoOfArgumentsException {
		if (minNoOfArguments == 0) return 0;
		if (null == args || args.length < minNoOfArguments) throw new IncorrectNoOfArgumentsException(minNoOfArguments);
		return minNoOfArguments;
	}

	protected void _verifyArguments(boolean isNegation, String[] args) //
			throws IncorrectNoOfArgumentsException, InvalidArgumentException {
		if (minNoOfArguments == 0) return;
		if (null == args || minNoOfArguments > args.length) throw new IncorrectNoOfArgumentsException(minNoOfArguments);
		verifyArguments(isNegation, args);
	}

	@Override
	public String toString() {
		return label + "(" + minNoOfArguments + ")\n" + AppConst.IDENTATOR + description;
	}

	protected abstract void verifyArguments(boolean isNegation, String[] args) //
			throws InvalidArgumentException;

	protected abstract String generateCodeBase(boolean isNegation, String[] args) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException;

}
