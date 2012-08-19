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
package spindle.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.app.utils.FileManager;
import com.app.utils.Utilities.ProcessStatus;

import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.Theory;
import spindle.engine.ReasoningEngine;
import spindle.engine.ReasoningEngineFactory;
import spindle.engine.ReasoningEngineFactoryException;
import spindle.engine.ReasoningEngineListener;
import spindle.engine.TheoryNormalizer;
import spindle.engine.TheoryNormalizerListener;
import spindle.io.IOManager;
import spindle.io.OutputterException;
import spindle.io.outputter.XmlTheoryOutputter;
import spindle.sys.AppConst;
import spindle.sys.AppLogger;
import spindle.sys.AppModuleBase;
import spindle.sys.AppModuleListener;
import spindle.sys.Conf;
import spindle.sys.Messages;
import spindle.sys.message.ErrorMessage;
import spindle.sys.message.SystemMessage;
import spindle.tools.evaluator.LiteralVariablesEvaluator;
import spindle.tools.evaluator.LiteralVariablesEvaluatorException;
import spindle.tools.evaluator.LiteralVariablesEvaluatorListener;
import spindle.tools.explanation.InferenceLogger;

/**
 * Base class for SPINdle reasoner.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.07.21
 */
public abstract class ReasonerBase extends AppModuleBase //
		implements AppLogger, LiteralVariablesEvaluatorListener, TheoryNormalizerListener, ReasoningEngineListener {

	protected static String LINE_SEPARATOR = FileManager.LINE_SEPARATOR;

	private LiteralVariablesEvaluator literalVariableEvaluator = null;
	private TheoryNormalizer theoryNormalizer = null;
	private ReasoningEngine reasoningEngine = null;

	protected Theory origTheory = null;
	protected Theory workingTheory = null;

	protected Map<Literal, Map<ConclusionType, Conclusion>> conclusions = null;
	protected List<Conclusion> conclusionsAsList = null;

	protected Map<ConclusionType, Set<Literal>> inapplicableLiteralsBeforeInference = null;
	protected InferenceLogger inferenceLogger = null;

	private boolean isTheoryChanged = false;

	public ReasonerBase() {
		super();
		if (!Conf.isInitialized()) {
			System.out.println(ReasonerUtilities.getAppStartMessage());
			Conf.initializeApplicationContext(null);
		}
	}

	public ProcessStatus loadTheory(final Theory theory) throws ReasonerException {
		if (null == theory) throw new ReasonerException(ErrorMessage.THEORY_NULL_THEORY);
		if (theory.isEmpty()) throw new ReasonerException(ErrorMessage.THEORY_EMPTY_THEORY);

		clear();

		try {
			origTheory = theory;
			workingTheory = origTheory.clone();

			isTheoryChanged = true;
			conclusions = null;

			fireOnReasonerMessage(MessageType.INFO, Messages.getSystemMessage(
					SystemMessage.REASONER_THEORY_LOADED_SUCCESSFULLY, new Object[] { workingTheory.getTheoryType()
							.name() }));

			if (Conf.isShowProgress()) fireOnReasonerMessage(MessageType.INFO, "Theory read:", workingTheory);

			return ProcessStatus.SUCCESS;
		} catch (Exception e) {
			clear();
			throw new ReasonerException(e);
		}
	}

	protected LiteralVariablesEvaluator getLiteralVariablesEvaluator() throws ReasonerException {
		if (null == workingTheory) throw new ReasonerException(ErrorMessage.THEORY_NULL_THEORY);
		if (null == literalVariableEvaluator) {
			literalVariableEvaluator = ReasoningEngineFactory.getLiteralVariablesEvaluator();
			literalVariableEvaluator.addLiteralVariablesEvaluatorListener(this);
			literalVariableEvaluator.setAppLogger(this);
		}
		return literalVariableEvaluator;
	}

	/**
	 * get the theory normalizer according to the theory type
	 * 
	 * @return theory normalizer
	 * @throws ReasonerException
	 *             throw when theory is null or theory type is not defined
	 */
	protected TheoryNormalizer getTheoryNormalizer() throws ReasonerException {
		if (null == workingTheory) throw new ReasonerException(ErrorMessage.THEORY_NULL_THEORY);
		if (isTheoryChanged) {
			if (null != theoryNormalizer) theoryNormalizer.removeTheoryNormalizerListener(this);
			theoryNormalizer = null;
		}

		if (null == theoryNormalizer) {
			try {
				theoryNormalizer = ReasoningEngineFactory.getTheoryNormalizer(workingTheory.getTheoryType());
				theoryNormalizer.addTheoryNormalizerListener(this);
				theoryNormalizer.setAppLogger(this);
			} catch (ReasoningEngineFactoryException e) {
				fireOnReasonerMessage(MessageType.ERROR, e.getMessage());
				throw new ReasonerException("getTheoryNormalizer exception", e);
			}
			isTheoryChanged = false;
		}
		theoryNormalizer.setTheory(workingTheory);
		return theoryNormalizer;
	}

	/**
	 * Get the reasoning engine according to the theory type.
	 * 
	 * @return an instance of reasoner
	 * @throws ReasonerException
	 */
	protected ReasoningEngine getReasoningEngine() throws ReasonerException {
		if (null == workingTheory) throw new ReasonerException(ErrorMessage.THEORY_NULL_THEORY);
		if (isTheoryChanged) {
			if (null != reasoningEngine) reasoningEngine.removeReasoningEngineListener(this);
			reasoningEngine = null;
		}
		if (null == reasoningEngine) {
			try {
				reasoningEngine = ReasoningEngineFactory.getReasoningEngine(workingTheory);
				reasoningEngine.addReasoningEngineListener(this);
				reasoningEngine.setAppLogger(this);
			} catch (ReasoningEngineFactoryException e) {
				fireOnReasonerMessage(MessageType.ERROR, e.getMessage());
				throw new ReasonerException(e);
			}
			isTheoryChanged = false;
		}
		return reasoningEngine;
	}

	public ProcessStatus transformTheoryToRegularForm() throws ReasonerException {
		if (workingTheory.getLiteralVariableCount() > 0 || workingTheory.getLiteralBooleanFunctionCount() > 0) {
			fireOnReasonerMessage(MessageType.INFO, "remove literal variables in theory");
			try {
				workingTheory = getLiteralVariablesEvaluator().evaluateLiteralVariables(workingTheory);
				if (Conf.isShowProgress()) fireOnReasonerMessage(MessageType.INFO, null, workingTheory);
			} catch (LiteralVariablesEvaluatorException e) {
				fireOnReasonerMessage(MessageType.ERROR, e.getMessage());
				throw new ReasonerException(
						"Literal variables evaluator exception throw while evaluating literal variable", e);
			}
			onLogMessage(Level.INFO, "=== literal variables removal:", workingTheory);
		} else {
			fireOnReasonerMessage(MessageType.INFO,
					Messages.getSystemMessage(SystemMessage.THEORY_CONTAINS_NO_LITERAL_VARIABLES));
		}

		return doTransformTheoryToRegularForm();
	}

	/**
	 * set the conclusions and extract only conclusion with non-place holder literals
	 * 
	 * @param tempConclusions
	 * @return Process status
	 * @throws ReasonerException
	 */
	protected ProcessStatus setConclusions(Map<Literal, Map<ConclusionType, Conclusion>> tempConclusions)
			throws ReasonerException {
		if (null == tempConclusions || tempConclusions.size() == 0) throw new ReasonerException(
				ErrorMessage.CONCLUSION_NULL_CONCLUSION);
		conclusions = new TreeMap<Literal, Map<ConclusionType, Conclusion>>();
		Set<Conclusion> tempConclusionList = new TreeSet<Conclusion>();
		for (Entry<Literal, Map<ConclusionType, Conclusion>> entry : tempConclusions.entrySet()) {
			Literal literal = entry.getKey();
			if (!literal.isPlaceHolder()) {
				conclusions.put(literal, entry.getValue());
				for (Conclusion conclusion : entry.getValue().values()) {
					tempConclusionList.add(conclusion);
				}
			}
		}
		conclusionsAsList = new ArrayList<Conclusion>(tempConclusionList);
		return ProcessStatus.SUCCESS;
	}

	public List<Conclusion> getConclusionsAsList() throws ReasonerException {
		fireOnReasonerMessage(MessageType.INFO, Messages.getSystemMessage(SystemMessage.REASONER_GET_CONCLUSION_AS_SET));
		if (null == conclusions) getConclusions();
		return conclusionsAsList;
	}

	public String getConclusionsAsXmlString() throws ReasonerException {
		try {
			return XmlTheoryOutputter.getConclusionsAsXmlString(getConclusionsAsList());
		} catch (OutputterException e) {
			fireOnReasonerMessage(MessageType.ERROR, e.getMessage());
			throw new ReasonerException("Exception throw while executing getConclusionsAsXmlString()", e);
		}
	}

	public ProcessStatus saveTheoryAs(final File filename) throws ReasonerException {
		if (null == workingTheory) throw new ReasonerException(ErrorMessage.THEORY_NULL_THEORY);
		if (workingTheory.isEmpty()) throw new ReasonerException(ErrorMessage.THEORY_EMPTY_THEORY);

		String msg = Messages.getSystemMessage(SystemMessage.IO_SAVE_THEORY, new Object[] { filename });
		fireOnReasonerMessage(MessageType.INFO, msg);
		onLogMessage(Level.INFO, msg);

		try {
			return IOManager.save(filename, workingTheory, this);
		} catch (OutputterException e) {
			fireOnReasonerMessage(MessageType.ERROR, e.getMessage());
			throw new ReasonerException(Messages.getErrorMessage(ErrorMessage.IO_OUTPUTTER_THEORY_SAVE_EXCEPTION,
					new Object[] { filename }), e);
		}
	}

	public ProcessStatus saveConclusions(final File filename) throws ReasonerException {
		if (null == conclusionsAsList || conclusionsAsList.size() == 0) throw new ReasonerException(
				ErrorMessage.CONCLUSION_NULL_CONCLUSION);

		String filenameStr = filename.toString();
		String msg = Messages.getSystemMessage(SystemMessage.IO_SAVE_CONCLUSIONS, new Object[] { filenameStr });
		fireOnReasonerMessage(MessageType.INFO, msg);
		onLogMessage(Level.INFO, msg);

		try {
			return IOManager.save(filename, conclusionsAsList, this);
		} catch (OutputterException e) {
			fireOnReasonerMessage(MessageType.ERROR, e.getMessage());
			throw new ReasonerException("Theory exception throw while saving conclusions to [" + filenameStr + "]", e);
		}
	}

	public Theory getTheory() {
		return workingTheory;
	}

	public Theory getOriginalTheory() {
		return origTheory;
	}

	public void printConclusions() {
		if (null == conclusions || conclusions.size() == 0) {
			this.fireOnReasonerMessage(MessageType.WARNING,
					Messages.getErrorMessage(ErrorMessage.CONCLUSION_NULL_CONCLUSION));
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.getSystemMessage(SystemMessage.REASONER_CONCLUSIONS_GENERATED));
		if (AppConst.isPrintConclusionByType) {
			List<Conclusion> cLst = conclusionsAsList;
			for (Conclusion c : cLst) {
				sb.append(LINE_SEPARATOR).append(AppConst.IDENTATOR).append(c.toString());
			}
		} else {
			for (Entry<Literal, Map<ConclusionType, Conclusion>> entry : conclusions.entrySet()) {
				sb.append(LINE_SEPARATOR).append(AppConst.IDENTATOR).append("literal [").append(entry.getKey())
						.append("]");
				for (Conclusion conclusion : entry.getValue().values()) {
					sb.append(LINE_SEPARATOR).append(AppConst.IDENTATOR).append(conclusion.toString());
				}
			}
		}
		onLogMessage(Level.INFO, sb.toString());
		fireOnReasonerMessage(MessageType.INFO, sb.toString());
	}

	public void onLogMessage(Level logLevel, String message, Object... objects) {
		onLogMessage(logLevel, 0, message, objects);
	}

	@Override
	public void onLogMessage(Level logLevel, int indentLevel, String message, Object... objects) {
		logMessage(logLevel, indentLevel, message, objects);
	}

	@Override
	public void onReasoningEngineMessage(MessageType messageType, String message) {
		fireOnReasonerMessage(messageType, message);
	}

	public Map<ConclusionType, Set<Literal>> getInapplicableLiteralsBeforeInference() {
		return inapplicableLiteralsBeforeInference;
	}

	@Override
	public void setInapplicableLiteralsBeforeInference(
			Map<ConclusionType, Set<Literal>> inapplicableLiteralsBeforeInference) {
		this.inapplicableLiteralsBeforeInference = inapplicableLiteralsBeforeInference;
	}

	public InferenceLogger getInferenceLogger() {
		return inferenceLogger;
	}

	@Override
	public void setInferenceLogger(InferenceLogger inferenceLogger) {
		this.inferenceLogger = inferenceLogger;
	}

	@Override
	public void onTheoryNormalizerMessage(MessageType messageType, String message) {
		fireOnReasonerMessage(messageType, message);
	}

	@Override
	public void onLiteralVariablesEvaluatorMesage(MessageType messageType, String message) {
		fireOnReasonerMessage(messageType, message);
	}

	protected ProcessStatus clear() {
		if (null != origTheory) {
			origTheory.clear();
			origTheory = null;
		}
		if (null != workingTheory) {
			workingTheory.clear();
			workingTheory = null;
		}
		theoryNormalizer = null;
		reasoningEngine = null;
		isTheoryChanged = false;
		return ProcessStatus.SUCCESS;
	}

	public Map<Literal, Map<ConclusionType, Conclusion>> generateConclusionsWithTransformations()
			throws ReasonerException {
		if (conclusions == null) {
			transformTheoryToRegularForm();

			if (workingTheory.getDefeatersCount() > 0) removeDefeater();

			switch (Conf.getReasonerVersion()) {
			case 1:
				if (workingTheory.getSuperiorityCount() > 0) removeSuperiority();
				break;
			default:
			}

			getConclusions();
		}
		if (Conf.isShowResult() || Conf.isShowProgress()) printConclusions();
		return conclusions;
	}

	// =========================
	// Reasoner Listener - start
	// =========================
	public void addReasonerMessageListener(ReasonerMessageListener listener) {
		addAppModuleListener(listener);
	}

	public void removeReasonerMessageListener(ReasonerMessageListener listener) {
		removeAppModuleListener(listener);
	}

	protected void fireOnReasonerMessage(final MessageType messageType, final String message, Object... objects) {
		for (AppModuleListener listener : getAppModuleListeners()) {
			if (listener instanceof ReasonerMessageListener) {
				((ReasonerMessageListener) listener).onReasonerMessage(messageType, message, objects);
			}
		}
	}

	// =======================
	// Reasoner Listener - end
	// =======================

	protected abstract ProcessStatus doTransformTheoryToRegularForm() throws ReasonerException;

	public abstract ProcessStatus removeDefeater() throws ReasonerException;

	public abstract ProcessStatus removeSuperiority() throws ReasonerException;

	public abstract Map<Literal, Map<ConclusionType, Conclusion>> getConclusions() throws ReasonerException;

}
