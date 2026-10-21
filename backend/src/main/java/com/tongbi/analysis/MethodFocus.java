package com.tongbi.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MethodFocus {
    private final String className;
    private final String methodName;
    private final String source;
    private final int otherMethodCount;
    private final List<CodeIssue> issues;
    private final String suggestion;

    public MethodFocus(String className, String methodName, String source, int otherMethodCount,
                       List<CodeIssue> issues, String suggestion) {
        this.className = className;
        this.methodName = methodName;
        this.source = source;
        this.otherMethodCount = otherMethodCount;
        this.issues = Collections.unmodifiableList(new ArrayList<CodeIssue>(issues));
        this.suggestion = suggestion;
    }

    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    public String getSource() { return source; }
    public int getOtherMethodCount() { return otherMethodCount; }
    public List<CodeIssue> getIssues() { return issues; }
    public String getSuggestion() { return suggestion; }
}
