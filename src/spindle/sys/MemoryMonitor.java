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
 * Class used to monitor the memory usage.
 * 
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory
 * @since version 1.0.0
 */
public class MemoryMonitor {
	private long maxMemoryUsed = 0;
	Runtime runtime = null;
	private long timeUsedInChecking = 0;

	public MemoryMonitor() {
		super();
		reset();
		runtime = Runtime.getRuntime();
	}

	public void reset() {
		maxMemoryUsed = 0;
		timeUsedInChecking = 0;
	}

	public long getMemoryUsed() {
		checkMemoryUsed();
		return maxMemoryUsed;
	}

	public long getTimeUsedInChecking() {
		return timeUsedInChecking;
	}

	public void startMonitor() {
		reset();
		checkMemoryUsed();
	}

	public void checkMemoryUsed() {
		long ts = System.currentTimeMillis();
		long totalMemory = runtime.totalMemory();
		long memoryUsed = totalMemory - runtime.freeMemory();
		if (memoryUsed > maxMemoryUsed) maxMemoryUsed = memoryUsed;
		timeUsedInChecking += (System.currentTimeMillis() - ts);
	}
}
