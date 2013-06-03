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
package spindle.core.dom;

import java.util.Set;
import java.util.TreeSet;

/**
 * Utilities class for manipulating defeasible theory.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 2.1.2
 * @version Last modified 2012.07.20
 */
public class TheoryUtilities {

	/**
	 * Extract the set of rules that are required to derive the specified literal.
	 * 
	 * @param theory Original theory.
	 * @param literal Literal to derive.
	 * @return A set of rule labels indicating the set of rules that are required to derive the set of literals.
	 * @see #getRulesToDerive(Theory theory, Set literals)
	 */
	public static Set<String> getRulesToDerive(Theory theory, Literal literal) {
		Set<Literal> literals = new TreeSet<Literal>();
		literals.add(literal);
		return getRulesToDerive(theory, literals);
	}

	/**
	 * Extract the set of rules that are required to derive the set specified literals.
	 * 
	 * @param theory Original theory.
	 * @param literals Literals to derive.
	 * @return A set of rule labels indicating the set of rules that are required to derive the set of literals.
	 * @see #getRulesToDerive(Theory theory, Literal literal)
	 * @see Theory#getRulesToDerive(Set literals)
	 */
	public static Set<String> getRulesToDerive(Theory theory, Set<Literal> literals) {
		return theory.getRulesToDerive(literals);
	}

	/**
	 * Return the set of rules after excluding the rules specified from the theory.
	 * 
	 * @param theory Original theory.
	 * @param excludedRules Rules to be excluded
	 * @return Set of rule labels after excluding the rules specified.
	 */
	public static Set<String> getRulesExclude(Theory theory, Set<String> excludedRules) {
		return theory.getRulesExclude(excludedRules);
	}

	/**
	 * Extract the set of facts, rules and superiority relation that are required to derive the specified literal and
	 * return it as a defeasible theory.
	 * 
	 * @param theory Original theory.
	 * @param literal Literal to derive.
	 * @return A new theory that contains the set of rules (including facts, all types of rules, superiorities, etc)
	 *         that are required to derive the literal specified.
	 * @see #getTheoryToDerive(Theory theory, Literal literal)
	 */
	public static Theory getTheoryToDerive(Theory theory, Literal literal) throws TheoryException {
		Set<Literal> literals = new TreeSet<Literal>();
		literals.add(literal);
		return getTheoryToDerive(theory, literals);
	}

	/**
	 * Extract the set of facts, rules and superiority relation that are required to derive the specified literal and
	 * return it as a defeasible theory.
	 * 
	 * @param theory Original theory.
	 * @param literals Literals to derive.
	 * @return A new theory that contains the set of rules (including facts, all types of rules, superiorities, etc)
	 *         that are required to derive the literal specified.
	 * @see #getTheoryToDerive(Theory theory,Set literals)
	 * @see Theory#createNewTheoryWithRules(Set rules)
	 */
	public static Theory getTheoryToDerive(Theory theory, Set<Literal> literals) throws TheoryException {
		Set<String> rules = theory.getRulesToDerive(literals);
		return createNewTheoryWithRules(theory, rules);
	}

	/**
	 * Create a new theory using the rule specified.
	 * 
	 * @param theory Original theory.
	 * @param rules Rules to be included.
	 * @return A new theory that contains the set of rules (including facts, all types of rules, superiorities, etc)
	 *         that are specified.
	 * @throws TheoryException
	 * @see Theory#createNewTheoryWithRules(Set rules)
	 */
	public static Theory createNewTheoryWithRules(Theory theory, Set<String> rules) throws TheoryException {
		return theory.createNewTheoryWithRules(rules);
	}
}
