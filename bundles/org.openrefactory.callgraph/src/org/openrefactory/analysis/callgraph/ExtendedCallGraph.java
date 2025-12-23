/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.Name;
import org.json.JSONObject;
import org.openrefactory.analysis.callgraph.method.MethodIdentity;
import org.openrefactory.analysis.type.typeinfo.TypeInfo;
import org.openrefactory.capslock.Call;
import org.openrefactory.capslock.Function;
import org.openrefactory.capslock.Module;
import org.openrefactory.capslock.Package;
import org.openrefactory.capslock.Site;
import org.openrefactory.cli.ConfigurationManager;
import org.openrefactory.util.ASTNodeUtility;
import org.openrefactory.util.CallGraphUtility;
import org.openrefactory.util.datastructure.IntPair;
import org.openrefactory.util.datastructure.Pair;
import org.openrefactory.util.datastructure.TokenRange;

/**
 * Extended call graph that includes library functions/methods and uses qualified names.
 *
 * <p>The ExtendedCallGraph is a specialized version of the call graph that provides
 * additional functionality beyond the standard CallGraph class. Unlike the normal call
 * graph which focuses on source code analysis, this extended version includes library
 * functions and methods that may not be present in the analyzed source code.</p>
 * 
 * <p>Key characteristics of the ExtendedCallGraph:</p>
 * <ul>
 *   <li><strong>Library Inclusion:</strong> Incorporates library functions and methods
 *       that are referenced but not defined in the source code</li>
 *   <li><strong>Qualified Names:</strong> Uses fully qualified method names (e.g., 
 *       "com.example.Class.method") instead of method hashes for better readability</li>
 *   <li><strong>JSON Export:</strong> Primarily designed for serializing the call graph
 *       to JSON format for external analysis or visualization</li>
 *   <li><strong>Thread Safety:</strong> Uses ConcurrentHashMap for thread-safe operations
 *       during concurrent call graph construction</li>
 * </ul>
 * 
 * <p>This class is typically used in conjunction with the main CallGraph to provide
 * a comprehensive view of all method call relationships, including those that span
 * across library boundaries.</p>
 *
 * @see CallGraph
 * @see CallGraphDataStructures
 */
public class ExtendedCallGraph {

	private String language;

	/**
	 * Contains all the methods that are present in the call graph 
	 */
	private List<Function> functions;

	/**
	 * Contains all the method calls with call sites 
	 */
	private List<Call> calls;

	/**
	 * Contains all the packages that the functions belong to
	 */
	private List<Package> packages;

	/**
	 * Contains all the modules that the packages belong to
	 */
	private List<Module> modules;

	// These maps are only for lookup. No need to serialize them
	private Map<Function, Integer> functionIndexMap;

	private Map<Package, Integer> packageIndexMap;

	private Map<Module, Integer> moduleIndexMap;

	public ExtendedCallGraph() {
		this.language = "java";
		this.modules = new ArrayList<>();
		this.packages = new ArrayList<>();
		this.functions = new ArrayList<>();
		this.calls = new ArrayList<>();
		this.functionIndexMap = new HashMap<>();
		this.packageIndexMap = new HashMap<>();
		this.moduleIndexMap = new HashMap<>();
	}
	
	public synchronized Set<String> getAllCallers() {
		return calls.stream().map(c -> getFunctionName(c.caller())).collect(toSet());
	}

	public synchronized Map<Site, Set<String>> getCalleesWithCallsites(String caller) {
		return calls.stream().filter(c -> getFunctionName(c.caller()).equals(caller))
				.collect(groupingBy(Call::callSite, mapping(c -> getFunctionName(c.callee()), toSet())));
	}
	
	public synchronized JSONObject toJson() {
		JSONObject graph = new JSONObject();
		return graph.put("language", language)
				.put("functions", functions.stream().map(Function::toJson).collect(toList()))
				.put("calls", calls.stream().map(Call::toJson).collect(toList()))
				.put("packages", packages.stream().map(Package::toJson).collect(toList()))
				.put("modules", modules.stream().map(Module::toJson).collect(toList()));
	}
	
	/**
	 * Dumps the call graph in the given file in capslock format
	 * 
	 * @param fileName the name of the file
	 * @throws IOException
	 */
	public void writeTofile(String fileName) throws IOException {
		File cgFile = Path.of(ConfigurationManager.config.RESULT, fileName).toFile();
		try (FileWriter fOut = new FileWriter(cgFile); BufferedWriter bw = new BufferedWriter(fOut)) {
			bw.write(toJson().toString(4));
		}
	}

	/**
	 * Adds a new edge in the call graph 
	 *  
	 * @param caller the function hash of the caller
	 * @param callee the function hash of the callee
	 * @param tr the token range of the callsite
	 */
	public void addEdge(String caller, String callee, TokenRange tr) {
		if (caller == null || caller.isBlank() || callee == null || callee.isBlank() || tr == null) {
			return;
		}

		int callerIdx = populateInfo(caller);
		int calleeIdx = populateInfo(callee);

		IntPair lineColumn = ASTNodeUtility.getLineAndColumn(tr);
		Path p = Path.of(tr.getFileName());
		// Calculate the relative directory path from the project root
		Path projectRoot = Path.of(ConfigurationManager.config.SOURCE);
		String fileName = p.getFileName().toString();
		String parentDir = projectRoot.relativize(p.getParent()).toString();
		Site site = new Site(parentDir, fileName, (long) lineColumn.fst, (long) lineColumn.snd);
		Call call = new Call((long) callerIdx, (long) calleeIdx, site);
		addCall(call);
	}
	
	/**
	 *
	 * Helper method to populated function, package and module info.
	 *
	 * @param funcHash the hash of the function
	 * @return the index of the function if successful, -1 otherwise.
	 */
	private int populateInfo(String funcHash) {
		String classHash = CallGraphUtility.getClassHashFromMethodHash(funcHash);
		if (classHash == null)
			return -1;

		Pair<Name, Name> pkgAndMod = CallGraphUtility.getPackageAndModuleName(classHash);
		int modIdx = -1;
		if (pkgAndMod.snd != null) {
			modIdx = addModule(new Module(pkgAndMod.snd.getFullyQualifiedName(), "", ""));
		}

		int pkgIdx = -1;
		String pkgName = "";
		if (pkgAndMod.fst != null) {
			pkgName = pkgAndMod.fst.getFullyQualifiedName();
			pkgIdx = addPackage(
					new Package(pkgName.substring(pkgName.lastIndexOf(".") + 1), pkgName, (long) modIdx, false, false));
		}

		int methodHashIndex = CallGraphDataStructures.getMethodIndexFromHash(funcHash);
		if (methodHashIndex == -1) {
			return -1;
		}

		MethodIdentity funcId = CallGraphDataStructures.getHashToMethodInfoBundleList().get(methodHashIndex)
				.getIdentity();
		List<String> paramTypes = funcId.getArgParamTypeInfos().stream().map(TypeInfo::getErasuredSimpleName).collect(toList());

		String funcCanonName = CallGraphUtility.getMethodNameInCanonicalizedFormat(funcHash, true);
		// Parse the canonical name of the function and find the class name
		Pattern classPattern = Pattern
				.compile("(?<class>.+)[.#]((?<special>\\<.+\\>)|" + funcId.getMethodName() + ")\\(");
		Matcher m = classPattern.matcher(funcCanonName);
		String functionName = funcId.getMethodName();
		List<String> properties = new ArrayList<>();
		String className = "";
		if (m.find()) {
			className = m.group("class");
			if (!pkgName.isBlank()) {
				className = className.substring(pkgName.length() + 1);
			}

			String special = m.group("special");
			if (special != null) {
				// Add properties for <init> and <staticinit>
				functionName = special;
				if (special.equals("<init>")) {
					properties.add("default constructor");
				} else if (special.equals("<staticinit>")) {
					properties.add("static initializer");
				}
			}
		}

		return addFunction(new Function(funcCanonName, (long)pkgIdx, className, functionName, paramTypes, properties));
	}

	private synchronized int addFunction(Function func) {
		if (functionIndexMap.containsKey(func)) {
			return functionIndexMap.get(func);
		}

		int idx = functions.size();
		functions.add(func);
		functionIndexMap.put(func, idx);
		return idx;
	}

	private synchronized void addCall(Call call) {
		calls.add(call);
	}

	private synchronized int addPackage(Package pkg) {
		if (packageIndexMap.containsKey(pkg)) {
			return packageIndexMap.get(pkg);
		}

		int idx = packages.size();
		packages.add(pkg);
		packageIndexMap.put(pkg, idx);
		return idx;
	}

	private synchronized int addModule(Module mod) {
		if (moduleIndexMap.containsKey(mod)) {
			return moduleIndexMap.get(mod);
		}

		int idx = modules.size();
		modules.add(mod);
		moduleIndexMap.put(mod, idx);
		return idx;
	}
	
	private synchronized String getFunctionName(long idx) {
		return functions.get((int) idx).name();
	}
}
