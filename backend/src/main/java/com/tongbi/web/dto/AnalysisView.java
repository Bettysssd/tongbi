package com.tongbi.web.dto;

import java.util.ArrayList;
import java.util.List;

public class AnalysisView {
    private String id;
    private String title;
    private String meta;
    private String originalCode;
    private String suggestion;
    private String rewrite;
    private boolean scored;
    private ScoreView score;
    private int listScore;
    private int openCount;
    private List<IssueView> issues = new ArrayList<IssueView>();
    private List<QuizQuestionView> quiz = new ArrayList<QuizQuestionView>();

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
    public ScoreView getScore() { return score; }
    public void setScore(ScoreView score) { this.score = score; }
    public int getListScore() { return listScore; }
    public void setListScore(int listScore) { this.listScore = listScore; }
    public int getOpenCount() { return openCount; }
    public void setOpenCount(int openCount) { this.openCount = openCount; }
    public List<IssueView> getIssues() { return issues; }
    public void setIssues(List<IssueView> issues) { this.issues = issues; }
    public List<QuizQuestionView> getQuiz() { return quiz; }
    public void setQuiz(List<QuizQuestionView> quiz) { this.quiz = quiz; }

    public static class ScoreView {
        private int total;
        private int correctness;
        private int readability;
        private int style;
        private String note;

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getCorrectness() { return correctness; }
        public void setCorrectness(int correctness) { this.correctness = correctness; }
        public int getReadability() { return readability; }
        public void setReadability(int readability) { this.readability = readability; }
        public int getStyle() { return style; }
        public void setStyle(int style) { this.style = style; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    public static class IssueView {
        private String id;
        private String title;
        private String why;
        private String better;
        private String miss;
        private String before;
        private String after;
        private String status;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getWhy() { return why; }
        public void setWhy(String why) { this.why = why; }
        public String getBetter() { return better; }
        public void setBetter(String better) { this.better = better; }
        public String getMiss() { return miss; }
        public void setMiss(String miss) { this.miss = miss; }
        public String getBefore() { return before; }
        public void setBefore(String before) { this.before = before; }
        public String getAfter() { return after; }
        public void setAfter(String after) { this.after = after; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class QuizQuestionView {
        private String id;
        private String prompt;
        private List<QuizOptionView> options = new ArrayList<QuizOptionView>();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public List<QuizOptionView> getOptions() { return options; }
        public void setOptions(List<QuizOptionView> options) { this.options = options; }
    }

    public static class QuizOptionView {
        private String value;
        private String label;

        public QuizOptionView() {}

        public QuizOptionView(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
}
