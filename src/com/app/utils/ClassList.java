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
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

public class ClassList {
	private ClassLoader classLoader = null;
	private Set<String> interfaceFilter = null;
	private Set<String> packageFilter = null;
	private Set<String> jarFilter = null;

	public ClassList() {
		this.classLoader = ResourcesUtils.class.getClassLoader();
	}

	public Map<String, Set<String>> findClass(Set<String> interfaceFilter, Set<String> packageFilter, Set<String> jarFilter)
			throws IOException, ClassNotFoundException {
		this.interfaceFilter = (null == interfaceFilter) ? new HashSet<String>() : interfaceFilter;
		this.packageFilter = (null == packageFilter) ? new HashSet<String>() : packageFilter;
		this.jarFilter = (null == jarFilter) ? new HashSet<String>() : jarFilter;

		Map<String, Set<String>> classTable = new HashMap<String, Set<String>>();
		Object[] classPaths = null;
		try {
			classPaths = ((java.net.URLClassLoader) ResourcesUtils.class.getClassLoader()).getURLs();
		} catch (Exception e) {
			classPaths = System.getProperty("java.class.path", "").split(File.pathSeparator);
		}

		for (int i = 0; i < classPaths.length; i++) {
			File classPath = new File((URL.class).isInstance(classPaths[i]) ? ((URL) classPaths[i]).getFile() : classPaths[i].toString());

			JarFile module = null;
			@SuppressWarnings("rawtypes")
			Enumeration files = null;
			if (classPath.isDirectory() && this.jarFilter.size() == 0) {
				Set<String> filesListing = new HashSet<String>();
				listFiles(classPath, filesListing, classPath.toString());
				files = Collections.enumeration(filesListing);
			} else if (classPath.getName().endsWith(".jar")) {
				if (this.jarFilter.size() != 0 && !this.jarFilter.contains(classPath.getName())) continue;
				module = new JarFile(classPath);
				files = module.entries();
			}

			while (files.hasMoreElements()) {
				String filename = files.nextElement().toString();
				if (filename.endsWith(".class")) {
					String className = convertClassName(filename);
					Set<String> interfaceNames = verifyClass(className);

					if (null != interfaceNames && interfaceNames.size() > 0) {
						Set<String> classList = null;
						for (String interfaceName : interfaceNames) {
							classList = classTable.get(interfaceName);
							if (null == classList) {
								classList = new HashSet<String>();
								classTable.put(interfaceName, classList);
							}
							// if (classTable.containsKey(interfaceName)) {
							// classList = classTable.get(interfaceName);
							// } else {
							// classList = new HashSet<String>();
							// classTable.put(interfaceName, classList);
							// }
							if (!classList.contains(className)) classList.add(className);
						}
					}
				}
			}

			if (null != module) module.close();
		}

		return classTable;
	}

	private String convertClassName(String filename) {
		String f = filename.endsWith(".class") ? filename.substring(0, filename.length() - 6) : filename;
		return f.replaceAll("[\\\\/]", ".");
	}

	public Map<String, Set<String>> identifyInterface(Set<String> interfaceNames, Set<String> classNames) throws ClassNotFoundException {
		this.interfaceFilter = (null == interfaceFilter) ? new HashSet<String>() : interfaceFilter;
		this.packageFilter = new HashSet<String>();
		this.jarFilter = new HashSet<String>();
		Map<String, Set<String>> classTable = new HashMap<String, Set<String>>();
		for (String className : classNames) {
			Set<String> interfaces = verifyClass(convertClassName(className));
			for (String interfaceName : interfaceNames) {
				if (interfaces.contains(interfaceName)) {
					Set<String> classList = classTable.get(interfaceName);
					if (null == classList) {
						classList = new HashSet<String>();
						classTable.put(interfaceName, classList);
					}
					if (!classList.contains(className)) classList.add(className);
				}
			}
		}
		return classTable;
	}

	private Set<String> verifyClass(String className) throws ClassNotFoundException {
		try {
			Class<?> clazz = (Class<?>) Class.forName(className, false, classLoader);

			if (clazz.isInterface() || clazz.isEnum()) return null;
			if (clazz.isAnnotation()) return null;
			if (clazz.isSynthetic()) return null;
			if (Modifier.isAbstract(clazz.getModifiers())) return null;
			if (isRejectPackage(clazz.getPackage().getName())) return null;

			Set<String> interfaceList = new HashSet<String>();

			Set<String> s = new HashSet<String>();
			getAllInterface(clazz, s);
			for (String interfaceNameStr : s) {
				if (!isRejectInterface(interfaceNameStr)) interfaceList.add(interfaceNameStr);
			}

			return (interfaceList.size() == 0) ? null : interfaceList;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void getAllInterface(Class<?> clazz, Set<String> interfacesSet) {
		Class<?>[] interfaces = clazz.getInterfaces();
		Class<?> c = clazz;
		while (null != c) {
			interfacesSet.add(c.getName());
			c = c.getSuperclass();
		}
		for (Class<?> inte : interfaces) {
			interfacesSet.add(inte.getName());
		}
		if (!clazz.getSuperclass().getName().equals("java.lang.Object")) {
			getAllInterface(clazz.getSuperclass(), interfacesSet);
		}
	}

	private boolean isRejectInterface(String interfaceName) {
		if (interfaceFilter.size() == 0) return false;
		if (interfaceFilter.contains(interfaceName)) return false;
		return true;
	}

	private boolean isRejectPackage(String className) {
		if (packageFilter.size() == 0) return false;
		int loc = className.lastIndexOf(".");
		String packageName = (loc > 0) ? className.substring(0, loc) : className;
		for (String p : packageFilter) {
			if (p.endsWith(".*") && packageName.startsWith(p.substring(0, p.length() - 2))) {
				return false;
			} else {
				if (packageName.startsWith(p)) return false;
			}
		}
		return true;
	}

	private void listFiles(File file, Set<String> fileListing, String parentPath) {
		if (!file.exists()) return;
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (File f : files) {
				listFiles(f, fileListing, parentPath);
			}
		} else {
			fileListing.add(file.toString().substring(parentPath.length() + 1));
		}
	}

	public static void main(String... args) {
		Set<String> interfaceFilter = new HashSet<String>();
		interfaceFilter.add(spindle.io.TheoryParser.class.getName());
		interfaceFilter.add(spindle.io.TheoryOutputter.class.getName());
		System.out.println("f=" + interfaceFilter);
		Set<String> packageFilter = new HashSet<String>();
		packageFilter.add("spindle.*");
		ClassList classList = new ClassList();
		try {
			classList.findClass(interfaceFilter, packageFilter, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
