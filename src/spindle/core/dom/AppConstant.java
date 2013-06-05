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

import spindle.sys.IncorrectNoOfArgumentsException;
import spindle.sys.InvalidArgumentException;

/**
 * Interface for application constant.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @version 2011.07.26
 * @since version 2.1.0
 */
public interface AppConstant {

	public String getLabel();

	public String getDescription();

	public String getUsage();

	public int getMinNoOfArguments();

	public boolean hasArguments();

	public LiteralVariable getLiteralVariable(boolean isNegation) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException;

	public LiteralVariable getLiteralVariable(boolean isNegation, String[] args) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException;

	public LiteralVariable getLiteralVariable(boolean isNegation, Literal[] args) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException;

	public String getCodeBase(boolean isNegation) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException;

	public String getCodeBase(boolean isNegation, String[] args) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException;

	public String getCodeBase(boolean isNegation, Literal[] args) //
			throws AppConstantException, IncorrectNoOfArgumentsException, InvalidArgumentException;
}
