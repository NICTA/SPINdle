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
package spindle.io;

import spindle.core.SpindleException;

/**
 * Indicates a component mismatch while parsing a defeasible theory.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since 2011.07.26
 * @since version 2.1.0
 * @version Last modified 2012.08.06
 * @deprecated As of version 2.1.0, the DFL theory parser class {@link spindle.io.parser.DflTheoryParser} is replaced by
 *             {@link spindle.io.parser.DflTheoryParser2}. The component mismatch exception class will no longer be used
 *             anymore.
 */
@Deprecated
public class ComponentMismatchException extends SpindleException {

	private static final long serialVersionUID = 1L;

	public ComponentMismatchException(String errorTag) {
		this(errorTag, (Object[]) null);
	}

	public ComponentMismatchException(String errorTag, Object... arguments) {
		super(null, errorTag, null, null, arguments);
	}

}
