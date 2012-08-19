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
package spindle.engine;

import java.util.Map;
import java.util.TreeMap;

import com.app.utils.TextUtilities;

import spindle.core.dom.Theory;
import spindle.core.dom.TheoryType;
import spindle.sys.AppConst;
import spindle.sys.Conf;
import spindle.sys.message.ErrorMessage;
import spindle.tools.evaluator.LiteralVariablesEvaluator;

/**
 * Factory class for theory normalizer and reasoning engine
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.07.21
 */
public final class ReasoningEngineFactory {

	private static enum ENGINE_TYPE {
		AB, AP, AB_WF, AP_WF
	};

	private static LiteralVariablesEvaluator literalVariableEvaluator = null;
	private static Map<TheoryType, TheoryNormalizer> theoryNormalizersStore = new TreeMap<TheoryType, TheoryNormalizer>();
	private static Map<TheoryType, Map<ENGINE_TYPE, Map<Integer, ReasoningEngine>>> reasoningEnginesStore = new TreeMap<TheoryType, Map<ENGINE_TYPE, Map<Integer, ReasoningEngine>>>();

	/**
	 * Return a copy of literal variables evaluator.
	 * 
	 * @return literal varaiable evaluator
	 */
	public static final LiteralVariablesEvaluator getLiteralVariablesEvaluator() {
		if (Conf.isMultiThreadMode()) return new LiteralVariablesEvaluator();
		if (null == literalVariableEvaluator) literalVariableEvaluator = new LiteralVariablesEvaluator();
		return literalVariableEvaluator;
	}

	/**
	 * Create a new theory normalizer according to the theory type.
	 * 
	 * @param theoryType theory type
	 * @return theory normalizer associated with the theory type
	 * @throws ReasoningEngineFactoryException Indicates when there is no theory normalizer associated with the theory
	 *             type specified or the theory type does not exist.
	 */
	private static final TheoryNormalizer createTheoryNormalizer(TheoryType theoryType)
			throws ReasoningEngineFactoryException {
		TheoryNormalizer theoryNormalizer = null;
		switch (theoryType) {
		case SDL:
			switch (Conf.getReasonerVersion()){
			case 1:
			theoryNormalizer = new spindle.engine.sdl.SdlTheoryNormalizer();
			break;
			default:
				theoryNormalizer = new spindle.engine.sdl.SdlTheoryNormalizer2();
			}
			break;
		case MDL:
			switch (Conf.getReasonerVersion()){
			case 1:
			theoryNormalizer = new spindle.engine.mdl.MdlTheoryNormalizer();
			break;
			default:
				theoryNormalizer = new spindle.engine.mdl.MdlTheoryNormalizer2();
			}
			break;
		case TDL:
			// only available when the package is not in deploy mode
			// - used for testing purpose
			if (!AppConst.isDeploy) {
				theoryNormalizer = new spindle.engine.tdl.TdlTheoryNormalizer();
			}
			break;
		default:
			throw new ReasoningEngineFactoryException(ErrorMessage.THEORY_UNRECOGNIZED_THEORY_TYPE);
		}
		return theoryNormalizer;
	}

	/**
	 * Return a theory normalizer as specified if the type of theory normalizer is in the theory normalizers store.
	 * or create a new one otherwise.
	 * 
	 * @param theoryType theory type
	 * @return theory normalizer associated with the theory type
	 * @throws ReasoningEngineFactoryException Indicates when there is no theory normalizer associated with the theory
	 *             type specified or the theory type does not exist.
	 */
	private static final TheoryNormalizer getTheoryNormalizerFromStore(TheoryType theoryType)
			throws ReasoningEngineFactoryException {
		TheoryNormalizer theoryNormalizer = theoryNormalizersStore.get(theoryType);
		if (null == theoryNormalizer) {
			theoryNormalizer = createTheoryNormalizer(theoryType);
			theoryNormalizersStore.put(theoryType, theoryNormalizer);
		}
		return theoryNormalizer;
	}

	/**
	 * Return a theory normalizer according to the theory type.
	 * 
	 * @param theoryType theory type
	 * @return theory normalizer associated with the theory type
	 * @throws ReasoningEngineFactoryException Indicates when there is no theory normalizer associated with the theory
	 *             type specified or the theory type does not exist.
	 */
	public static final TheoryNormalizer getTheoryNormalizer(TheoryType theoryType)
			throws ReasoningEngineFactoryException {
		if (theoryType == null) return null;
		if (Conf.isMultiThreadMode()) {
			return createTheoryNormalizer(theoryType);
		} else {
			return getTheoryNormalizerFromStore(theoryType);
		}
	}

	/**
	 * Create a reasoning engine according to the theory type, engine type and reasoner version.
	 * 
	 * @param theoryType theory type.
	 * @param engineType engine type (ambiguity blocking, ambiguity propagation, well-founded semantics, or their
	 *            combination).
	 * @param reasonerVersion reasoner version.
	 * @return Reasoning engine with appropriate type as specified.
	 * @throws ReasoningEngineFactoryException
	 */
	private static final ReasoningEngine createReasoningEngine(TheoryType theoryType, ENGINE_TYPE engineType,
			int reasonerVersion) throws ReasoningEngineFactoryException {
		ReasoningEngine engine = null;
		switch (theoryType) {
		case SDL:
			switch (reasonerVersion) {
			case 1:
				switch (engineType) {
				case AB:
				case AB_WF:
					engine = new spindle.engine.sdl.SdlReasoningEngine();
					break;
				case AP:
				case AP_WF:
					engine = new spindle.engine.sdl.SdlReasoningEngineAP();
					break;
				}
				break;
			default:
				switch (engineType) {
				case AB:
				case AB_WF:
					engine = new spindle.engine.sdl.SdlReasoningEngine2();
					break;
				case AP:
				case AP_WF:
					engine = new spindle.engine.sdl.SdlReasoningEngineAP2();
					break;
				}
			}
			break;
		case MDL:
			switch (reasonerVersion) {
			case 1:
				switch (engineType) {
				case AB:
				case AB_WF:
					engine = new spindle.engine.mdl.MdlReasoningEngine();
					break;
				case AP:
				case AP_WF:
					engine = new spindle.engine.mdl.MdlReasoningEngineAP();
					break;
				}
				break;
			default:
				switch (engineType) {
				case AB:
				case AB_WF:
					engine = new spindle.engine.mdl.MdlReasoningEngine2();
					break;
				case AP:
				case AP_WF:
					engine = new spindle.engine.mdl.MdlReasoningEngineAP2();
					break;
				}
			}
			break;
		case TDL: // TDL support only available in version 2 onward
			engine = new spindle.engine.tdl.TdlReasoningEngine2();
			break;
		default:
			throw new ReasoningEngineFactoryException(ErrorMessage.THEORY_UNRECOGNIZED_THEORY_TYPE);
		}
		return engine;
	}

	/**
	 * Return a reasoning engine as specified if the type of engine is in the reasoning engines store;
	 * or create a new one otherwise.
	 * 
	 * @param theoryType theory type.
	 * @param engineType engine type (ambiguity blocking, ambiguity propagation, well-founded semantics, or their
	 *            combination).
	 * @param reasonerVersion reasoner version.
	 * @return Reasoning engine with appropriate type as specified.
	 * @throws ReasoningEngineFactoryException
	 */
	private static final ReasoningEngine getReasoningEngineFromStore(TheoryType theoryType, ENGINE_TYPE engineType,
			int reasonerVersion) throws ReasoningEngineFactoryException {
		Map<ENGINE_TYPE, Map<Integer, ReasoningEngine>> enginesSetByTheoryType = reasoningEnginesStore.get(theoryType);
		if (null == enginesSetByTheoryType) {
			enginesSetByTheoryType = new TreeMap<ENGINE_TYPE, Map<Integer, ReasoningEngine>>();
			reasoningEnginesStore.put(theoryType, enginesSetByTheoryType);
		}
		Map<Integer, ReasoningEngine> enginesSet = enginesSetByTheoryType.get(engineType);
		if (null == enginesSet) {
			enginesSet = new TreeMap<Integer, ReasoningEngine>();
			enginesSetByTheoryType.put(engineType, enginesSet);
		}
		ReasoningEngine engine = enginesSet.get(reasonerVersion);
		if (null == engine) {
			engine = createReasoningEngine(theoryType, engineType, reasonerVersion);
			enginesSet.put(reasonerVersion, engine);
		}
		engine.clear();
		return engine;
	}

	/**
	 * Return a reasoning engine according to the theory type, reasoning mode and reasoner version required.
	 * 
	 * @param theory defeasible theory
	 * @return reasoning engine associated with the theory type and reasoning mode configured.
	 * @throws ReasoningEngineFactoryException Indicates when there is no reasoning engine associated with the theory
	 *             type or reasoning mode specified.
	 */
	public static final ReasoningEngine getReasoningEngine(Theory theory) throws ReasoningEngineFactoryException {
		if (theory == null) return null;
		TheoryType theoryType = theory.getTheoryType();

		if (Conf.isShowProgress() || !AppConst.isDeploy) {
			StringBuilder sb = new StringBuilder();
			sb.append("Reasoning engine information\n----------------------------\nTheory type: ").append(theoryType) //
					.append(", Reasoner version: ").append(Conf.getReasonerVersion());

			if (Conf.isReasoningWithAmbiguityPropagation()) sb.append("\n - Ambiguity Propagation");
			if (Conf.isReasoningWithWellFoundedSemantics()) sb.append("\n - Well-Founded Semantics");
			System.out.println(TextUtilities.generateHighLightedMessage(sb.toString()));
		}

		ENGINE_TYPE engineType = Conf.isReasoningWithAmbiguityPropagation() ? ENGINE_TYPE.AP : ENGINE_TYPE.AB;

		if (Conf.isMultiThreadMode()) {
			return createReasoningEngine(theoryType, engineType, Conf.getReasonerVersion());
		} else {
			return getReasoningEngineFromStore(theoryType, engineType, Conf.getReasonerVersion());
		}
	}
}
