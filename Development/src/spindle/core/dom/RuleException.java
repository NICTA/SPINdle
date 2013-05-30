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
package spindle.core.dom;

/**
 * Signals an exception of some sort has occurred when manipulating the rule.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.0.0
 * @since 2011.07.27
 * @version Last modified 2011.07.27
 */
public class RuleException extends TheoryException {

	private static final long serialVersionUID = 1L;

	public RuleException(String errorTag) {
		this(errorTag, null, null, (Object[]) null);
	}

	public RuleException(Throwable cause) {
		this(null, null, cause, (Object[]) null);
	}

	public RuleException(String errorTag, Throwable cause) {
		this(errorTag, null, cause, (Object[]) null);
	}

	public RuleException(String errorTag, String message) {
		this(errorTag, message, null, (Object[]) null);
	}

	public RuleException(String errorTag, Object... arguments) {
		this(errorTag, null, null, arguments);
	}

	public RuleException(String errorTag, String message, Object... arguments) {
		this(errorTag, message, null, (Object[]) arguments);
	}

	public RuleException(String errorTag, Throwable cause, Object... arguments) {
		this(errorTag, null, cause, arguments);
	}

	protected RuleException(String errorTag, String message, Throwable cause, Object... arguments) {
		super(errorTag, message, cause, arguments);
	}

}
