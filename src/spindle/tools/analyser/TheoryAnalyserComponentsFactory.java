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
package spindle.tools.analyser;

import spindle.sys.AppConst;
import spindle.sys.Conf;
import spindle.sys.ConfigurationException;
import spindle.tools.analyser.impl.StronglyConnectedComponents;

/**
 * Theory analyser factory.
 * <p>
 * Provides classes used to anlayser the content of the inputed defeasible theory.
 * </p>
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.5
 * @version Last modified 2012.07.21
 */
public class TheoryAnalyserComponentsFactory {
	private static StronglyConnectedComponents scc = null;

	// public static StronglyConnectedComponents getStronglyConnectedComponentsImpl() throws ConfigurationException {
	// if (null == scc) {
	// String clazzName = Conf.getTheoryAnalyser_stronglyConnectedComponentClassName();
	// try {
	// if (!AppConst.isDeploy) System.out.println("generating scc class [" + clazzName + "]");
	// Class<?> clazz = Class.forName(clazzName);
	// scc = clazz.asSubclass(StronglyConnectedComponents.class).newInstance();
	// if (!AppConst.isDeploy) System.out.println("generating scc class [" + clazzName + "]..success");
	// } catch (Exception e) {
	// if (!AppConst.isDeploy) System.out.println("generating scc class [" + clazzName + "]..failed");
	// throw new ConfigurationException("exception throw while generating scc class [" + clazzName + "]", e);
	// }
	// }
	// return scc;
	// }

	public static StronglyConnectedComponents getStronglyConnectedComponentsImpl() throws ConfigurationException {
		if (Conf.isMultiThreadMode()) {
			return createStronglyConnectedComplement();
		} else if (null == scc) {
			scc = createStronglyConnectedComplement();
		}
		return scc;
	}

	private static StronglyConnectedComponents createStronglyConnectedComplement() throws ConfigurationException {
		StronglyConnectedComponents scc;
		String clazzName = Conf.getTheoryAnalyser_stronglyConnectedComponentClassName();
		try {
			if (!AppConst.isDeploy) System.out.println("generating scc class [" + clazzName + "]");
			Class<?> clazz = Class.forName(clazzName);
			scc = clazz.asSubclass(StronglyConnectedComponents.class).newInstance();
			if (!AppConst.isDeploy) System.out.println("generating scc class [" + clazzName + "]..success");
		} catch (Exception e) {
			if (!AppConst.isDeploy) System.out.println("generating scc class [" + clazzName + "]..failed");
			throw new ConfigurationException("exception throw while generating scc class [" + clazzName + "]", e);
		}
		return scc;
	}
}
