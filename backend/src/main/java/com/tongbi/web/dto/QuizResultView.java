package com.tongbi.web.dto;

public class QuizResultView {
    private int correct;
    private int total;
    private String scoreText;
    private String judge;
    private String truth;
    private String miss;

    public int getCorrect() { return correct; }
    public void setCorrect(int correct) { this.correct = correct; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public String getScoreText() { return scoreText; }
    public void setScoreText(String scoreText) { this.scoreText = scoreText; }
    public String getJudge() { return judge; }
    public void setJudge(String judge) { this.judge = judge; }
    public String getTruth() { return truth; }
    public void setTruth(String truth) { this.truth = truth; }
    public String getMiss() { return miss; }
    public void setMiss(String miss) { this.miss = miss; }
}
