package com.tongbi.analysis;

import com.tongbi.web.dto.AnalysisView;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreCalculatorTest {

    private final ScoreCalculator calculator = new ScoreCalculator();

    @Test
    void originalSampleIs47() {
        AnalysisView.ScoreView score = calculator.score(sampleStyle(), Arrays.asList("empty", "money", "status"));
        assertEquals(22, score.getCorrectness());
        assertEquals(50, score.getReadability());
        assertEquals(88, score.getStyle());
        assertEquals(47, score.getTotal());
        assertEquals("这是你现在的写法：列表没判空，比较方式也还是旧的。", score.getNote());
    }

    @Test
    void cleanForLoopIs85() {
        AnalysisView.ScoreView score = calculator.score(sampleStyle(), Collections.<String>emptyList());
        assertEquals(90, score.getCorrectness());
        assertEquals(74, score.getReadability());
        assertEquals(88, score.getStyle());
        assertEquals(85, score.getTotal());
        assertEquals("这版还保持 for 循环和 ArrayList。", score.getNote());
    }

    @Test
    void streamDropsStyle() {
        AnalysisView.ScoreView score = calculator.score("return items.stream().collect(Collectors.toList());",
                Collections.<String>emptyList());
        assertEquals(42, score.getStyle());
        assertEquals(73, score.getTotal());
        assertEquals("Stream 能用，但已经不像你现在的写法。", score.getNote());
    }

    @Test
    void listScoreUsesIssueShares() {
        assertEquals(100, calculator.listScore(Collections.<String>emptyList()));
        assertEquals(52, calculator.listScore(Arrays.asList("未解决", "未解决", "未解决")));
        assertEquals(68, calculator.listScore(Arrays.asList("已改善", "未解决", "未解决")));
        assertEquals(58, calculator.listScore(Arrays.asList("已忽略", "未解决", "未解决")));
        assertEquals(70, calculator.listScore(Arrays.asList("已忽略", "已忽略", "已忽略")));
        assertEquals(100, calculator.listScore(Arrays.asList("已改善", "已改善", "已改善")));
    }

    private String sampleStyle() {
        return "List<BidItem> result = new ArrayList<BidItem>();\nfor (int i = 0; i < items.size(); i++) {";
    }
}
