package com.tongbi.analysis;

import com.github.javaparser.ast.CompilationUnit;

class ParsedSource {
    private final String original;
    private final CompilationUnit unit;
    private final int lineOffset;

    ParsedSource(String original, CompilationUnit unit, int lineOffset) {
        this.original = original;
        this.unit = unit;
        this.lineOffset = lineOffset;
    }

    String getOriginal() { return original; }
    CompilationUnit getUnit() { return unit; }
    int getLineOffset() { return lineOffset; }
}
