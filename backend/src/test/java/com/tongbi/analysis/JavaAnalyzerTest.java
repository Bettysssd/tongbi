package com.tongbi.analysis;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaAnalyzerTest {

    private final JavaAnalyzer analyzer = new JavaAnalyzer();

    @Test
    void sampleFocusesCuratedRewrite() {
        MethodFocus focus = analyzer.analyze(JavaAnalyzer.SAMPLE_ORIGINAL);
        assertEquals("BidItemService", focus.getClassName());
        assertEquals("filterValid", focus.getMethodName());
        assertEquals(0, focus.getOtherMethodCount());
        assertEquals(Arrays.asList("empty", "status", "money"), ids(focus));
        assertEquals(JavaAnalyzer.CURATED_REWRITE, focus.getSuggestion());
        assertEquals("for (int i = 0; i < items.size(); i++) {", focus.getIssues().get(0).getBefore());
        assertEquals("item.getStatus().equals(\"1\")", focus.getIssues().get(1).getBefore());
        assertEquals("item.getPrice().doubleValue() > 0", focus.getIssues().get(2).getBefore());
        assertFalse(focus.getSuggestion().contains("stream("));
        assertTrue(focus.getSuggestion().contains("for ("));
        assertTrue(focus.getSuggestion().contains("ArrayList"));
    }

    @Test
    void curatedRewriteClearsTheThreeIssues() {
        assertEquals(CollectionsEmpty.NONE, analyzer.issueIds(JavaAnalyzer.CURATED_REWRITE));
    }

    @Test
    void genericRewriteKeepsTheForLoop() {
        String source = "public List<Order> keep(List<Order> orders) {\n"
                + "    List<Order> result = new ArrayList<Order>();\n"
                + "    for (int i = 0; i < orders.size(); i++) {\n"
                + "        Order order = orders.get(i);\n"
                + "        if (order.getAmount().doubleValue() > 0 && order.getFlag().equals(\"0\")) {\n"
                + "            result.add(order);\n"
                + "        }\n"
                + "    }\n"
                + "    return result;\n"
                + "}";
        MethodFocus focus = analyzer.analyze(source);
        assertEquals(Arrays.asList("empty", "status", "money"), ids(focus));
        assertTrue(focus.getSuggestion().contains("orders == null"));
        assertTrue(focus.getSuggestion().contains("for (int i = 0; i < orders.size(); i++)"));
        assertTrue(focus.getSuggestion().contains("new ArrayList"));
        assertTrue(focus.getSuggestion().contains("compareTo(BigDecimal.ZERO) > 0"));
        assertTrue(focus.getSuggestion().contains("BidStatus.INVALID.equals(order.getFlag())"));
        assertFalse(focus.getSuggestion().contains("doubleValue"));
        assertEquals(CollectionsEmpty.NONE, analyzer.issueIds(focus.getSuggestion()));
    }

    @Test
    void nullCheckAndConstantOnTheLeftAreNotIssues() {
        String source = "public List<BidItem> filterValid(List<BidItem> items) {\n"
                + "    if (items == null || items.isEmpty()) {\n"
                + "        return new ArrayList<BidItem>();\n"
                + "    }\n"
                + "    for (BidItem item : items) {\n"
                + "        if (\"1\".equals(item.getStatus())) {\n"
                + "            return items;\n"
                + "        }\n"
                + "    }\n"
                + "    return items;\n"
                + "}";
        assertEquals(CollectionsEmpty.NONE, analyzer.issueIds(source));
    }

    @Test
    void focusesTheMethodWithMoreIssues() {
        String source = "public class BidItemService {\n"
                + "    public void ready() {\n"
                + "        int n = 1;\n"
                + "    }\n"
                + "    public List<BidItem> filterValid(List<BidItem> items) {\n"
                + "        for (int i = 0; i < items.size(); i++) {\n"
                + "            BidItem item = items.get(i);\n"
                + "            if (item.getPrice().doubleValue() > 0 && item.getStatus().equals(\"1\")) {\n"
                + "                items.get(i);\n"
                + "            }\n"
                + "        }\n"
                + "        return items;\n"
                + "    }\n"
                + "}";
        MethodFocus focus = analyzer.analyze(source);
        assertEquals("filterValid", focus.getMethodName());
        assertEquals("BidItemService", focus.getClassName());
        assertEquals(1, focus.getOtherMethodCount());
        assertEquals(3, focus.getIssues().size());
    }

    @Test
    void defaultInterfaceMethodIsAnalyzed() {
        String source = "default FlyQiuFieldFilter andThen(FlyQiuFieldFilter after) {\n"
                + "    Objects.requireNonNull(after);\n"
                + "    return (Field field, Class<?> clazz) -> {\n"
                + "        boolean currentResult = this.filter(field, clazz);\n"
                + "        if (!currentResult) {\n"
                + "            return false; // 如果当前过滤器返回 false，则不再执行后续过滤器\n"
                + "        }\n"
                + "        return after.filter(field, clazz); // 执行后续过滤器\n"
                + "    };\n"
                + "}";
        MethodFocus focus = analyzer.analyze(source);
        assertEquals("andThen", focus.getMethodName());
        assertEquals(null, focus.getClassName());
        assertEquals(CollectionsEmpty.NONE, ids(focus));
        assertTrue(focus.getSource().startsWith("default FlyQiuFieldFilter andThen"));
        assertTrue(focus.getSource().contains("requireNonNull(after)"));
        assertTrue(focus.getSuggestion().contains("Class<?>"));
    }

    private java.util.List<String> ids(MethodFocus focus) {
        java.util.List<String> ids = new java.util.ArrayList<String>();
        for (CodeIssue issue : focus.getIssues()) {
            ids.add(issue.getId());
        }
        return ids;
    }

    private static final class CollectionsEmpty {
        private static final java.util.List<String> NONE = java.util.Collections.emptyList();
    }
}
