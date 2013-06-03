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
package spindle;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.app.utils.FileManager;
import com.app.utils.Utilities;

import spindle.core.AbstractReasonerMessageListener;
import spindle.core.ReasonerException;
import spindle.core.ReasonerMessageListener;
import spindle.core.ReasonerUtilities;
import spindle.core.dom.Theory;
import spindle.sys.AppConst;
import spindle.sys.Conf;
import spindle.sys.ConfTag;
import spindle.sys.MemoryMonitor;
import spindle.sys.Messages;
import spindle.sys.PerformanceStatistic;
import spindle.sys.message.SystemMessage;
import spindle.tools.explanation.InferenceLogger;

/**
 * Main class for running SPINdle.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 * @version Last modified 2012.08.10
 */
public class ReasonerMain {
	public static List<PerformanceStatistic> performanceStatistics = null;
	private static MemoryMonitor memoryMonitor = null;
	private static ReasonerMessageListener messageListener = null;

	private static void runReasoner(URL url) throws ReasonerException {
		PerformanceStatistic ps = new PerformanceStatistic(url);
		performanceStatistics.add(ps);

		Reasoner reasoner = new Reasoner();
		reasoner.addReasonerMessageListener(messageListener);

		try {
			memoryMonitor.reset();
			memoryMonitor.startMonitor();

			ps.setStartLoadTheory();
			reasoner.loadTheory(url);
			Theory theory = reasoner.getTheory();
			ps.setEndLoadTheory();

			ps.setNoOfRules(reasoner.getTheory().getFactsAndAllRules().size());
			ps.setNoOfLiterals(reasoner.getTheory().getAllLiteralsInRules().size());

			ps.setStartNormalFormTransformation();
			reasoner.transformTheoryToRegularForm();
			ps.setEndNormalFormTransformation();

			if (theory.getDefeatersCount() > 0) {
				ps.setStartDefeaterRemoval();
				reasoner.removeDefeater();
				ps.setEndTimeDefeaterRemoval();
			}
			
			switch (Conf.getReasonerVersion()) {
			case 1:
				if (theory.getSuperiorityCount() > 0) {
					ps.setStartSuperiorityRemoval();
					reasoner.removeSuperiority();
					ps.setEndTimeSuperiorityRemoval();
				}
				break;
			default:
			}
			
			ps.setStartReasoning();
			reasoner.getConclusions();
			ps.setEndReasoning();

			ps.setMaxMemoryUsed(memoryMonitor.getMemoryUsed());
			if (Conf.isShowStatistics()) System.out.println(ps.toString());

			if (Conf.isSaveResult()) {
				File fd = null;
				File urlFile = new File(url.getFile());
				if ("".equals(url.getHost())) {
					// create the conclusions folder if it does not exists
					fd = new File(urlFile.getParentFile(), Conf.getResultFolder());
					fd.mkdirs();
				} else {
					fd = new File(Conf.getResultFolder(), url.getFile());
					fd = fd.getParentFile();

					if (null != fd.getParentFile()) fd.getParentFile().mkdirs();
				}

				// create the conclusions file name
				File outFilename = FileManager.changeFileExtension(
						FileManager.addFilenamePostfix(urlFile, "_conclusions"), Conf.getConclusionExt());
				File outFile = new File(fd, outFilename.getName());

				// save conclusions
				reasoner.saveConclusions(outFile);
			}
			if (Conf.isLogInferenceProcess()) {
				InferenceLogger inferenceLogger = reasoner.getInferenceLogger();
				if (null != inferenceLogger) {
					System.out.println("=== Inference Logger - start ===\n" + inferenceLogger
							+ "\n=== Inference Logger -  end  ===");
				}
			}
		} catch (Exception e) {
			throw new ReasonerException("exception throw while reasoning", e);
		} finally {
			reasoner.clear();
			reasoner.removeReasonerMessageListener(messageListener);
			reasoner = null;
		}
	}

	public static void main(String... args) {
		// read in the file and start reasoning process
		System.out.println(ReasonerUtilities.getAppStartMessage());
		System.out.flush();

		Map<String, String> configs = new Hashtable<String, String>();
		List<String> theoryFiles = new ArrayList<String>();
		List<URL> filenames = null;

		// print application message
		boolean isPrintAppMessage = false;
		try {
			Utilities.extractArguments(args, AppConst.ARGUMENT_PREFIX, configs, theoryFiles);

			if (theoryFiles.size() > 0) {
				filenames = new ArrayList<URL>();
				for (String f : theoryFiles) {
					filenames.addAll(ReasonerUtilities.getFilenames(f));
				}
			}

			isPrintAppMessage = ReasonerUtilities.printAppMessage(configs);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.err.flush();
			System.exit(1);
		}

		// reasoning engine start here
		if (configs.containsKey(ConfTag.USE_CONSOLE)) {
			Console console = null;
			try {
				console = new Console(configs);
				console.start((null == filenames || filenames.size() == 0) ? null : filenames);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (null != console) {
					console.terminate();
					console = null;
				}
			}
		} else if (!isPrintAppMessage) {
			if (null == filenames || filenames.size() == 0) {
				System.out.flush();
				System.out.println("\n" + AppConst.APP_USAGE);
				System.out.flush();
				System.err.flush();
				System.err.println(Messages.getSystemMessage(SystemMessage.APPLICATION_NO_THEORY_TO_PROCESS) + "\n");
				System.err.flush();
			} else {
				Conf.initializeApplicationContext(configs);

				long sleepTime = Conf.getReasoningFileSleeptime();

				performanceStatistics = new ArrayList<PerformanceStatistic>();
				memoryMonitor = new MemoryMonitor();
				messageListener = new AbstractReasonerMessageListener(System.out);

				Timer timer = null;
				try {
					timer = new Timer();
					timer.schedule(new TimerTask() {
						@Override
						public void run() {
							memoryMonitor.checkMemoryUsed();
						}
					}, 10, Conf.getMemoryMonitorTimeInterval());

					for (int i = 0; i < filenames.size(); i++) {
						URL filename = filenames.get(i);
						try {
							runReasoner(filename);
						} catch (Exception e) {
							e.printStackTrace(System.out);
							System.out.flush();
						} finally {
							System.gc();
						}

						if (i != filenames.size() - 1) {
							try {
								long m = performanceStatistics.get(performanceStatistics.size() - 1).getNoOfRules() / 8000;
								if (m > 1) m = 1;
								Thread.sleep(m * sleepTime);
							} catch (Exception e) {
							}
						}
					}
				} finally {
					if (null != timer) {
						timer.cancel();
						timer = null;
					}

					// print out the performance statistics
					ReasonerUtilities.printPerformanceStatistics(performanceStatistics);
					performanceStatistics = null;
				}
			}
		}
	}
}
