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

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import spindle.core.MessageType;
import spindle.core.dom.Literal;
import spindle.core.dom.Rule;
import spindle.core.dom.Temporal;
import spindle.core.dom.TheoryType;
import spindle.engine.TheoryNormalizerException;
import spindle.sys.AppConst;
import spindle.sys.AppFeatureConst;
import spindle.sys.Conf;
import spindle.sys.message.ErrorMessage;
import spindle.sys.message.SystemMessage;

/**
 * TDL Theory Normalizer.
 * <p>
 * Provides methods that can be used to transform a defeasible theory into an equivalent theory without superiority
 * relation or defeater using the algorithms described in:
 * <ul>
 * <li>G. Antoniou, D. Billington, G. Governatori and M.J. Maher (2001) Representation Results for Defeasible Logic,
 * <i>ACM Transactions on Computational Logic</i>, Vol. 2 (2), pp. 255-287</li>
 * </ul>
 * </p>
 * <p>
 * Rule/literal modal conversions and conflict resolutions are based on description presented in:
 * <ul>
 * <li>G. Governatori and A. Rotolo (2008) BIO Logical Agents: Norms, Beliefs, Intentions in Defeasible Logic,
 * <i>Journal of Autonomous Agents and Multi Agent Systems</i>, Vol. 17 (1), pp. 36--69</li>
 * </ul>
 * </p>
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.07.21
 */
public class TdlTheoryNormalizer2 extends spindle.engine.mdl.MdlTheoryNormalizer2 {
	// private static final Comparator<Temporal> TEMPORAL_START_COMPARATOR = new TemporalStartComparator();
	// private LiteralDataStore literalDataStore = null;
	// private TreeMap<Literal, Map<Temporal, List<Literal>>> conflictTemporalLiteralsStore = null;
	//private TreeMap<Literal, List<Literal>> conflictTemporalLiteralsStore = null;
	private boolean isReasoningWithMixedTemporalLiterals=false;

	public TdlTheoryNormalizer2() throws TheoryNormalizerException {
		super();
		if (AppConst.isDeploy)
			throw new TheoryNormalizerException(getClass(), ErrorMessage.THEORY_NORMALIZER_NOT_SUPPORTED, new Object[] { TheoryType.TDL });
		//conflictTemporalLiteralsStore = new TreeMap<Literal, List<Literal>>(new LiteralComparator(false));
		
		isReasoningWithMixedTemporalLiterals=Conf.isReasoningWithMixedTemporalLiterals();
	}

//	@Override
//	protected void verifyConflictRules() throws TheoryNormalizerException {
//		List<Superiority> superiorities = theory.getAllSuperiority();
//		for (Superiority superiority : superiorities) {
//			String superiorRuleId = superiority.getSuperior();
//			String inferiorRuleId = superiority.getInferior();
//
//			Rule superiorRule = factsAndRules.get(superiorRuleId);
//			Rule inferiorRule = factsAndRules.get(inferiorRuleId);
//
//			if (null == superiorRule)
//				throw new TheoryNormalizerException(getClass(), ErrorMessage.THEORY_SUPERIOR_RULE_NOT_FOUND_IN_THEORY,
//						new Object[] { superiorRuleId });
//			if (null == inferiorRule)
//				throw new TheoryNormalizerException(getClass(), ErrorMessage.THEORY_INFERIOR_RULE_NOT_FOUND_IN_THEORY,
//						new Object[] { inferiorRuleId });
//
//			boolean isConflictRule = false;
//			List<Literal> superiorRuleHead = superiorRule.getHeadLiterals();
//			List<Literal> inferiorRuleHead = inferiorRule.getHeadLiterals();
//			
//			for (int j = 0; !isConflictRule && j < superiorRuleHead.size(); j++) {
//				Literal superiorHead = superiorRuleHead.get(j);
//				boolean superiorHeadHasTemporal = superiorHead.hasTemporalInfo();
//				Temporal superiorHeadTemporal = superiorHead.getTemporal();
//				Set<Literal> conflictLiterals = getConflictLiterals(superiorHead);
//				if (!AppConst.isDeploy)
//					System.out.println("TdlTheoryNormalizer2.verifyConflictRules: superiorHead=" + superiorRuleHead.get(j)
//							+ ", conflictLiterals=" + conflictLiterals);
//				for (int i = 0; !isConflictRule && i < inferiorRuleHead.size(); i++) {
//					Literal inferiorHead = inferiorRuleHead.get(i);
//					boolean inferiorHeadHasTemporal = inferiorHead.hasTemporalInfo();
//					Temporal inferiorHeadTemporal = inferiorHead.getTemporal();
//
//					Literal plainInferiorHead=DomUtilities.getPlainLiteral(inferiorHead);					
//					boolean hasConflictPlainHeads = conflictLiterals.contains(plainInferiorHead);
//					if (!AppConst.isDeploy) System.out.println("  hasConflictPlainHeads=" + hasConflictPlainHeads);
//					
//					// continue checking next head literal if the current literal is not in conflict with the superior head literal.
//					if (!hasConflictPlainHeads) continue;
//					
//					// if both the superior rule head and inferior rule head are of temporal literals,
//					// then check whether the inferior rule head (without temporal info) appears in the conflict
//					// literals set or not.
//					// if yes, then check whether the temporal of the two head's literals overlapped each other.
//					// if yes, then are conflicting rules; or not otherwise.				
//					if (superiorHeadHasTemporal && inferiorHeadHasTemporal){
//						if (superiorHeadTemporal.isIntersect(inferiorHeadTemporal) )
//						isConflictRule=true;					
////						if (superiorHeadTemporal.isIntersect(inferiorHeadTemporal) && !superiorHeadTemporal.meet(inferiorHeadTemporal))
////							isConflictRule = true;
//					} else if (!(superiorHeadHasTemporal || inferiorHeadHasTemporal)){
//						isConflictRule=true;
//					} else{						
//						if (!isReasoningWithMixedTemporalLiterals)
//							throw new TheoryNormalizerException(getClass(),ErrorMessage.THEORY_NORMALIZER_PLAIN_AND_TEMPORAL_FORM_OF_SAME_LITERAL_APPEAR_IN_CONFLICTING_HEADS,new Object[]{plainInferiorHead,superiorRuleId,inferiorRuleId});							
//
//						fireTheoryNormalizerMessage(MessageType.WARNING,SystemMessage.THEORY_NORMALIZER_PLAIN_AND_TEMPORAL_FORM_OF_SAME_LITERAL_APPEAR_IN_CONFLICTING_HEADS,new Object[]{plainInferiorHead,superiorRuleId,inferiorRuleId});
//						isConflictRule = true;						
//					}
//					
////					if (superiorHeadHasTemporal) {
////						if (inferiorHeadHasTemporal) {
////							if (superiorHeadTemporal.isIntersect(inferiorHeadTemporal) && !superiorHeadTemporal.meet(inferiorHeadTemporal))
////								isConflictRule = true;
////						} else {
////								isConflictRule = true;
////								fireTheoryNormalizerMessage(MessageType.WARNING, "plain and temporal Literal [" + superiorRuleHead + "," + inferiorRuleHead+"] appeared");
////						}
////					} else {
////						if (inferiorHeadHasTemporal) {
////					isConflictRule = true;
////					fireTheoryNormalizerMessage(MessageType.WARNING, "plain and temporal Literal [" + superiorRuleHead + "," + inferiorRuleHead+"] appeared");
////						} else {
////							//if (conflictLiterals.contains(inferiorRuleHead.get(i))) isConflictRule = true;
////							isConflictRule = true;
////						}
////					}
//
//					// if (superiorHead.hasTemporalInfo() && inferiorHead.hasTemporalInfo()) {
//					// // if (null!=superiorHeadTemporal && null!=inferiorHeadTemporal){
//					// Literal conflictHead = conflictLiterals.floor(inferiorRuleHead.get(i));
//					// if (null != conflictHead) {
//					// if (superiorHeadTemporal.isIntersect(inferiorHeadTemporal) //
//					// && !superiorHeadTemporal.meet(inferiorHeadTemporal)) isConflictRule = true;
//					// // if (superiorHead.getTemporal().isIntersect(inferiorHead.getTemporal())) isConflictRule =
//					// // true;
//					// }
//					// } else if (!superiorHead.hasTemporalInfo() && !inferiorHead.hasTemporalInfo()) {
//					// if (conflictLiterals.contains(inferiorRuleHead.get(i))) isConflictRule = true;
//					// }
//					if (!AppConst.isDeploy)
//						System.out.println("    checking inferior rule head:" + inferiorRuleHead.get(i) + ", isConflictRule="
//								+ isConflictRule);
//				}
//			}
//			if (!isConflictRule) { throw new TheoryNormalizerException(getClass(), ErrorMessage.SUPERIORITY_UNCONFLICTING_RULES,
//					new Object[] { superiorRuleId, inferiorRuleId }); }
//		}
//	}

	@Override
	protected boolean isConflictRules(Rule r1, Rule r2) throws TheoryNormalizerException {
		logMessage(Level.FINER, 1, "verify conflict rules", r1.getLabel(), r2.getLabel());

		boolean isConflictRule = false;
		List<Literal> r1Heads = r1.getHeadLiterals();
		List<Literal> r2Heads = r2.getHeadLiterals();

		for (int j = 0; !isConflictRule && j < r1Heads.size(); j++) {
			Literal r1Head = r1Heads.get(j);
			boolean r1HeadHasTemporal = r1Head.hasTemporalInfo();
			Temporal r1HeadTemporal = r1Head.getTemporal();
			Set<Literal> conflictLiterals = getConflictLiterals(r1Head);
			if (!AppConst.isDeploy)
				System.out.println("TdlTheoryNormalizer2.verifyConflictRules: superiorHead=" + r1Head // r1Heads.get(j)
						+ ", conflictLiterals=" + conflictLiterals);
			for (int i = 0; !isConflictRule && i < r2Heads.size(); i++) {
				Literal r2Head = r2Heads.get(i);
				boolean r2HeadHasTemporal = r2Head.hasTemporalInfo();
				Temporal r2HeadTemporal = r2Head.getTemporal();

				Literal plainInferiorHead=r2Head.cloneWithNoTemporal();
				boolean hasConflictPlainLiteralHeads = conflictLiterals.contains(plainInferiorHead);
				if (!AppConst.isDeploy) System.out.println("  hasConflictPlainLiteralHeads=" + hasConflictPlainLiteralHeads);

				// continue checking next head literal if the two head literals (in plain form) are not conflicting each others.
				if (!hasConflictPlainLiteralHeads) continue;

				// if the two head literals (in plain form) are conflicting each others, return true either:
				// Case 1: when considering superiority relation with same start temporals only
				// (1) the two temporals are having the same start time; or
				// (2) if one of the literals is plain literal, then check whether it holds at -ve inf.
				// Case 2: Otherwise
				// (1) the temporals of the two literals are overlapping each others; or
				// (2) one of them is a temporal literal and the other is an ordinary literal.
				if (r1HeadHasTemporal && r2HeadHasTemporal) {
					if (AppFeatureConst.superiorityWithSameStartTemporalOnly) {
						if (r1HeadTemporal.sameStart(r2HeadTemporal)) isConflictRule = true;
					} else if (r1HeadTemporal.overlap(r2HeadTemporal)) isConflictRule = true;
				} else if (!(r1HeadHasTemporal || r2HeadHasTemporal)) {
					isConflictRule = true;
				} else {
					if (!isReasoningWithMixedTemporalLiterals)
						throw new TheoryNormalizerException(getClass(),
								ErrorMessage.THEORY_NORMALIZER_PLAIN_AND_TEMPORAL_FORM_OF_SAME_LITERAL_APPEAR_IN_CONFLICTING_HEADS,
								new Object[] { plainInferiorHead, r1.getLabel(), r2.getLabel() });

					fireTheoryNormalizerMessage(MessageType.WARNING,
							SystemMessage.THEORY_NORMALIZER_PLAIN_AND_TEMPORAL_FORM_OF_SAME_LITERAL_APPEAR_IN_CONFLICTING_HEADS,
							new Object[] { plainInferiorHead, r1.getLabel(), r2.getLabel() });
					
					if (AppFeatureConst.superiorityWithSameStartTemporalOnly){
						if (r1HeadHasTemporal){
							if (Long.MIN_VALUE==r1HeadTemporal.getStartTime())isConflictRule=true;
						} else {
							if (Long.MIN_VALUE==r2HeadTemporal.getStartTime())isConflictRule=true;
						}
					} else	isConflictRule = true;
				}
			//	if (!AppConst.isDeploy)
				//	System.out.println("    check rule head:"+r1Head+"," + r2Head + " :- isConflictRule=" + isConflictRule);
				logMessage(Level.FINEST, 2, "verify conflict literals", r1Head, r2Head, "isConflictRule=" + isConflictRule);
			}
		}
		return isConflictRule;
	}
	

	// /**
	// * transform the theory to regular form and defeasible rules with multiple heads to single headed rules.
	// */
	// @Override
	// protected void transformTheoryToRegularFormImpl() throws TheoryNormalizerException {
	// try {
	// literalDataStore = new LiteralDataStore(theory);
	// conflictTemporalLiteralsStore = new TreeMap<Literal, Map<Temporal, List<Literal>>>(new LiteralComparator(false));
	//
	// super.transformTheoryToRegularFormImpl();
	// } catch (LiteralDataStoreException e) {
	// throw new TheoryNormalizerException(getClass(), e);
	// }finally{
	// literalDataStore=null;
	// if (null!=conflictTemporalLiteralsStore) {
	// conflictTemporalLiteralsStore.clear();
	// conflictTemporalLiteralsStore=null;
	// }
	// }
	// }

	// protected List<Literal> getConflictLiterals(Literal literal) {
	// if (literal.hasTemporalInfo()) {
	// Map<Temporal, List<Literal>> temporalSet = conflictTemporalLiteralsStore.get(literal);
	// if (null == temporalSet) {
	// temporalSet = new TreeMap<Temporal, List<Literal>>(TEMPORAL_START_COMPARATOR);
	// conflictTemporalLiteralsStore.put(literal, temporalSet);
	// }
	// Temporal temporal = literal.getTemporal();
	// List<Literal> conflictLiterals = temporalSet.get(temporal);
	// if (null == conflictLiterals) {
	// Set<Literal> conflictLiteralsSet = new TreeSet<Literal>();
	// Set<Literal> relatedLiterals = literalDataStore.getRelatedTemporalLiterals(literal);
	// Set<Literal> relatedConflictLiterals = literalDataStore.getRelatedTemporalLiterals(literal.getComplementClone());
	//
	// System.out.println(getClass().getCanonicalName() + ".relatedLiteral(" + literal + ")=" + relatedLiterals);
	// System.out.println(getClass().getCanonicalName() + ".relatedConflictLiterals(" + literal + ")=" +
	// relatedConflictLiterals);
	//
	// for (Literal relatedLiteral : relatedLiterals) {
	// conflictLiteralsSet.addAll(theory.getConflictLiterals(relatedLiteral));
	// }
	// for (Literal relatedConflictLiteral : relatedConflictLiterals) {
	// conflictLiteralsSet.addAll(theory.getConflictLiterals(relatedConflictLiteral.getComplementClone()));
	// }
	// conflictLiterals = new Vector<Literal>(conflictLiteralsSet);
	// temporalSet.put(temporal, conflictLiterals);
	// }
	// System.out.println(getClass().getCanonicalName() + ".getConflictLiterals(" + literal + ")=" + conflictLiterals);
	// return conflictLiterals;
	// // return new TreeSet<Literal>(theory.getConflictLiterals(literal));
	// } else {
	// return theory.getConflictLiterals(literal);
	// }
	// }
}
