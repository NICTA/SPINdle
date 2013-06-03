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
import java.util.Set;

import spindle.core.MessageType;
import spindle.core.dom.ConclusionType;
import spindle.core.dom.Literal;
import spindle.sys.AppModuleListener;
import spindle.tools.explanation.InferenceLogger;

/**
 * Interface for reasoning engines listener.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.08.20
 */
public interface ReasoningEngineListener extends AppModuleListener {

	void onReasoningEngineMessage(MessageType messageType, String message);

	void setInapplicableLiteralsBeforeInference(Map<ConclusionType, Set<Literal>> inapplicableLiteralsBeforeInference);

	void setInferenceLogger(InferenceLogger inferenceLogger);
}
