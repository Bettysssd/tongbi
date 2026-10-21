package com.tongbi.analysis;

import com.tongbi.web.dto.AnalysisView;
import com.tongbi.web.dto.QuizResultView;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisServiceTest {

    private final AnalysisService service = new AnalysisService(new JavaAnalyzer(), new ScoreCalculator(), new SessionStore());

    @Test
    void sampleStartsAt47AndSuggestionScores85() {
        AnalysisView created = service.createSample();
        assertEquals("BidItemService.filterValid", created.getTitle());
        assertEquals("只改这一个方法，保持 for 循环和 ArrayList", created.getMeta());
        assertFalse(created.isScored());
        assertEquals(47, created.getScore().getTotal());
        assertEquals(22, created.getScore().getCorrectness());
        assertEquals(50, created.getScore().getReadability());
        assertEquals(88, created.getScore().getStyle());
        assertEquals(52, created.getListScore());
        assertEquals(3, created.getOpenCount());
        assertEquals(JavaAnalyzer.CURATED_REWRITE, created.getSuggestion());
        assertEquals(created.getSuggestion(), created.getRewrite());
        assertEquals(2, created.getQuiz().size());
        assertEquals("items 为 null 时，这段代码会怎样？", created.getQuiz().get(0).getPrompt());

        AnalysisView scored = service.score(created.getId(), created.getSuggestion());
        assertTrue(scored.isScored());
        assertEquals(85, scored.getScore().getTotal());
        assertEquals(90, scored.getScore().getCorrectness());
        assertEquals(74, scored.getScore().getReadability());
        assertEquals(88, scored.getScore().getStyle());
        assertEquals("这版还保持 for 循环和 ArrayList。", scored.getScore().getNote());
        assertEquals(created.getSuggestion(), scored.getSuggestion());
        assertEquals(52, scored.getListScore());
    }

    @Test
    void issueListReaches100AndQuizHidesAnswers() {
        AnalysisView created = service.createSample();
        assertFalse(created.getQuiz().get(0).getOptions().isEmpty());
        AnalysisView improved = service.updateIssue(created.getId(), "empty", "已改善");
        assertEquals(68, improved.getListScore());
        assertEquals(2, improved.getOpenCount());
        service.updateIssue(created.getId(), "status", "已忽略");
        AnalysisView all = service.updateIssue(created.getId(), "money", "已改善");
        service.updateIssue(created.getId(), "status", "已改善");
        all = service.get(created.getId());
        assertEquals(100, all.getListScore());
        assertEquals(0, all.getOpenCount());

        Map<String, String> answers = new LinkedHashMap<String, String>();
        answers.put("q-empty", "throw");
        answers.put("q-status", "mismatch");
        QuizResultView result = service.submitQuiz(created.getId(), answers);
        assertEquals(1, result.getCorrect());
        assertEquals(2, result.getTotal());
        assertEquals("1/2", result.getScoreText());
        assertEquals("有一处和代码表面意思不一样。", result.getJudge());
        assertTrue(result.getTruth().contains("BidStatus.VALID.equals"));
        assertTrue(result.getMiss().startsWith("常见原因"));
    }

    @Test
    void namesTheOtherMethods() {
        String source = "public class BidItemService {\n"
                + "    public int ready() { return 1; }\n"
                + "    public List<BidItem> filterValid(List<BidItem> items) {\n"
                + "        List<BidItem> result = new ArrayList<BidItem>();\n"
                + "        for (int i = 0; i < items.size(); i++) {\n"
                + "            BidItem item = items.get(i);\n"
                + "            if (item.getPrice().doubleValue() > 0 && item.getStatus().equals(\"1\")) {\n"
                + "                result.add(item);\n"
                + "            }\n"
                + "        }\n"
                + "        return result;\n"
                + "    }\n"
                + "}";
        AnalysisView created = service.create(source, null);
        assertEquals("BidItemService.filterValid", created.getTitle());
        assertTrue(created.getMeta().contains("还有 1 个方法"));
        assertTrue(created.getMeta().contains("filterValid"));
    }

    @Test
    void rejectsUnparsedCode() {
        assertThrows(IllegalArgumentException.class, () -> service.create("not java", null));
    }
}
