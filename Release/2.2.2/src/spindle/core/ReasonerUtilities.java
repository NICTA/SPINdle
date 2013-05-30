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
package spindle.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.app.exception.InvalidArgumentException;
import com.app.utils.Converter;
import com.app.utils.FileManager;
import com.app.utils.FileSelector;
import com.app.utils.TextUtilities;

import spindle.io.IOManager;
import spindle.sys.AppConst;
import spindle.sys.Conf;
import spindle.sys.ConfTag;
import spindle.sys.Messages;
import spindle.sys.PerformanceStatistic;
import spindle.sys.message.ErrorMessage;
import spindle.sys.message.SystemMessage;

/**
 * Utilities class for SPINdle reasoner.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 */
public class ReasonerUtilities {
	private static String LINE_SEPARATOR = FileManager.LINE_SEPARATOR;

	private static String TEXT_START = Messages.getSystemMessage(SystemMessage.APPLICATION_TEXT_START);
	private static String TEXT_END = Messages.getSystemMessage(SystemMessage.APPLICATION_TEXT_END);

	private static String PERFORMANCE_STATISTICS_HEADER_LINE = null;
	private static String PERFORMANCE_STATISTICS_HEADER = null;
	private static String PERFORMANCE_STATISTICS_HEADER_EMPTY = null;

	private static FileSelector fileSelector = null;

	private static String APPLICATION_TITLE = null;
	private static String APPLICATION_START_MESSAGE = null;

	private static FileSelector getFileSelector() {
		if (null == fileSelector) {
			fileSelector = new FileSelector();
			fileSelector.addExt(IOManager.getParserTypes());
		}
		return fileSelector;
	}

	private static String getApplicationTitle() {
		if (null == APPLICATION_TITLE) APPLICATION_TITLE = AppConst.APP_TITLE + " (version " + AppConst.APP_VERSION + ")";
		return APPLICATION_TITLE;
	}

	public static String getAppStartMessage() {
		if (null == APPLICATION_START_MESSAGE)
			APPLICATION_START_MESSAGE = TextUtilities.generateHighLightedMessage(getApplicationTitle() + LINE_SEPARATOR
					+ AppConst.APP_COPYRIGHT_MESSAGE);
		return APPLICATION_START_MESSAGE;
	}

	public static boolean printAppMessage(final Map<String, String> args) throws ReasonerException {
		StringBuilder sb = new StringBuilder();
		try {
			if (args.containsKey(ConfTag.APP_LICENSE)) {
				sb.append(Conf.getLicense()).append(LINE_SEPARATOR);
			} else if (args.containsKey(ConfTag.APP_VERSION)) {
				sb.append(LINE_SEPARATOR);
			} else if (args.containsKey(ConfTag.USE_CONSOLE)) {
				sb.append(LINE_SEPARATOR).append(Messages.getSystemMessage(SystemMessage.CONSOLE_CONSOLE_MODE_START));
			}
			if (sb.length() > 0) {
				System.out.println(sb.toString());
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static String getPerformanceStatistics_noResults() {
		if (null == PERFORMANCE_STATISTICS_HEADER_EMPTY) {
			StringBuilder sb = new StringBuilder();
			sb.append(LINE_SEPARATOR).append("===");
			sb.append(LINE_SEPARATOR).append("=== No performance statistics found!!");
			sb.append(LINE_SEPARATOR).append("===");
			PERFORMANCE_STATISTICS_HEADER_EMPTY = sb.toString();
		}
		return PERFORMANCE_STATISTICS_HEADER_EMPTY;
	}

	private static String getPerformanceStatisticsHeaderLine() {
		if (null == PERFORMANCE_STATISTICS_HEADER_LINE) {
			StringBuilder sb = new StringBuilder();
			sb.append(LINE_SEPARATOR).append("+------------+------------");
			switch (Conf.getReasonerVersion()) {
			case 1:
				sb.append(TextUtilities.repeatStringPattern("+-----------------", 6));
				break;
			default:
				sb.append(TextUtilities.repeatStringPattern("+-----------------", 5));
			}
			sb.append("+-------------+-----");
			PERFORMANCE_STATISTICS_HEADER_LINE = sb.toString();
		}
		return PERFORMANCE_STATISTICS_HEADER_LINE;
	}

	private static String getPerformanceStatisticsTemplate() {
		if (null == PERFORMANCE_STATISTICS_HEADER) {
			StringBuilder sb = new StringBuilder();
			sb.append(LINE_SEPARATOR).append("====================================");
			sb.append(LINE_SEPARATOR).append("== Performance statistics summary ==");
			sb.append(LINE_SEPARATOR).append("====================================");
			sb.append(LINE_SEPARATOR).append("== I/O classes configuration time used: {0}");
			sb.append(LINE_SEPARATOR).append("== No. of record(s) found: {1}");
			sb.append(LINE_SEPARATOR).append("== --- start");
			sb.append(getPerformanceStatisticsHeaderLine());
			sb.append(LINE_SEPARATOR);
			switch (Conf.getReasonerVersion()) {
			case 1:
				sb.append("|   No. of   |   No. of   |   Time used on  |   Time used on  |   Time used on  |   Time used on  |  Time used on   |    Total time   | Max. Memory |");
				sb.append(LINE_SEPARATOR);
				sb.append("|    Rules   |  Literals  |  loading theory | transform theory| remove defeater | rmv superiority |    reasoning    |       used      |     used    | filename");
				break;
			default:
				sb.append("|   No. of   |   No. of   |   Time used on  |   Time used on  |   Time used on  |  Time used on   |    Total time   | Max. Memory |");
				sb.append(LINE_SEPARATOR);
				sb.append("|    Rules   |  Literals  |  loading theory | transform theory| remove defeater |    reasoning    |       used      |     used    | filename");
			}
			sb.append(getPerformanceStatisticsHeaderLine());
			sb.append(LINE_SEPARATOR);
			sb.append("{2}");
			sb.append(getPerformanceStatisticsHeaderLine());
			sb.append(LINE_SEPARATOR).append("== --- end").append(LINE_SEPARATOR);

			PERFORMANCE_STATISTICS_HEADER = sb.toString();
		}
		return PERFORMANCE_STATISTICS_HEADER;
	}

	public static void printPerformanceStatistics(List<PerformanceStatistic> performanceStatistics) {
		if (null == performanceStatistics) return;
		System.out.flush();
		StringBuilder sb = new StringBuilder();
		if (performanceStatistics.size() == 0) {
			sb.append(getPerformanceStatistics_noResults());
		} else {
			Collections.sort(performanceStatistics);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream writer = new PrintStream(baos);

			switch (Conf.getReasonerVersion()) {
			case 1:
				for (PerformanceStatistic rp : performanceStatistics) {
					writer.append(LINE_SEPARATOR);
					writer.printf(
							"| %10d | %10d | %11.3f sec | %11.3f sec | %11.3f sec | %11.3f sec | %11.3f sec | %11.3f sec | %8.2f MB | %s", //
							rp.getNoOfRules(), rp.getNoOfLiterals(),//
							.001 * rp.getLoadTheoryTimeUsed(), //
							.001 * rp.getNormalFormTransformationTimeUsed(), //
							.001 * rp.getDefeaterRemovalTimeUsed(),//
							.001 * rp.getSuperiorityRemovalTimeUsed(), //
							.001 * rp.getReasoningTimeUsed(), //
							.001 * rp.getTotalTimeUsed(), //
							1.0 * rp.getMaxMemoryUsed() / 1024 / 1024,//
							rp.getUrl().toString());
				}
				break;
			default:
				for (PerformanceStatistic rp : performanceStatistics) {
					writer.append(LINE_SEPARATOR);
					writer.printf("| %10d | %10d | %11.3f sec | %11.3f sec | %11.3f sec | %11.3f sec | %11.3f sec | %8.2f MB | %s", //
							rp.getNoOfRules(), rp.getNoOfLiterals(),//
							.001 * rp.getLoadTheoryTimeUsed(), //
							.001 * rp.getNormalFormTransformationTimeUsed(), //
							.001 * rp.getDefeaterRemovalTimeUsed(),//
							.001 * rp.getReasoningTimeUsed(), //
							.001 * rp.getTotalTimeUsed(), //
							1.0 * rp.getMaxMemoryUsed() / 1024 / 1024,//
							rp.getUrl().toString());
				}
			}
			writer.flush();

			Object[] args = { Converter.long2TimeString(IOManager.getConfigurationTimeUsed()), performanceStatistics.size(),
					baos.toString() };

			try {
				sb.append(TextUtilities.formatArguments(getPerformanceStatisticsTemplate(), args.length, args));
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			}
		}
		System.out.println(sb.toString());
		System.out.flush();
	}

	public static List<URL> getFilenames(String filenameStr) throws IOException, URISyntaxException {
		List<URL> filenames = new ArrayList<URL>();

		URI uri = new URI(filenameStr.replaceAll("\\\\", "/"));
		if (null == uri.getScheme() || uri.getScheme().length() < 3 || "file:".equals(uri.getScheme())) {
			File file = new File(filenameStr);
			if (file.exists()) {
				if (file.isDirectory()) {
					File[] fs = file.listFiles(getFileSelector());
					for (File f : fs) {
						filenames.add(new URL("file", "", f.getCanonicalPath()));
					}
				} else {
					filenames.add(new URL("file", "", file.getCanonicalPath()));
				}
			} else {
				throw new IOException(Messages.getErrorMessage(ErrorMessage.IO_FILE_NOT_EXIST, new Object[] { filenameStr }));
			}
		} else {
			filenames.add(uri.toURL());
		}
		return filenames;
	}

	public static String getTextSectionStart(String sectionText) {
		StringBuilder sb = new StringBuilder();
		sb.append("===\n=== ").append(sectionText).append(TEXT_START).append("\n===");
		return sb.toString();
	}

	public static String getTextSectionEnd(String sectionText) {
		StringBuilder sb = new StringBuilder();
		sb.append("===\n=== ").append(sectionText).append(TEXT_END).append("\n===");
		return sb.toString();
	}

}
