/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.capslock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Name;
import org.json.JSONObject;
import org.openrefactory.analysis.callgraph.CallGraphDataStructures;
import org.openrefactory.analysis.callgraph.method.MethodIdentity;
import org.openrefactory.analysis.type.typeinfo.TypeInfo;
import org.openrefactory.util.CallGraphUtility;
import org.openrefactory.util.datastructure.Pair;

/**
 * Issue 32
 * 
 * Contains necessary information of a call graph in the capslock format.
 * 
 * @author Rifat Rubayatul Islam
 */
public class Graph {
    private String language;

    private List<Function> functions;

    private List<Call> calls;

    private List<Package> packages;

    private List<Module> modules;

    // These maps are only for lookup. No need to serialize them
    private Map<Function, Integer> functionIndexMap;

    private Map<Package, Integer> packageIndexMap;

    private Map<Module, Integer> moduleIndexMap;

    public Graph() {
        this.language = "java";
        this.functions = new ArrayList<>();
        this.functionIndexMap = new HashMap<>();
        this.calls = new ArrayList<>();
        this.packages = new ArrayList<>();
        this.packageIndexMap = new HashMap<>();
        this.modules = new ArrayList<>();
        this.moduleIndexMap = new HashMap<>();
    }

    public int addFunction(Function func) {
        if (functionIndexMap.containsKey(func)) { return functionIndexMap.get(func); }

        int idx = functions.size();
        functions.add(func);
        functionIndexMap.put(func, idx);
        return idx;
    }

    public int getFunctionIndex(Function func) {
        return functionIndexMap.getOrDefault(func, -1);
    }

    public void addCall(Call call) {
        calls.add(call);
    }

    public int addPackage(Package pkg) {
        if (packageIndexMap.containsKey(pkg)) { return packageIndexMap.get(pkg); }

        int idx = packages.size();
        packages.add(pkg);
        packageIndexMap.put(pkg, idx);
        return idx;
    }

    public int getPackageIndex(Package pkg) {
        return packageIndexMap.getOrDefault(pkg, -1);
    }

    public int addModule(Module mod) {
        if (moduleIndexMap.containsKey(mod)) { return moduleIndexMap.get(mod); }

        int idx = modules.size();
        modules.add(mod);
        moduleIndexMap.put(mod, idx);
        return idx;
    }

    /**
     *
     * Helper method to populated function, package and module info.
     *
     * @param funcHash the hash of the function
     * @return the index of the function if successful, -1 otherwise.
     */
    public int populateInfo(String funcHash) {
        String callerClassHash = CallGraphUtility.getClassHashFromMethodHash(funcHash);
        if (callerClassHash == null) return -1;

        Pair<Name, Name> pkgAndMod = CallGraphUtility.getPackageAndModuleName(callerClassHash);
        int modIdx = -1;
        if (pkgAndMod.snd != null) {
            modIdx = addModule(new Module(pkgAndMod.snd.getFullyQualifiedName(), "", ""));
        }

        int pkgIdx = -1;
        String pkgName = "";
        if (pkgAndMod.fst != null) {
            pkgName = pkgAndMod.fst.getFullyQualifiedName();
            pkgIdx = addPackage(
                new Package(pkgName.substring(pkgName.lastIndexOf(".") + 1), pkgName, (long)modIdx, false, false));
        }

        int methodHashIndex = CallGraphDataStructures.getMethodIndexFromHash(funcHash);
        if (methodHashIndex == -1) return -1;

        MethodIdentity callerId = CallGraphDataStructures.getHashToMethodInfoBundleList().get(methodHashIndex)
            .getIdentity();
        List<String> paramTypes = callerId.getArgParamTypeInfos().stream().map(TypeInfo::toString)
            .collect(Collectors.toList());

        String funcCanonName = CallGraphUtility.getMethodNameInCanonicalizedFormat(funcHash, true);
        // Parse the canonical name of the function and find the class name
        Pattern classPattern = Pattern
            .compile("(?<class>.+)[.#]((?<special>\\<.+\\>)|" + callerId.getMethodName() + ")\\(");
        Matcher m = classPattern.matcher(funcCanonName);
        String functionName = callerId.getMethodName();
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

    public JSONObject toJson() {
        JSONObject graph = new JSONObject();
        graph.put("language", language);
        graph.put("functions", functions.stream().map(Function::toJson).collect(Collectors.toList()));
        graph.put("calls", calls.stream().map(Call::toJson).collect(Collectors.toList()));
        graph.put("packages", packages.stream().map(Package::toJson).collect(Collectors.toList()));
        graph.put("modules", modules.stream().map(Module::toJson).collect(Collectors.toList()));
        return graph;
    }
}
