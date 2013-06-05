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
package spindle.tools.analyser;

import java.util.List;
import java.util.Set;

import spindle.core.dom.Literal;
import spindle.core.dom.Theory;

import spindle.sys.AppModuleBase;
import spindle.sys.message.ErrorMessage;
import spindle.tools.analyser.impl.StronglyConnectedComponents;

/**
 * Theory analyser controller class.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.5
 */
public class TheoryAnalyser extends AppModuleBase {
	private Theory theory = null;

	public void setTheory(Theory theory) throws TheoryAnalyserException {
		if (null == theory) throw new TheoryAnalyserException(ErrorMessage.THEORY_NULL_THEORY);
		this.theory = theory;
	}

	public List<Set<Literal>> getStronglyConnectedLiterals() throws TheoryAnalyserException {
		if (null == theory) throw new TheoryAnalyserException(ErrorMessage.THEORY_NULL_THEORY);
		StronglyConnectedComponents scc = null;
		try {
			scc = TheoryAnalyserComponentsFactory.getStronglyConnectedComponentsImpl();
			scc.setAppLogger(logger);
			scc.setTheory(theory);
			return scc.getStronglyConnectedLiterals();
		} catch (Exception e) {
			throw new TheoryAnalyserException(e);
		} finally {
			scc.resetAppLogger();
			scc.clear();
		}
	}
}
