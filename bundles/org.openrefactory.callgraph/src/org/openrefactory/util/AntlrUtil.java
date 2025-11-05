/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import org.openrefactory.analysis.antlr.TypeLexer;
import org.openrefactory.analysis.antlr.TypeParser;
import org.openrefactory.analysis.antlr.TypeParser.TypeContext;
import org.openrefactory.analysis.type.typeinfo.TypeInfo;
import org.openrefactory.analysis.type.TypeCalculator;

/**
 * 
 * Antlr related utility methods
 * 
 * @author Ridwanul Haque
 */
public class AntlrUtil {
    /**
     * 
     * Parse type String in Antlr and return constructed TypeInfo.
     * 
     * @param typeString String type String from json
     * @return TypeInfo constructed TypeInfo
     */
    public static TypeInfo parseAndReturnTypeInfo(String typeString) {
        TypeParser typeParser = getParserOfTypeString(typeString);
        TypeContext typeContext = typeParser.type();

        AntlrTypeVisitor visitor = new AntlrTypeVisitor();
        AntlrTypeInfoWrapper wrapper = visitor.visit(typeContext);
        TypeInfo typeInfo = wrapper.getTypeInfo();
        return TypeCalculator.putOrGetTypeInfoFromMemo(typeInfo);
    }

    /**
     * 
     * Parse type String in Antlr and return the parsed tree as String.
     * 
     * @param typeString String type String from json
     * @return String parse tree from Antlr
     */
    public static String parseAndReturnParseTree(String typeString) {
        TypeParser typeParser = getParserOfTypeString(typeString);
        TypeContext typeContext = typeParser.type();

        AntlrTypeVisitor visitor = new AntlrTypeVisitor();
        visitor.visit(typeContext);
        return typeContext.toStringTree(typeParser);
    }

    /**
     * 
     * Parse type String in Antlr and return Antlr generated Type Parser.
     * 
     * @param typeString String type String from json
     * @return TypeParser constructed parser object
     */
    private static TypeParser getParserOfTypeString(String typeString) {
        CharStream inputStream = CharStreams.fromString(typeString);
        TypeLexer typeLexer = new TypeLexer(inputStream);
        CommonTokenStream commonTokenStream = new CommonTokenStream(typeLexer);
        TypeParser typeParser = new TypeParser(commonTokenStream);
        return typeParser;
    }
}
