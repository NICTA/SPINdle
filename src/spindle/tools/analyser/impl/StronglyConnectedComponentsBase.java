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
package spindle.tools.analyser.impl;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import spindle.core.dom.Literal;
import spindle.tools.analyser.TheoryAnalyserComponentBase;
import spindle.tools.analyser.TheoryAnalyserException;

/**
 * Base class for searching strongly connected literals in theory.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.5
 */
public abstract class StronglyConnectedComponentsBase extends TheoryAnalyserComponentBase implements StronglyConnectedComponents {
	public StronglyConnectedComponentsBase() {
		super();
	}

	@Override
	public List<Set<Literal>> getStronglyConnectedLiterals() throws TheoryAnalyserException {
		if (null == getTheory()) throw new TheoryAnalyserException("theory is null");
		logMessage(Level.FINE, 0, "===");
		logMessage(Level.FINE, 0, "=== SCC Analyser - start");
		logMessage(Level.FINE, 0, "===");
		try {
			return searchStronglyConnectedLiterals();
		} catch (Exception e) {
			throw new TheoryAnalyserException(e);
		} finally {
			logMessage(Level.FINE, 0, "===");
			logMessage(Level.FINE, 0, "=== SCC Analyser - end");
			logMessage(Level.FINE, 0, "===");
		}
	}

	protected abstract List<Set<Literal>> searchStronglyConnectedLiterals() throws TheoryAnalyserException;

}
