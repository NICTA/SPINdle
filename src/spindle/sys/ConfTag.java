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
package spindle.sys;

/**
 * Configuration tag used in SPINdle.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 */
public interface ConfTag {
	String APP_LICENSE = "license";
	String APP_VERSION = "version";

	String IS_SHOW_PROGRESS = "app.showProgress";
	String IS_SHOW_RESULT = "app.showResult";
	String IS_SAVE_RESULT = "app.saveResult";
	String APP_RESULT_FOLDER = "app.result.folder";
	String APP_PROGRESS_TIME_INTERVAL = "app.progress.timeInterval";
	String APP_REASONING_SLEEPTIME = "app.reasoning.sleepTime";
	String APP_MEMORY_MONITOR_TIME_INTERVAL = "app.memoryMonitor.timeInterval";

	String LOG_LEVEL = "log.level";
	String LOG_FOLDER = "log.folder";
	String LOG_FILE_EXTENSION = "log.filename.extension";

	String REASONER_CONCLUSION_DEFAULT_EXTENSION = "reasoner.conclusion.defaultExt";
	String REASONER_VERSION = "reasoner.version";
	String REASONER_LOG_FILE_PREFIX = "reasoner.log.filename.prefix";
	String IS_GARBAGE_COLLECTION = "reasoner.garbage.collection";
	String REASONER_GARBAGE_COLLECTION_TIME_INTERVAL = "reasoner.garbage.collection.timeInterval";
	String IS_MULTI_THREAD_MODE = "reasoner.multiThreadMode";

	String IS_SEARCH_IO_CLASSES = "app.io.searchClasses";
	String IS_SHOW_STATISTICS = "app.showStatistics";

	String IS_LOG_INFERENCE_PROCESS = "reasoner.logInference";

	String REASONER_WELL_FOUNDED_SEMANTICS = "reasoner.wellFoundedSemantics";
	String REASONER_AMBIGUOUS_PROPAGATION = "reasoner.ambiguityPropagation";
	String REASONER_CONTINUES_WITH_MIXED_TEMPORAL_LITERAL="reasoner.mixLiteralsMode";

	String REASONER_TDL_CONCLUSION_UPDATER="reasoner.tdl.conclusionUpdater";
	
	String THEORY_ANALYSER_STRONGLY_COMPONENT_IMPL = "theoryAnalyser.scc.impl";

	String THEORY_VARIABLE_BOOLEAN_EVALUATOR_ENGINE_NAME = "theoryEvaluator.scriptEngineName";

	String USE_CONSOLE = "console";

	String IS_CONSOLE_MODE = "app.consoleMode";
}
