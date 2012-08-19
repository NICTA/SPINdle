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
package spindle.engine.tdl;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.core.dom.Temporal;
import spindle.engine.ReasoningEngineFactoryException;
import spindle.engine.mdl.MdlReasoningEngine2;
import spindle.sys.AppConst;

public class TdlReasoningEngine2 extends MdlReasoningEngine2 {

	private Map<Literal, Map<Temporal,ConclusionType>> literalsTemporalInfo = new TreeMap<Literal, Map<Temporal,ConclusionType>>();

	private Map<Literal, Literal> basicLiterals = new TreeMap<Literal, Literal>();

	public TdlReasoningEngine2() throws ReasoningEngineFactoryException {
		super();
		if (AppConst.isDeploy) throw new ReasoningEngineFactoryException("Reasoning engine is currently not support!");
	}

	protected void generateTheoryBasicLiterals(){
		Set<Literal> literalsInTheory = theory.getAllLiteralsInRules();
		for (Literal literal : literalsInTheory) {
			basicLiterals.put(literal, generateBasicLiteral(literal));
		}
	}
	
	
	protected Literal generateBasicLiteral(Literal literal) {
		Literal basicLiteral = literal.clone();
			basicLiteral.setTemporal(null);
			return basicLiteral;
	}
	
	
	protected void addLiteralTemporalInfo(Literal literal,ConclusionType conclusionType){
Literal		basicLiteral=basicLiterals.get(literal);

		Map<Temporal,ConclusionType> literalTemporalInfo = literalsTemporalInfo.get(basicLiteral);
		if (null == literalTemporalInfo) {
			literalTemporalInfo = new TreeMap<Temporal,ConclusionType>();
			literalsTemporalInfo.put(basicLiteral, literalTemporalInfo);
		}
		literalTemporalInfo.put(literal.getTemporal(),conclusionType);
	}

	
	
}
