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
package com.app.utils;

import java.io.File;

import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Set;

public class FileSelector implements FilenameFilter {
	private Set<String> extLst = null;

	public FileSelector() {
	}

	public FileSelector(final String ext) {
		addExt(ext);
	}

	public void addExt(final Set<String> exts) {
		for (String ext : exts) {
			addExt(ext);
		}
	}

	public void addExt(final String ext) {
		if (ext == null || "".equals(ext.trim())) return;
		String e = "." + ext.trim();
		if (extLst == null) extLst = new HashSet<String>();
		if (extLst.contains(ext)) return;
		extLst.add(e.toLowerCase());
	}

	public void removeExt(final String ext) {
		if (ext == null || "".equals(ext.trim())) return;
		if (extLst == null) return;
		String e = "." + ext.trim().toLowerCase();
		if (extLst.contains(e)) extLst.remove(e);
	}

	@Override
	public boolean accept(File dir, String file) {
		if (extLst == null || extLst.size() == 0) return true;
		String fileExt = FileManager.getFileExtension(file);
		if (!"".equals(fileExt)) fileExt = "." + fileExt.toLowerCase();
		return (extLst.contains(fileExt));
	}

}
