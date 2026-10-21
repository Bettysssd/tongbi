package com.tongbi.analysis;

public class CodeIssue {
    private final String id;
    private final String title;
    private final String why;
    private final String better;
    private final String miss;
    private final String before;
    private final String after;
    private final String paramName;
    private String status;

    public CodeIssue(String id, String title, String why, String better, String miss,
                     String before, String after, String paramName) {
        this.id = id;
        this.title = title;
        this.why = why;
        this.better = better;
        this.miss = miss;
        this.before = before;
        this.after = after;
        this.paramName = paramName;
        this.status = "未解决";
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getWhy() { return why; }
    public String getBetter() { return better; }
    public String getMiss() { return miss; }
    public String getBefore() { return before; }
    public String getAfter() { return after; }
    public String getParamName() { return paramName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
