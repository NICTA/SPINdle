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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;

import spindle.core.dom.Literal;
import spindle.core.dom.Rule;
import spindle.tools.analyser.TheoryAnalyserException;
import spindle.tools.analyser.dom.CLiteral;

/**
 * Search strongly connected literals in a theory using Gabow's algorithm.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.5
 */
public class StronglyConnectedComponentsImpl_gabow extends StronglyConnectedComponentsBase implements StronglyConnectedComponents {

	private List<CLiteral> literalsInTheory = null;
	private List<CLiteral> literalsPending = null;
	private int cnt = 0;
	private int scnt = 0;
	private Stack<CLiteral> path = null;
	private Stack<CLiteral> S = null;

	private List<Set<Literal>> sccLiteralsGroups = null;

	public StronglyConnectedComponentsImpl_gabow() {
		super();
	}

	private void setUp() {
		literalsPending = new Vector<CLiteral>();
		literalsInTheory = new Vector<CLiteral>();
		for (Literal literal : getTheory().getAllLiteralsInRules()) {
			CLiteral cl = new CLiteral(literal);
			literalsPending.add(cl);
			literalsInTheory.add(cl);
		}

		path = new Stack<CLiteral>();
		S = new Stack<CLiteral>();
		cnt = 0;
		scnt = 0;
		sccLiteralsGroups = new ArrayList<Set<Literal>>();
	}

	@Override
	protected List<Set<Literal>> searchStronglyConnectedLiterals() throws TheoryAnalyserException {
		setUp();
		// this.logMessage(0,"scc_gabow\n"+getTheory().toString());
		try {
			CLiteral literal = null;
			while ((literal = getNextLiteral()) != null) {
				scR(literal);
			}
			return sccLiteralsGroups;
		} catch (Exception e) {
			throw new TheoryAnalyserException(e);
		}
	}

	private int findMinimumPre(CLiteral literal) {
		Literal conflictLiteral = literal.getComplementClone();
		CLiteral cl = getCLiteral(conflictLiteral);
		int minPre = literal.getPre();

		if (null != cl && S.contains(cl) && cl.getPre() != Integer.MIN_VALUE && cl.getPre() < minPre) {
			minPre = cl.getPre();
		}
		// logMessage(1, "findMin, literal=" + literal + ", min=" + minPre);
		return minPre;
	}

	private void scR(CLiteral literal) {
		literal.setPre(cnt++);
		S.push(literal);
		path.push(literal);
		// logMessage(1,literal.toString());
		Map<String, Rule> rules = getTheory().getRules(literal);
		for (Rule rule : rules.values()) {
			// logMessage(2,"scR:"+rule.toString());
			if (rule.isHeadLiteral(literal)) {
				if (rule.isBodyLiteral(literal)) {
				}
				// logMessage(3,"is head literal: "+literal);
			} else {
				for (Literal l : rule.getHeadLiterals()) {
					CLiteral cl = getCLiteral(l);
					if (cl.getPre() == Integer.MIN_VALUE) {
						literalsPending.remove(cl);
						scR(cl);
					} else {
						if (cl.getGroupId() == Integer.MIN_VALUE) {
							while (path.size() > 0 && path.peek().getPre() > cl.getPre()) {
								path.pop();
							}
						}
					}
				}
			}
		}
		int minPre = findMinimumPre(literal);
		while (path.size() > 0 && path.peek().getPre() > minPre) {
			path.pop();
		}
		if (path.size() > 0 && path.peek().getPre() == literal.getPre()) {
			path.pop();
		} else {
			logMessage(Level.FINER, 2, "==> return");
			return;
		}

		CLiteral tLiteral = null;
		Set<Literal> sscLiterals = new TreeSet<Literal>();
		do {
			tLiteral = S.pop();
			tLiteral.setGroupId(scnt);
			sscLiterals.add(tLiteral);
		} while (S.size() >= 0 && !tLiteral.equals(literal));
		sccLiteralsGroups.add(sscLiterals);

		logMessage(Level.FINER, 1, "scc group found:[" + sscLiterals.size() + "]-", sscLiterals);
		scnt++;
	}

	private CLiteral getCLiteral(final Literal literal) {
		int l = literalsInTheory.indexOf(literal);
		return l < 0 ? null : literalsInTheory.get(l);
	}

	private CLiteral getNextLiteral() {
		if (literalsPending.size() == 0) return null;
		return literalsPending.remove(0);
	}

	protected Set<Literal> getConflictLiterals(final Literal literal) {
		Set<Literal> literalList = new TreeSet<Literal>();
		literalList.add(literal.getComplementClone());
		return literalList;
	}

}
