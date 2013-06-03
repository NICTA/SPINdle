/**
 * SPINdle (version 2.2.0)
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
package spindle.tools.analyser;

import spindle.core.dom.Theory;
import spindle.sys.AppModuleBase;

/**
 * Base class for theory analyser.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.5
 */
public abstract class TheoryAnalyserComponentBase extends AppModuleBase implements TheoryAnalyserComponent {
	private Theory theory = null;

	public TheoryAnalyserComponentBase() {
		super();
	}

	@Override
	public void setTheory(Theory theory) {
		this.theory = theory;
	}

	protected Theory getTheory() {
		return theory;
	}

	@Override
	public void clear() {
		theory = null;
	}
}
