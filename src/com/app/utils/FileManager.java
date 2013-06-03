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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FileManager {
	public static String LINE_SEPARATOR = System.getProperty("line.separator");
	public static String separator = System.getProperty("file.separator");
	public static final int BUFFER_SIZE = 512;

	public static String read(final File filename) throws IOException {
		return new String(readBytes(new FileInputStream(filename)));
	}

	public static String read(final InputStream ins) throws IOException {
		return new String(readBytes(ins));
	}

	public static byte[] readBytes(final InputStream ins) throws IOException {
		if (null == ins) throw new IllegalArgumentException("input stream is null");
		byte[] buffer = new byte[BUFFER_SIZE];
		int len = 0;
		BufferedInputStream in = null;
		ByteArrayOutputStream baos = null;

		in = new BufferedInputStream(ins, BUFFER_SIZE);
		baos = new ByteArrayOutputStream();
		try {
			while ((len = in.read(buffer)) > 0) {
				baos.write(buffer, 0, len);
			}
			return baos.toByteArray();
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			if (null != in) {
				in.close();
				in = null;
			}
			if (null != baos) {
				baos.close();
				baos = null;
			}
		}
	}

	public static Utilities.ProcessStatus write(final File filename, final String content) throws IOException {
		writeBytes(new FileOutputStream(filename), content.getBytes());
		return Utilities.ProcessStatus.SUCCESS;
	}

	public static void writeBytes(final OutputStream outs, final byte[] content) throws IOException {
		int currLoc = 0;
		int contentLen = content.length;
		BufferedOutputStream out = null;
		try {
			out = new BufferedOutputStream(outs, BUFFER_SIZE);
			while (currLoc < contentLen) {
				int len = (contentLen > currLoc + BUFFER_SIZE) ? BUFFER_SIZE : contentLen - currLoc;
				out.write(content, currLoc, len);
				currLoc += len;
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			if (null != out) {
				out.flush();
				out.close();
				out = null;
			}
		}
	}

	public static String getFileExtension(File filename) {
		return getFileExtension(filename.toString());
	}

	public static String getFileExtension(String filename) {
		if (filename == null || "".equals(filename.trim())) return "";

		int sepLoc = filename.lastIndexOf(separator);
		if (sepLoc < 0) sepLoc = 0;
		int dotLoc = filename.indexOf(".", sepLoc);

		return (dotLoc < 0) ? "" : filename.substring(dotLoc + 1).toLowerCase();
	}

	public static File changeFileExtension(final File file, String newExt) throws IOException {
		if (file.isDirectory()) throw new IOException("[" + file + "] is a directory!");
		String filename = file.getName();
		String origExt = getFileExtension(file.getName());
		if (newExt.equals(origExt)) return file;

		if ("".equals(origExt)) {
			return new File(file.getParent(), filename + "." + newExt);
		} else {
			String f = filename.substring(0, filename.length() - origExt.length());
			return new File(file.getParent(), f + newExt);
		}
	}

	public static File addFilenamePostfix(final File file, String postfix) throws IOException {
		if (file.isDirectory()) throw new IOException("[" + file + "] is a directory!");
		String filename = file.getName();
		String origExt = getFileExtension(file.getName());

		if ("".equals(origExt)) {
			return new File(file.getParent(), filename + postfix);
		} else {
			String f = filename.substring(0, filename.length() - origExt.length() - 1);
			String newExt = (origExt.startsWith(".")) ? origExt : "." + origExt;
			return new File(file.getParent(), f + postfix + newExt);
		}
	}

	public static void deleteFile(final File file) throws IOException {
		if (!file.exists()) throw new IOException("file [" + file + "] does not exist");

		File[] files = file.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) deleteFile(files[i]);
				files[i].delete();
			}
		}
		file.delete();
	}

	public static File[] listFiles(File file, Set<String> exts, int noOfLevels) {
		FileSelector fs = null;
		if (!(null == exts || exts.size() == 0)) {
			fs = new FileSelector();
			fs.addExt(exts);
		}
		List<File> files = listFiles(file, fs, noOfLevels);
		return files.toArray(new File[files.size()]);
	}

	public static List<File> listFiles(File file, FileSelector fs, int noOfLevels) {
		List<File> files = new ArrayList<File>();
		if (!file.exists()) return files;

		if (file.isDirectory()) {
			if (noOfLevels == 0) {
				File[] fileSet = null == fs ? file.listFiles() : file.listFiles(fs);
				for (File f : fileSet) {
					if (f.isFile()) files.add(f);
				}
			} else {
				for (File f : file.listFiles()) {
					if (f.isDirectory()) {
						files.addAll(listFiles(f, fs, noOfLevels < 0 ? noOfLevels : noOfLevels - 1));
					} else {
						addFileToList(f, files, fs);
					}
				}
			}
		} else {
			addFileToList(file, files, fs);
		}
		return files;
	}

	private static void addFileToList(File file, List<File> files, FileSelector fs) {
		if (null == fs) {
			files.add(file);
		} else {
			if (fs.accept(file, file.toString())) files.add(file);
		}
	}

}
