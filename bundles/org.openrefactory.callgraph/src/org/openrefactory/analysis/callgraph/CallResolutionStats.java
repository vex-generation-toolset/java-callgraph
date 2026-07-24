/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.analysis.callgraph;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import org.eclipse.jdt.core.dom.ASTNode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openrefactory.cli.ConfigurationManager;
import org.openrefactory.util.ASTNodeUtility;
import org.openrefactory.util.datastructure.IntPair;
import org.openrefactory.util.datastructure.TokenRange;

/**
 * Tallies how each call expression in a target project is resolved during
 * call graph construction. An independent AST inventory supplies the complete
 * phase-4 denominator and initial binding classifications; the processAll*
 * handlers of {@link CallGraphProcessor} may upgrade unresolved sites when
 * they find a source target.
 *
 * Resolution is decided from the JDT binding (binding-truth), not from the
 * receiver's declared-type hash or the over-approximating static-import matcher,
 * both of which misclassify. A call expression falls into one of four buckets:
 *   SOURCE     - binding points to a method/constructor defined in the analyzed project
 *   LIBRARY    - binding points to a confirmed library/JDK/3rd-party target
 *   OTHER      - resolved through another verified mechanism
 *   UNRESOLVED - no source or confirmed library target was found
 *
 * Every call expression is registered once from a direct AST walk and initially
 * classified from its JDT binding. The call-graph handlers may later upgrade an
 * unresolved site when their own source-method matching finds a target. Only
 * unresolved sites need to retain their token ranges; the resolved categories
 * use counters, which keeps memory proportional to the resolution gaps rather
 * than to every call in a large project.
 *
 * For UNRESOLVED call expressions, available call site details (expression text,
 * file, line, column) are captured and dumped as
 * &lt;project&gt;-java-unresolved-call-sites.json for diagnosis.
 */
public class CallResolutionStats {

    public enum Category {
        SOURCE, LIBRARY, OTHER, UNRESOLVED
    }

    // Phase 4 runs multi-threaded, so counters and unresolved-site upgrades must
    // be thread-safe.
    private static final LongAdder source = new LongAdder();
    private static final LongAdder library = new LongAdder();
    private static final LongAdder other = new LongAdder();
    private static final LongAdder unresolved = new LongAdder();

    // TokenRange is the stable identity already used by the call graph. The
    // Boolean is true when a handler may upgrade the unresolved binding and
    // false when a call-resolution compiler error makes the site definitively
    // unresolved.
    private static final ConcurrentMap<TokenRange, Boolean> unresolvedCallSites =
        new ConcurrentHashMap<>();

    // Details of unresolved call sites, capped to guard against
    // memory blowup on very large projects.
    private static final int MAX_UNRESOLVED_DETAILS = 200_000;
    // Cap on a single call expression text, e.g. one carrying a
    // large anonymous class body as an argument.
    private static final int MAX_EXPRESSION_LENGTH = 2000;
    // Each value is {filePath, line, column, callExpression}. Keying details by
    // TokenRange lets us remove an entry when a handler upgrades that site.
    private static final ConcurrentMap<TokenRange, String[]> unresolvedDetails =
        new ConcurrentHashMap<>();
    private static final AtomicInteger unresolvedDetailCount = new AtomicInteger();

    private CallResolutionStats() {
    }

    /**
     * Registers a call expression from the independent AST inventory. A resolved
     * binding is authoritative; a later handler may only upgrade an unresolved
     * category, never downgrade or replace an already resolved binding category.
     *
     * @param category   the resolution category
     * @param node       the call expression AST node
     * @param tokenRange the token range of the call expression
     */
    public static void register(Category category, ASTNode node, TokenRange tokenRange) {
        register(category, node, tokenRange, true);
    }

    /**
     * Registers a call expression and whether a later call-graph handler may
     * upgrade it when the binding was unavailable.
     *
     * @param category            the binding-derived category
     * @param node                the call expression AST node
     * @param tokenRange          the token range of the call expression
     * @param allowHandlerUpgrade false when the compiler rejected call resolution
     */
    public static void register(
        Category category,
        ASTNode node,
        TokenRange tokenRange,
        boolean allowHandlerUpgrade)
    {
        if (tokenRange == null) {
            return;
        }
        Category bindingCategory = category == null ? Category.UNRESOLVED : category;
        if (bindingCategory == Category.UNRESOLVED) {
            Boolean previous = unresolvedCallSites.putIfAbsent(tokenRange, allowHandlerUpgrade);
            if (previous == null) {
                unresolved.increment();
                captureUnresolvedDetail(node, tokenRange);
            } else if (previous.booleanValue() && !allowHandlerUpgrade) {
                unresolvedCallSites.replace(tokenRange, Boolean.TRUE, Boolean.FALSE);
            }
        } else {
            increment(bindingCategory);
        }
    }

    /**
     * Records the result found by a call-graph handler. The AST inventory normally
     * created the entry already; this operation upgrades it only when the inventory
     * could not resolve the binding.
     *
     * @param category   the resolution category found by the handler
     * @param node       the call expression AST node
     * @param tokenRange the token range of the call expression
     */
    public static void record(Category category, ASTNode node, TokenRange tokenRange) {
        if (tokenRange == null || category == null || category == Category.UNRESOLVED) {
            return;
        }
        // Only a site whose binding was unresolved and had no compiler error may
        // be upgraded. remove(key, value) makes repeated handler paths idempotent.
        if (unresolvedCallSites.remove(tokenRange, Boolean.TRUE)) {
            unresolved.decrement();
            increment(category);
            removeUnresolvedDetail(tokenRange);
        }
    }

    private static void increment(Category category) {
        switch (category) {
            case SOURCE:
                source.increment();
                break;
            case LIBRARY:
                library.increment();
                break;
            case OTHER:
                other.increment();
                break;
            case UNRESOLVED:
                unresolved.increment();
                break;
        }
    }

    private static void captureUnresolvedDetail(ASTNode node, TokenRange tokenRange) {
        if (unresolvedDetails.containsKey(tokenRange) || !reserveUnresolvedDetailSlot()) {
            return;
        }
        boolean added = false;
        try {
            // Calculate line:column now, while the file's AST is still loaded.
            IntPair lineColumn = ASTNodeUtility.getLineAndColumn(tokenRange);
            String expression = node == null ? "" : node.toString().replaceAll("\\s+", " ").trim();
            if (expression.length() > MAX_EXPRESSION_LENGTH) {
                expression = expression.substring(0, MAX_EXPRESSION_LENGTH) + "...";
            }
            String[] detail = new String[] {tokenRange.getFileName(),
                Integer.toString(lineColumn.fst), Integer.toString(lineColumn.snd), expression};
            added = unresolvedDetails.putIfAbsent(tokenRange, detail) == null;
        } catch (Exception | Error e) {
            // Losing one diagnostic entry does not affect the category or total.
        } finally {
            if (!added) {
                unresolvedDetailCount.decrementAndGet();
            } else if (!unresolvedCallSites.containsKey(tokenRange)) {
                // The handler may have upgraded the category while the detail was
                // being built.
                removeUnresolvedDetail(tokenRange);
            }
        }
    }

    private static boolean reserveUnresolvedDetailSlot() {
        while (true) {
            int count = unresolvedDetailCount.get();
            if (count >= MAX_UNRESOLVED_DETAILS) {
                return false;
            }
            if (unresolvedDetailCount.compareAndSet(count, count + 1)) {
                return true;
            }
        }
    }

    private static void removeUnresolvedDetail(TokenRange tokenRange) {
        if (unresolvedDetails.remove(tokenRange) != null) {
            unresolvedDetailCount.decrementAndGet();
        }
    }

    // Called at the start of a call graph build so repeated runs start clean.
    public static void reset() {
        source.reset();
        library.reset();
        other.reset();
        unresolved.reset();
        unresolvedCallSites.clear();
        unresolvedDetails.clear();
        unresolvedDetailCount.set(0);
    }

    /**
     * Builds the coverage report block.
     *
     * @return the formatted multi-line report
     */
    public static String report() {
        long src = source.sum();
        long lib = library.sum();
        long oth = other.sum();
        long unres = unresolved.sum();
        long resolved = src + lib + oth;
        long total = resolved + unres;
        long loggedUnres = unresolvedDetails.size();
        long unloggedUnres = unres - loggedUnres;

        StringBuilder sb = new StringBuilder();
        sb.append("======== Call Expression Resolution Coverage ========").append(System.lineSeparator());
        sb.append(line("Total call expressions", total, total, false)).append(System.lineSeparator());
        sb.append(line("Resolved", resolved, total, true)).append(System.lineSeparator());
        sb.append(line("  -> source function", src, total, true)).append(System.lineSeparator());
        sb.append(line("  -> library/3rd-party func", lib, total, true)).append(System.lineSeparator());
        sb.append(line("  -> other callable", oth, total, true)).append(System.lineSeparator());
        sb.append(line("Unresolved", unres, total, true)).append(System.lineSeparator());
        sb.append(line("  -> logged call sites", loggedUnres, total, false)).append(System.lineSeparator());
        sb.append(line("  -> without diagnostic entry", unloggedUnres, total, false)).append(System.lineSeparator());
        sb.append("=====================================================");
        return sb.toString();
    }

    public static int unresolvedDetailCount() {
        return unresolvedDetails.size();
    }

    /**
     * The name of the unresolved call sites dump file for this project.
     *
     * @return &lt;project&gt;-java-unresolved-call-sites.json
     */
    public static String unresolvedCallSitesJsonFileName() {
        return projectName() + "-java-unresolved-call-sites.json";
    }

    /**
     * Builds a JSON report of the unresolved call sites. Each entry has
     * filename, callExpression, line, column and directory, where directory
     * is relative to the project root, i.e., the source file is at
     * &lt;project_root&gt;/&lt;directory&gt;/&lt;filename&gt;.
     *
     * @return the JSON array content
     */
    public static String unresolvedCallSitesJson() {
        List<String[]> entries = new ArrayList<>();
        for (Map.Entry<TokenRange, String[]> entry : unresolvedDetails.entrySet()) {
            if (unresolvedCallSites.containsKey(entry.getKey())) {
                entries.add(entry.getValue());
            }
        }
        entries.sort(Comparator.<String[], String>comparing(e -> e[0])
            .thenComparingInt(e -> Integer.parseInt(e[1]))
            .thenComparingInt(e -> Integer.parseInt(e[2])));
        Path sourceRoot = sourceRoot();
        String projectSegment = uniformFirstSegment();
        JSONArray sites = new JSONArray();
        for (String[] e : entries) {
            Path filePath = Path.of(e[0]);
            String directory = "";
            if (filePath.getParent() != null) {
                directory = filePath.getParent().toString();
                if (sourceRoot != null) {
                    try {
                        directory = sourceRoot.relativize(filePath.getParent()).toString();
                    } catch (Exception ex) {
                        // Keep the absolute directory
                    }
                }
                // The project directory itself is the project root, strip it
                if (projectSegment != null) {
                    if (directory.equals(projectSegment)) {
                        directory = "";
                    } else if (directory.startsWith(projectSegment + "/")) {
                        directory = directory.substring(projectSegment.length() + 1);
                    }
                }
            }
            JSONObject site = new JSONObject();
            site.put("filename", filePath.getFileName().toString());
            site.put("callExpression", e[3]);
            site.put("line", Integer.parseInt(e[1]));
            site.put("column", Integer.parseInt(e[2]));
            site.put("directory", directory);
            sites.put(site);
        }
        return sites.toString(4);
    }

    // The name of the analyzed project. The configured source directory
    // contains the project directory, so when all unresolved sites share
    // one top-level directory under it, that is the project. Falls back
    // to the source directory's own name.
    private static String projectName() {
        String segment = uniformFirstSegment();
        if (segment != null) {
            return segment;
        }
        Path sourceRoot = sourceRoot();
        if (sourceRoot != null && sourceRoot.getFileName() != null) {
            return sourceRoot.getFileName().toString();
        }
        return "project";
    }

    // The common top-level directory of all unresolved sites relative to
    // the configured source directory, or null if there is none.
    private static String uniformFirstSegment() {
        Path sourceRoot = sourceRoot();
        if (sourceRoot == null) {
            return null;
        }
        String candidate = null;
        for (String[] e : unresolvedDetails.values()) {
            try {
                Path rel = sourceRoot.relativize(Path.of(e[0]));
                if (rel.getNameCount() < 2) {
                    return null;
                }
                String first = rel.getName(0).toString();
                if (candidate == null) {
                    candidate = first;
                } else if (!candidate.equals(first)) {
                    return null;
                }
            } catch (Exception ex) {
                return null;
            }
        }
        return candidate;
    }

    private static Path sourceRoot() {
        try {
            return Path.of(ConfigurationManager.config.SOURCE);
        } catch (Exception e) {
            return null;
        }
    }

    // Formats one line with the label padded, the count, and an optional percentage.
    private static String line(String label, long count, long total, boolean showPercent) {
        if (showPercent) {
            double percent = total == 0 ? 0.0 : (count * 100.0) / total;
            return String.format("%-30s: %d (%.1f%%)", label, count, percent);
        }
        return String.format("%-30s: %d", label, count);
    }
}
