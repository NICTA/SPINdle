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
package spindle.sys;

import java.net.URL;
import java.util.Date;

import com.app.utils.Converter;

/**
 * Performance statistics.
 * <p>
 * To store different performance values during the reasoning process.
 * </p>
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @version Last modified 2012.07.17
 * @since version 1.0.0
 */
public class PerformanceStatistic implements Comparable<Object> {
	private static final String headString = "======================================================================";

	private URL url = null;
	private long noOfRules = 0;
	private long noOfLiterals = 0;
	private long loadTheoryStartTime = 0;
	private long loadTheoryEndTime = 0;
	private long reasoningStartTime = 0;
	private long reasoningEndTime = 0;
	private long normalFormTransformationStartTime = 0;
	private long normalFormTransformationEndTime = 0;
	private long defeaterRemovalStartTime = 0;
	private long defeaterRemovalEndTime = 0;
	private long superiorityRemovalStartTime = 0;
	private long superiorityRemovalEndTime = 0;
	private long maxMemoryUsed = 0;

	public PerformanceStatistic() {
		this(null);
	}

	public PerformanceStatistic(final URL url) {
		this.url = url;
	}

	public void setNoOfRules(final long noOfRules) {
		this.noOfRules = noOfRules;
	}

	public void setNoOfLiterals(final long noOfLiterals) {
		this.noOfLiterals = noOfLiterals;
	}

	public void setStartLoadTheory() {
		loadTheoryStartTime = System.currentTimeMillis();
		loadTheoryEndTime = loadTheoryStartTime;
		// System.out.println("Theory load time started: " + ((new Date(loadTheoryStartTime)).toString()));
	}

	public void setEndLoadTheory() {
		loadTheoryEndTime = System.currentTimeMillis();
		// System.out.println("Theory load time ended: " + ((new Date(loadTheoryEndTime)).toString()));
	}

	public long getLoadTheoryTimeUsed() {
		return loadTheoryEndTime - loadTheoryStartTime;
	}

	public void setStartReasoning() {
		reasoningStartTime = System.currentTimeMillis();
		reasoningEndTime = reasoningStartTime;
		// System.out.println("Reasoning time start: " + ((new Date(reasoningStartTime)).toString()));
	}

	public void setEndReasoning() {
		reasoningEndTime = System.currentTimeMillis();
		// System.out.println("Reasoning time ended: " + ((new Date(reasoningEndTime)).toString()));
	}

	public void setStartNormalFormTransformation() {
		normalFormTransformationStartTime = System.currentTimeMillis();
		normalFormTransformationEndTime = normalFormTransformationStartTime;
		// System.out.println("Normal form transformation time start: " + ((new
		// Date(normalFormTransformationStartTime)).toString()));
	}

	public void setEndNormalFormTransformation() {
		normalFormTransformationEndTime = System.currentTimeMillis();
		// System.out.println("Normal form transformation time ended: " + ((new
		// Date(normalFormTransformationEndTime)).toString()));
	}

	public void setStartDefeaterRemoval() {
		defeaterRemovalStartTime = System.currentTimeMillis();
		defeaterRemovalEndTime = defeaterRemovalStartTime;
		// System.out.println("Remove defeater time start: " + ((new Date(defeaterRemovalStartTime)).toString()));
	}

	public void setEndTimeDefeaterRemoval() {
		defeaterRemovalEndTime = System.currentTimeMillis();
		// System.out.println("Remove defeater time ended: " + ((new Date(defeaterRemovalEndTime)).toString()));
	}

	public void setStartSuperiorityRemoval() {
		superiorityRemovalStartTime = System.currentTimeMillis();
		superiorityRemovalEndTime = superiorityRemovalStartTime;
		// System.out.println("Remove superiority time start: " + ((new Date(superiorityRemovalStartTime)).toString()));
	}

	public void setEndTimeSuperiorityRemoval() {
		superiorityRemovalEndTime = System.currentTimeMillis();
		// System.out.println("Remove superiority time ended: " + ((new Date(superiorityRemovalEndTime)).toString()));
	}

	public void setMaxMemoryUsed(long maxMemoryUsed) {
		this.maxMemoryUsed = maxMemoryUsed;
	}

	public long getMaxMemoryUsed() {
		return maxMemoryUsed;
	}

	public long getReasoningTimeUsed() {
		return reasoningEndTime - reasoningStartTime;
	}

	public long getNormalFormTransformationTimeUsed() {
		return normalFormTransformationEndTime - normalFormTransformationStartTime;
	}

	public long getDefeaterRemovalTimeUsed() {
		return defeaterRemovalEndTime - defeaterRemovalStartTime;
	}

	public long getSuperiorityRemovalTimeUsed() {
		return superiorityRemovalEndTime - superiorityRemovalStartTime;
	}

	public long getTotalTimeUsed() {
		return getLoadTheoryTimeUsed() + getNormalFormTransformationTimeUsed() + getDefeaterRemovalTimeUsed()
				+ getSuperiorityRemovalTimeUsed() + getReasoningTimeUsed();
	}

	public long getNoOfRules() {
		return noOfRules;
	}

	public long getNoOfLiterals() {
		return noOfLiterals;
	}

	public URL getUrl() {
		return url;
	}

	@Override
	public int compareTo(Object o) {
		if (this == o) return 0;
		if (o instanceof PerformanceStatistic) {
			PerformanceStatistic ps = (PerformanceStatistic) o;
			if (noOfRules != ps.noOfRules) return (int) (noOfRules - ps.noOfRules);
			if (noOfLiterals != ps.noOfLiterals) return (int) (noOfLiterals - ps.noOfLiterals);
			if (url == null) {
				if (ps.url != null) return Integer.MIN_VALUE;
			} else {
				if (!url.equals(ps.url)) return url.toString().compareTo(ps.url.toString());
			}
			if (getTotalTimeUsed() != ps.getTotalTimeUsed()) return (int) (ps.getTotalTimeUsed() - getTotalTimeUsed());
			if (getReasoningTimeUsed() != ps.getReasoningTimeUsed()) return (int) (ps.getReasoningTimeUsed() - getReasoningTimeUsed());
			if (getLoadTheoryTimeUsed() != ps.getLoadTheoryTimeUsed()) return (int) (ps.getLoadTheoryTimeUsed() - getLoadTheoryTimeUsed());
			return 0;
		}
		return toString().compareTo(o.toString());
	}

	@Override
	public boolean equals(Object o) {
		return compareTo(o) == 0;
	}

	public String toString() {
		long loadTheoryElapseTime = getLoadTheoryTimeUsed();
		String loadTheoryElapseTimeStr = Converter.long2TimeString(loadTheoryElapseTime);

		long reasoningElapseTime = getReasoningTimeUsed();
		String reasoningElapseTimeStr = Converter.long2TimeString(reasoningElapseTime);

		long transformationTime = getNormalFormTransformationTimeUsed();
		String transformationTimeUsed = Converter.long2TimeString(transformationTime);

		long defeaterRemovalTime = getDefeaterRemovalTimeUsed();
		String defeaterRemovalTimeUsed = Converter.long2TimeString(defeaterRemovalTime);

		long superiorityRemovalTime = getSuperiorityRemovalTimeUsed();
		String superiorityRemovalTimeUsed = Converter.long2TimeString(superiorityRemovalTime);

		long totalElapseTime = getTotalTimeUsed();
		String totalElapseTimeStr = Converter.long2TimeString(totalElapseTime);

		StringBuilder sb = new StringBuilder();
		sb.append(headString);
		if (null != url) sb.append("\n").append("File name      : ").append(url.toString());
		sb.append("\nno. of rules   : ").append(noOfRules) //
				.append("\nno. of literals: ").append(noOfLiterals) //
				.append("\nTheory loading time                  : ").append(loadTheoryElapseTimeStr) //
				.append("\nRegular form transformation time used: ").append(transformationTimeUsed) //
				.append("\nDefeaters removal time used          : ").append(defeaterRemovalTimeUsed);
		switch (Conf.getReasonerVersion()) {
		case 1:
			sb.append("\nSuperiorities removal time used      : ").append(superiorityRemovalTimeUsed);
			break;
		default:

		}
		sb.append("\nReasoning start at: ").append((new Date(reasoningStartTime))) //
				.append("\nReasoning end at  : ").append((new Date(reasoningEndTime))) //
				.append("\n").append(AppConst.IDENTATOR).append("Time used for reasoning: ") //
				.append(reasoningElapseTimeStr).append(" (").append(reasoningElapseTime).append("ms)") //
				.append("\nTotal time used: ").append(totalElapseTimeStr).append(" (").append(totalElapseTime).append(" ms)") //
				.append("\nMax. memory used: ").append(maxMemoryUsed / 1024 / 1024).append(" MB\n");
		sb.append(headString);
		return sb.toString();
	}

}
