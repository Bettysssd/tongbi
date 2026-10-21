package com.tongbi.analysis;

import com.tongbi.web.dto.AnalysisView;

import java.util.ArrayList;
import java.util.List;

public class AnalysisSession {
    private String id;
    private String title;
    private String meta;
    private String originalCode;
    private String suggestion;
    private String rewrite;
    private boolean scored;
    private AnalysisView.ScoreView score;
    private List<CodeIssue> issues = new ArrayList<CodeIssue>();
    private List<QuizItem> quiz = new ArrayList<QuizItem>();
    private String emptyParam = "items";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }
    public String getOriginalCode() { return originalCode; }
    public void setOriginalCode(String originalCode) { this.originalCode = originalCode; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public String getRewrite() { return rewrite; }
    public void setRewrite(String rewrite) { this.rewrite = rewrite; }
    public boolean isScored() { return scored; }
    public void setScored(boolean scored) { this.scored = scored; }
    public AnalysisView.ScoreView getScore() { return score; }
    public void setScore(AnalysisView.ScoreView score) { this.score = score; }
    public List<CodeIssue> getIssues() { return issues; }
    public void setIssues(List<CodeIssue> issues) { this.issues = issues; }
    public List<QuizItem> getQuiz() { return quiz; }
    public void setQuiz(List<QuizItem> quiz) { this.quiz = quiz; }
    public String getEmptyParam() { return emptyParam; }
    public void setEmptyParam(String emptyParam) { this.emptyParam = emptyParam; }
}
