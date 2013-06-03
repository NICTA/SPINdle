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
package spindle.engine.tdl;

import spindle.engine.ReasoningEngineException;

/**
 * Signals that an exception of some sort has occurred in the literal data store.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.2.1
 * @version Last modified 2012.10.17
 */
public class LiteralDataStoreException extends ReasoningEngineException {
	private static final long serialVersionUID = 1L;

	public LiteralDataStoreException(String errorTag) {
		this(errorTag, null, null);
	}

	public LiteralDataStoreException(Throwable cause) {
		this(null, null, cause);
	}

	public LiteralDataStoreException(String errorTag, Throwable cause) {
		this(errorTag, null, cause);
	}

	public LiteralDataStoreException(String errorTag, Object... arguments) {
		this(errorTag, null, null, arguments);
	}

	public LiteralDataStoreException(String errorTag, String message, Throwable cause, Object... arguments) {
		super(null, errorTag, message, cause, arguments);
	}

}
