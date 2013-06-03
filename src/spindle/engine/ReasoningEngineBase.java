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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;

import com.app.utils.Utilities.ProcessStatus;

import spindle.core.MessageType;
import spindle.core.dom.Conclusion;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.Rule;
import spindle.core.dom.RuleExt;
import spindle.core.dom.RuleType;
import spindle.core.dom.Superiority;
import spindle.core.dom.Theory;
import spindle.core.dom.TheoryException;
import spindle.sys.AppConst;
import spindle.sys.AppModuleBase;
import spindle.sys.AppModuleListener;
import spindle.sys.Conf;
import spindle.sys.Messages;
import spindle.sys.message.ErrorMessage;
import spindle.sys.message.SystemMessage;
import spindle.tools.analyser.TheoryAnalyser;
import spindle.tools.explanation.InferenceLogger;

/**
 * Base class for reasoning engines.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.07.21
 */
public abstract class ReasoningEngineBase extends AppModuleBase implements ReasoningEngine {

	private static final String SUMMARY_BORDER = "=================================================";

	private static ReasoningEngineUtilities reasoningEngineUtilities = null;

	protected Theory theory = null;
	private TheoryAnalyser _theoryAnalyser = null;

	protected Map<String, Rule> strictRules = null;
	protected Map<String, Rule> defeasibleRules = null;

	protected Map<Literal, Map<ConclusionType, Conclusion>> _conclusions = null;

	protected Map<Literal, Map<ConclusionType, Conclusion>> records = null;

	private Timer timer = null;

	protected boolean isUpdateInapplicableLiteralsBeforeInference = true;
	protected Map<ConclusionType, Set<Literal>> inapplicableLiteralsBeforeInference = null;

	protected boolean isLogInferenceProcess = false;
	private InferenceLogger inferenceLogger = null;

	public ReasoningEngineBase() {
		super();
		inapplicableLiteralsBeforeInference = new TreeMap<ConclusionType, Set<Literal>>();
	}

	@Override
	public Map<Literal, Map<ConclusionType, Conclusion>> getConclusions(final Theory theory)
			throws ReasoningEngineException {
		clear();
		if (theory == null) throw new ReasoningEngineException(getClass(), ErrorMessage.THEORY_NULL_THEORY);
		if (theory.isEmpty())throw new ReasoningEngineException(getClass(),ErrorMessage.THEORY_EMPTY_THEORY);

		// this.theory = (Conf.isCloneTheoryInReasoner()) ? theory.clone() : theory;
		this.theory = theory;

		if (this.theory.getLiteralVariablesInRulesCount() > 0) throw new ReasoningEngineException(getClass(),
				ErrorMessage.REASONING_ENGINE_LITERAL_VARIABLES_NOT_YET_EVALUATED);
		if (this.theory.getLiteralBooleanFunctionCount() > 0) throw new ReasoningEngineException(getClass(),
				ErrorMessage.REASONING_ENGINE_LITERAL_BOOLEAN_FUNCTION_NOT_YET_EVALUATED);

		if (this.theory.getFactsCount() > 0) throw new ReasoningEngineException(getClass(),
				ErrorMessage.THEORY_NOT_IN_REGULAR_FORM_CONTAINS_FACT);
		if (this.theory.getDefeatersCount() > 0) throw new ReasoningEngineException(getClass(),
				ErrorMessage.THEORY_NOT_IN_REGULAR_FORM_CONTAINS_DFEATER);
		switch (Conf.getReasonerVersion()) {
		case 1:
			if (this.theory.getSuperiorityCount() > 0) throw new ReasoningEngineException(getClass(),
					ErrorMessage.THEORY_NOT_IN_REGULAR_FORM_CONTAINS_SUPERIORITY_RELATION);
			break;
		default:
		}

		// assume the theory is normalized,
		// i.e., no facts, defeaters and superiority is needed to be considered
		strictRules = this.theory.getRules(RuleType.STRICT);
		defeasibleRules = this.theory.getRules(RuleType.DEFEASIBLE);

		records = new TreeMap<Literal, Map<ConclusionType, Conclusion>>();

		if (Conf.isShowProgress()) {
			StringBuilder sb = new StringBuilder();
			sb.append(SUMMARY_BORDER) //
					.append("\ntheory (regular form) summary") //
					.append("\n-----------------------------") //
					.append("\ntheory size=" + this.theory.getFactsAndAllRules().size()) //
					.append("\nnumber of literals=" + this.theory.getAllLiteralsInRules().size()) //
					.append("\nnumber of strict rule(s)=" + this.theory.getStrictRulesCount()) //
					.append("\nnumber of defeasible rule(s)=" + this.theory.getDefeasibleRulesCount()) //
					.append("\n").append(SUMMARY_BORDER);
			System.out.println(sb.toString());
		}
		try {
			isLogInferenceProcess = Conf.isLogInferenceProcess();

			_initialize();

			fireSetInapplicableLiteralsBeforeInference();
			isUpdateInapplicableLiteralsBeforeInference = false;

			if (Conf.isReasonerGarbageCollection()) {
				timer = new Timer();
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						System.gc();
					}
				}, Conf.getGarbageCollectionTimeInterval() + 20, Conf.getGarbageCollectionTimeInterval());
			}
			if (Conf.isShowProgress()) {
				if (null == timer) timer = new Timer();
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						fireOnReasoningEngineMessage(MessageType.INFO, getProgressMessage());
					}
				}, Conf.getShowProgressTimeInterval() + 10, Conf.getShowProgressTimeInterval());
			}

			_generateConclusions();
			if (Conf.isShowProgress()) System.out.println(Messages
					.getSystemMessage(SystemMessage.REASONING_ENGINE_ALL_PENDING_CONCLUSIONS_ARE_EVALUATED));
			if (isLogInferenceProcess) fireSetInferenceLogger();
			_terminate();
			return _conclusions;
		} catch (ReasoningEngineException e) {
			throw e;
		} finally {
			if (timer != null) {
				timer.cancel();
				timer = null;
			}
		}
	}

	protected void removeRules(Set<String> ruleLabels) throws ReasoningEngineException {
		logMessage(Level.FINE, 0, "removeRules:", ruleLabels);
		try {
			for (String ruleLabel : ruleLabels) {
				removeRule(ruleLabel);
			}
		} catch (ReasoningEngineException e) {
			throw e;
		} catch (Exception e) {
			throw new ReasoningEngineException(getClass(), e);
		}
	}

	protected void removeRule(String ruleLabel) throws ReasoningEngineException {
		try {
			Rule rule = theory.getRule(ruleLabel);
			Rule ruleRemoved = null;
			switch (rule.getRuleType()) {
			case STRICT:
				ruleRemoved = strictRules.remove(ruleLabel);
				if (null == ruleRemoved) throw new ReasoningEngineException(getClass(),
						"removeRule: strict rule NOT found! ruleLabel=" + ruleLabel);
				try {
					theory.removeRule(ruleLabel);
					logMessage(Level.FINE, 0, "removeRule: strict rule", ruleLabel, " - removed");
				} catch (TheoryException e) {
					throw new ReasoningEngineException(getClass(), e);
				}
				break;
			case DEFEASIBLE:
				ruleRemoved = defeasibleRules.get(ruleLabel);
				if (null == ruleRemoved) throw new ReasoningEngineException(getClass(),
						"removeRule: defeasible rule NOT found! ruleLabel=" + ruleLabel);
				try {
					switch (Conf.getReasonerVersion()) {
					case 2:
						// update superiority rules counter
						Set<Superiority> superiorities = theory.getSuperior(ruleLabel);
						if (null != superiorities) {
							List<Superiority> s2 = new Vector<Superiority>(superiorities);
							for (int i = s2.size() - 1; i >= 0; i--) {
								Superiority sup = s2.get(i);
								((RuleExt) theory.getRule(sup.getInferior())).strongerRulesCountDecrement();
								theory.remove(sup);
							}
						}
						superiorities = theory.getInferior(ruleLabel);
						if (null != superiorities) {
							List<Superiority> s2 = new Vector<Superiority>(superiorities);
							for (int i = s2.size() - 1; i >= 0; i--) {
								Superiority sup = s2.get(i);
								((RuleExt) theory.getRule(sup.getSuperior())).weakerRulesCountDecrement();
								theory.remove(sup);
							}
						}
						break;
					default:
					}
					theory.removeRule(ruleLabel);
					logMessage(Level.FINE, 0, "removeRule: defeasiblerule", ruleLabel, " - removed");
				} catch (TheoryException e) {
					throw new ReasoningEngineException(getClass(), e);
				}
				break;
			default:
			}
		} catch (ReasoningEngineException e) {
			throw e;
		} catch (Exception e) {
			throw new ReasoningEngineException(getClass(), "removeRule" + ruleLabel + "\n======" + theory, e);
		}
	}

	protected List<Literal> getConflictLiterals(final Literal literal) {
		List<Literal> conflictLiterals = theory.getConflictLiterals(literal);
		logMessage(Level.FINEST, 2, "*** ", literal, "conflict literals=", conflictLiterals);
		return conflictLiterals;
	}

	private ProcessStatus _generateConclusions() throws ReasoningEngineException {
		logMessage(Level.INFO, 0,
				Messages.getSystemMessage(SystemMessage.REASONING_ENGINE_CONCLUSIONS_GENERATION_START));
		generateConclusions();
		logMessage(Level.INFO, 0, Messages.getSystemMessage(SystemMessage.REASONING_ENGINE_CONCLUSIONS_GENERATION_END));

		return ProcessStatus.SUCCESS;
	}

	private ProcessStatus _initialize() throws ReasoningEngineException {
		logMessage(Level.INFO, 0, Messages.getSystemMessage(SystemMessage.REASONING_ENGINE_INITIALIZE_START));
		initialize();
		logMessage(Level.INFO, 0, Messages.getSystemMessage(SystemMessage.REASONING_ENGINE_INITIALIZE_END));

		return ProcessStatus.SUCCESS;
	}

	private ProcessStatus _terminate() throws ReasoningEngineException {
		logMessage(Level.INFO, 0, Messages.getSystemMessage(SystemMessage.REASONING_ENGINE_TERMINATE_START));
		terminate();
		logMessage(Level.INFO, 0, Messages.getSystemMessage(SystemMessage.REASONING_ENGINE_TERMINATE_END));

		return ProcessStatus.SUCCESS;
	}

	protected ProcessStatus setConclusion(Map<Literal, Map<ConclusionType, Conclusion>> conclusions) {
		this._conclusions = conclusions;
		return ProcessStatus.SUCCESS;
	}

	/**
	 * verify conclusions - remove conflicting conclusions from the conclusion list
	 * 
	 * @return list of consolidated conclusions
	 */
	protected synchronized Map<Literal, Map<ConclusionType, Conclusion>> verifyConclusions(
			Map<Literal, Map<ConclusionType, Conclusion>> pendingConclusionSet) {
		Map<Literal, Map<ConclusionType, Conclusion>> verifiedConclusions = new TreeMap<Literal, Map<ConclusionType, Conclusion>>(
				pendingConclusionSet);
		Set<Conclusion> conclusionsToRemove = new TreeSet<Conclusion>();

		StringBuilder sb = null;
		if (!AppConst.isDeploy) sb = new StringBuilder();

		if (AppConst.isDeploy) {
			// if in deploy mode, execute the loop without appending the string builder.
			for (Literal literal : pendingConclusionSet.keySet()) {
				Map<ConclusionType, Conclusion> conclusionsSet = pendingConclusionSet.get(literal);
				Map<ConclusionType, Conclusion> complementConclusionsSet = pendingConclusionSet.get(literal
						.getComplementClone());
				for (Conclusion conclusion : conclusionsSet.values()) {
					if (conclusion.isConflictWith(conclusionsSet.values())) {
						conclusionsToRemove.add(conclusion);
					}
					if (complementConclusionsSet != null) {
						if (conclusion.isConflictWith(complementConclusionsSet.values())) {
							conclusionsToRemove.add(conclusion);
						}
					}
				}
			}
		} else {
			// otherwise, execute the loop with string appending.
			// code duplicated here due to efficiency purpose
			for (Literal literal : pendingConclusionSet.keySet()) {
				if (!AppConst.isDeploy) sb.append(LINE_SEPARATOR).append("for literal: ").append(literal.toString());
				Map<ConclusionType, Conclusion> conclusionsSet = pendingConclusionSet.get(literal);
				Map<ConclusionType, Conclusion> complementConclusionsSet = pendingConclusionSet.get(literal
						.getComplementClone());
				for (Conclusion conclusion : conclusionsSet.values()) {
					ConclusionType conclusionType = conclusion.getConclusionType();
					if (!AppConst.isDeploy) sb.append(LINE_SEPARATOR).append(AppConst.IDENTATOR)
							.append("Check conclusion: " + conclusion.toString());
					if (conclusion.isConflictWith(conclusionsSet.values())) {
						conclusionsToRemove.add(conclusion);
						if (!AppConst.isDeploy) sb.append(LINE_SEPARATOR).append(AppConst.IDENTATOR)
								.append("conclusion contains conflict, remove conclusion type: ")
								.append(conclusionType.toString());
					}
					if (complementConclusionsSet != null) {
						if (conclusion.isConflictWith(complementConclusionsSet.values())) {
							conclusionsToRemove.add(conclusion);
							if (!AppConst.isDeploy) sb.append(LINE_SEPARATOR).append(AppConst.IDENTATOR)
									.append("conclusion contains conflict, remove conclusion type: ")
									.append(conclusionType.toString());
						}
					}
				}
			}
			logMessage(Level.INFO, 0, sb.toString());
		}
		if (conclusionsToRemove.size() > 0) {
			for (Conclusion conclusionToRemove : conclusionsToRemove) {
				Map<ConclusionType, Conclusion> conclusionsList = verifiedConclusions.get(conclusionToRemove
						.getLiteral());
				ConclusionType conclusionType = conclusionToRemove.getConclusionType();
				conclusionsList.remove(conclusionType);
				if (conclusionsList.size() == 0) verifiedConclusions.remove(conclusionToRemove.getLiteral());
			}
		}

		return verifiedConclusions;
	}

	protected void addInapplicableLiteralsBeforeInference(Literal literal, ConclusionType conclusionType) {
		if (!isUpdateInapplicableLiteralsBeforeInference) return;
		Set<Literal> literalsSet = inapplicableLiteralsBeforeInference.get(conclusionType);
		if (null == literalsSet) {
			literalsSet = new TreeSet<Literal>();
			inapplicableLiteralsBeforeInference.put(conclusionType, literalsSet);
		}
		literalsSet.add(literal);
	}

	public Set<Literal> getInapplicableLiteralsBeforeInference(ConclusionType conclusionType) {
		return inapplicableLiteralsBeforeInference.get(conclusionType);
	}

	/**
	 * clear the engine data
	 * 
	 * @return Process status
	 */
	@Override
	public ProcessStatus clear() {
		theory = null;

		strictRules = null;
		defeasibleRules = null;

		records = null;

		_conclusions = null;

		_theoryAnalyser = null;

		isUpdateInapplicableLiteralsBeforeInference = true;

		return ProcessStatus.SUCCESS;
	}

	protected TheoryAnalyser getTheoryAnalyser() throws ReasoningEngineException {
		try {
			if (null == _theoryAnalyser) {
				_theoryAnalyser = new TheoryAnalyser();
				_theoryAnalyser.setAppLogger(logger);
			}
			_theoryAnalyser.setTheory(theory);
			return _theoryAnalyser;
		} catch (Exception e) {
			throw new ReasoningEngineException(getClass(), e);
		}
	}

	protected ReasoningEngineUtilities getReasoningEngineUtilities() {
		if (null == reasoningEngineUtilities) reasoningEngineUtilities = new ReasoningEngineUtilities();
		return reasoningEngineUtilities;
	}

	protected InferenceLogger getInferenceLogger() {
		if (null == inferenceLogger) inferenceLogger = new InferenceLogger();
		return inferenceLogger;
	}

	protected ProcessStatus addRecord(Conclusion conclusion) {
		logMessage(Level.FINE, 3, "record added: ", conclusion);

		Literal literal = conclusion.getLiteral();
		Map<ConclusionType, Conclusion> recordsList = records.get(literal);
		if (null == recordsList) {
			recordsList = new TreeMap<ConclusionType, Conclusion>();
			records.put(literal, recordsList);
		}
		recordsList.put(conclusion.getConclusionType(), conclusion);

		return ProcessStatus.SUCCESS;
	}

	protected ProcessStatus removeRecord(Conclusion conclusion) {
		Literal literal = conclusion.getLiteral();
		Map<ConclusionType, Conclusion> recordList = records.get(literal);
		if (null != recordList) {
			recordList.remove(conclusion.getConclusionType());
			if (recordList.size() == 0) records.remove(literal);
		}
		return ProcessStatus.SUCCESS;
	}

	protected boolean isRecordExist(Literal literal, ConclusionType conclusionType) {
		if (records == null) return false;
		Map<ConclusionType, Conclusion> recordList = records.get(literal);
		if (null == recordList) return false;
		return recordList.containsKey(conclusionType);
	}

	protected boolean isRecordExist(List<Literal> literals, ConclusionType conclusionType) {
		for (Literal literal : literals) {
			if (isRecordExist(literal, conclusionType)) return true;
		}
		return false;
	}

	protected ProcessStatus newLiteralFind_definiteProvable(final Literal literal, final boolean isCheckInference) {
		logMessage(Level.FINE, 1, "newLiteralFind_definiteProvable", literal);
		Conclusion conclusion1 = new Conclusion(ConclusionType.DEFINITE_PROVABLE, literal);
		Conclusion conclusion2 = new Conclusion(ConclusionType.DEFEASIBLY_PROVABLE, literal);
		Conclusion conclusion3 = new Conclusion(ConclusionType.TENTATIVELY_PROVABLE, literal);

		addPendingConclusion(conclusion1);
		addRecord(conclusion1);
		addRecord(conclusion2);
		addRecord(conclusion3);
		if (isCheckInference) {
			checkInference(conclusion1);
		}
		return ProcessStatus.SUCCESS;
	}

	protected ProcessStatus newLiteralFind_definiteNotProvable(final Literal literal, final boolean isCheckInference) {
		logMessage(Level.FINE, 1, "newLiteralFind_definiteNotProvable", literal);
		Conclusion conclusion = new Conclusion(ConclusionType.DEFINITE_NOT_PROVABLE, literal);
		addPendingConclusion(conclusion);
		addRecord(conclusion);
		if (isCheckInference) {
			checkInference(conclusion);
		}
		return ProcessStatus.SUCCESS;
	}

	protected ProcessStatus newLiteralFind_defeasiblyProvable(final Literal literal, final boolean isCheckInference) {
		logMessage(Level.FINE, 1, "newLiteralFind_defeasiblyProvable", literal);
		Conclusion conclusion = new Conclusion(ConclusionType.TENTATIVELY_PROVABLE, literal);
		addRecord(conclusion);
		if (isCheckInference) {
			checkInference(conclusion);
		}
		return ProcessStatus.SUCCESS;
	}

	protected ProcessStatus newLiteralFind_defeasiblyNotProvable(final Literal literal, final boolean isCheckInference) {
		logMessage(Level.FINE, 1, "newLiteralFind_defeasiblyNotProvable", literal);
		Conclusion conclusion = new Conclusion(ConclusionType.TENTATIVELY_NOT_PROVABLE, literal);
		addRecord(conclusion);
		if (isCheckInference) {
			checkInference(conclusion);
		}
		return ProcessStatus.SUCCESS;
	}

	// =================================
	// Reasoning Engine Listener - start
	// =================================
	@Override
	public void addReasoningEngineListener(ReasoningEngineListener listener) {
		addAppModuleListener(listener);
	}

	@Override
	public void removeReasoningEngineListener(ReasoningEngineListener listener) {
		removeAppModuleListener(listener);
	}

	protected void fireOnReasoningEngineMessage(MessageType messageType, String message) {
		for (AppModuleListener listener : getAppModuleListeners()) {
			if (listener instanceof ReasoningEngineListener) {
				((ReasoningEngineListener) listener).onReasoningEngineMessage(messageType, message);
			}
		}
	}

	private void fireSetInapplicableLiteralsBeforeInference() {
		for (AppModuleListener listener : getAppModuleListeners()) {
			if (listener instanceof ReasoningEngineListener) {
				((ReasoningEngineListener) listener)
						.setInapplicableLiteralsBeforeInference((new TreeMap<ConclusionType, Set<Literal>>(
								inapplicableLiteralsBeforeInference)));
			}
		}
	}

	private void fireSetInferenceLogger() {
		for (AppModuleListener listener : getAppModuleListeners()) {
			if (listener instanceof ReasoningEngineListener) {
				((ReasoningEngineListener) listener).setInferenceLogger(inferenceLogger);
			}
		}
	}

	// ===============================
	// Reasoning Engine Listener - end
	// ===============================

	// ======================================
	// Abstract functions declaration - start
	// ======================================
	protected abstract void initialize() throws ReasoningEngineException;

	protected abstract void generateConclusions() throws ReasoningEngineException;

	protected abstract ProcessStatus addPendingConclusion(Conclusion conclusion);

	protected abstract ProcessStatus checkInference(Conclusion conclusion);

	protected abstract String getProgressMessage();

	protected abstract void terminate() throws ReasoningEngineException;

	// ====================================
	// Abstract functions declaration - end
	// ====================================
}
