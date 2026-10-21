package com.tongbi.analysis;

import com.tongbi.web.dto.AnalysisView;
import com.tongbi.web.dto.QuizResultView;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AnalysisService {

    private static final Pattern FOR_LOOP = Pattern.compile("\\bfor\\s*\\(");

    private final JavaAnalyzer analyzer;
    private final ScoreCalculator calculator;
    private final SessionStore store;

    public AnalysisService(JavaAnalyzer analyzer, ScoreCalculator calculator, SessionStore store) {
        this.analyzer = analyzer;
        this.calculator = calculator;
        this.store = store;
    }

    public AnalysisView createSample() {
        InputStream input = AnalysisService.class.getResourceAsStream("/samples/filter-valid.java");
        if (input == null) {
            throw new IllegalStateException("示例代码不见了。");
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return create(new String(output.toByteArray(), StandardCharsets.UTF_8), "BidItemService.java");
        } catch (IOException ex) {
            throw new IllegalStateException("示例代码读不出来。");
        } finally {
            try {
                input.close();
            } catch (IOException ignored) {
                // The sample text is already buffered.
            }
        }
    }

    public AnalysisView create(String code, String fileName) {
        MethodFocus focus = analyzer.analyze(code);
        AnalysisSession session = new AnalysisSession();
        session.setId(UUID.randomUUID().toString());
        session.setTitle(title(focus, fileName));
        session.setMeta(meta(focus));
        session.setOriginalCode(focus.getSource());
        session.setSuggestion(focus.getSuggestion());
        session.setRewrite(focus.getSuggestion());
        session.setScored(false);
        session.setScore(calculator.score(focus.getSource(), ids(focus.getIssues())));
        session.setIssues(new ArrayList<CodeIssue>(focus.getIssues()));
        session.setQuiz(quiz(focus));
        session.setEmptyParam(emptyParam(focus));
        store.save(session);
        return toView(session);
    }

    public AnalysisView get(String id) {
        return toView(store.get(id));
    }

    public AnalysisView score(String id, String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("请先留下要评分的改写。");
        }
        AnalysisSession session = store.get(id);
        String normalized = code.replace("\r\n", "\n").replace('\r', '\n');
        List<String> issueIds = analyzer.issueIds(normalized);
        session.setRewrite(normalized);
        session.setScored(true);
        session.setScore(calculator.score(normalized, issueIds));
        return toView(session);
    }

    public AnalysisView updateIssue(String id, String issueId, String status) {
        if (!"未解决".equals(status) && !"已改善".equals(status) && !"已忽略".equals(status)) {
            throw new IllegalArgumentException("状态只能是未解决、已改善或已忽略。");
        }
        AnalysisSession session = store.get(id);
        boolean found = false;
        for (CodeIssue issue : session.getIssues()) {
            if (issue.getId().equals(issueId)) {
                issue.setStatus(status);
                found = true;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("没有这条问题。");
        }
        return toView(session);
    }

    public QuizResultView submitQuiz(String id, Map<String, String> answers) {
        AnalysisSession session = store.get(id);
        Map<String, String> given = answers == null ? new LinkedHashMap<String, String>() : answers;
        int correct = 0;
        boolean sawEmpty = false;
        boolean sawStatus = false;
        boolean sawMoney = false;
        for (QuizItem item : session.getQuiz()) {
            if (item.getAnswer().equals(given.get(item.getId()))) {
                correct++;
            }
            if ("q-empty".equals(item.getId())) {
                sawEmpty = true;
            } else if ("q-status".equals(item.getId())) {
                sawStatus = true;
            } else if ("q-money".equals(item.getId())) {
                sawMoney = true;
            }
        }
        int total = session.getQuiz().size();
        QuizResultView result = new QuizResultView();
        result.setCorrect(correct);
        result.setTotal(total);
        result.setScoreText(total == 0 ? "—" : correct + "/" + total);
        result.setJudge(judge(correct, total));
        result.setTruth(truth(session.getEmptyParam(), sawEmpty, sawStatus, sawMoney));
        result.setMiss(miss(sawEmpty, sawStatus, sawMoney));
        return result;
    }

    private AnalysisView toView(AnalysisSession session) {
        AnalysisView view = new AnalysisView();
        view.setId(session.getId());
        view.setTitle(session.getTitle());
        view.setMeta(session.getMeta());
        view.setOriginalCode(session.getOriginalCode());
        view.setSuggestion(session.getSuggestion());
        view.setRewrite(session.getRewrite());
        view.setScored(session.isScored());
        view.setScore(session.getScore());
        List<String> statuses = new ArrayList<String>();
        List<AnalysisView.IssueView> issues = new ArrayList<AnalysisView.IssueView>();
        for (CodeIssue issue : session.getIssues()) {
            statuses.add(issue.getStatus());
            AnalysisView.IssueView item = new AnalysisView.IssueView();
            item.setId(issue.getId());
            item.setTitle(issue.getTitle());
            item.setWhy(issue.getWhy());
            item.setBetter(issue.getBetter());
            item.setMiss(issue.getMiss());
            item.setBefore(issue.getBefore());
            item.setAfter(issue.getAfter());
            item.setStatus(issue.getStatus());
            issues.add(item);
        }
        view.setIssues(issues);
        view.setListScore(calculator.listScore(statuses));
        view.setOpenCount(calculator.openCount(statuses));
        List<AnalysisView.QuizQuestionView> quiz = new ArrayList<AnalysisView.QuizQuestionView>();
        for (QuizItem item : session.getQuiz()) {
            AnalysisView.QuizQuestionView question = new AnalysisView.QuizQuestionView();
            question.setId(item.getId());
            question.setPrompt(item.getPrompt());
            List<AnalysisView.QuizOptionView> options = new ArrayList<AnalysisView.QuizOptionView>();
            for (QuizItem.Option option : item.getOptions()) {
                options.add(new AnalysisView.QuizOptionView(option.getValue(), option.getLabel()));
            }
            question.setOptions(options);
            quiz.add(question);
        }
        view.setQuiz(quiz);
        return view;
    }

    private List<String> ids(List<CodeIssue> issues) {
        List<String> ids = new ArrayList<String>();
        for (CodeIssue issue : issues) {
            ids.add(issue.getId());
        }
        return ids;
    }

    private String title(MethodFocus focus, String fileName) {
        String className = focus.getClassName();
        if (className == null) {
            className = classNameFromFile(fileName);
        }
        if (className == null) {
            return focus.getMethodName();
        }
        return className + "." + focus.getMethodName();
    }

    private String classNameFromFile(String fileName) {
        if (fileName == null) {
            return null;
        }
        String name = fileName;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        if (!name.endsWith(".java") || name.length() <= 5) {
            return null;
        }
        return name.substring(0, name.length() - 5);
    }

    private String meta(MethodFocus focus) {
        String text = "只改这一个方法";
        if (focus.getSource().contains("ArrayList") && FOR_LOOP.matcher(focus.getSource()).find()) {
            text = text + "，保持 for 循环和 ArrayList";
        }
        if (focus.getOtherMethodCount() > 0) {
            text = "这个类里还有 " + focus.getOtherMethodCount() + " 个方法，这次只看问题最多的 "
                    + focus.getMethodName() + "。" + text;
        }
        return text;
    }

    private String emptyParam(MethodFocus focus) {
        for (CodeIssue issue : focus.getIssues()) {
            if ("empty".equals(issue.getId()) && issue.getParamName() != null) {
                return issue.getParamName();
            }
        }
        return "items";
    }

    private List<QuizItem> quiz(MethodFocus focus) {
        boolean empty = false;
        boolean status = false;
        boolean money = false;
        String param = emptyParam(focus);
        String statusExpr = "getStatus().equals(\"1\")";
        for (CodeIssue issue : focus.getIssues()) {
            if ("empty".equals(issue.getId())) {
                empty = true;
            } else if ("status".equals(issue.getId())) {
                status = true;
                statusExpr = issue.getBefore();
            } else if ("money".equals(issue.getId())) {
                money = true;
            }
        }
        List<QuizItem> questions = new ArrayList<QuizItem>();
        if (empty) {
            questions.add(new QuizItem("q-empty", param + " 为 null 时，这段代码会怎样？",
                    options("empty", "返回空列表", "throw", "抛出空指针", "skip", "跳过这一次循环"), "throw"));
        }
        if (status) {
            questions.add(new QuizItem("q-status", "status 为 null 时，" + statusExpr + " 会怎样？",
                    options("mismatch", "判断成不相等", "throw", "抛出空指针", "compile", "编译不通过"), "throw"));
        }
        if (money && questions.size() < 2) {
            questions.add(new QuizItem("q-money", "这里用 doubleValue() 比较金额，实际差在哪里？",
                    options("same", "和 compareTo 完全一样", "precision", "精度可能丢掉", "faster", "只是写起来更快"),
                    "precision"));
        }
        return questions;
    }

    private List<QuizItem.Option> options(String v1, String l1, String v2, String l2, String v3, String l3) {
        List<QuizItem.Option> options = new ArrayList<QuizItem.Option>();
        options.add(new QuizItem.Option(v1, l1));
        options.add(new QuizItem.Option(v2, l2));
        options.add(new QuizItem.Option(v3, l3));
        return options;
    }

    private String judge(int correct, int total) {
        if (total == 0) {
            return "这版没有要核对的读码题。";
        }
        if (correct == total) {
            return total >= 2 ? "两处实际路径都看出来了。" : "这一处实际路径看出来了。";
        }
        if (total >= 2 && correct == 0) {
            return "这两处和代码表面意思不一样。";
        }
        if (total >= 2) {
            return "有一处和代码表面意思不一样。";
        }
        return "这一处和代码表面意思不一样。";
    }

    private String truth(String param, boolean empty, boolean status, boolean money) {
        StringBuilder text = new StringBuilder();
        if (empty && status) {
            text.append(param).append(" 或 status 为空，都会在调用 size() 或 equals() 时抛出空指针。同风格改法是先判空，再用 BidStatus.VALID.equals。");
        } else if (empty) {
            text.append(param).append(" 为空时，会在调用 size() 或进入 for-each 时抛出空指针。同风格改法是先判空再返回。");
        } else if (status) {
            text.append("状态为空时，getter.equals(\"1\") 会抛出空指针，而不是判断成不相等。同风格改法是把常量放在左边。");
        }
        if (money) {
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append("doubleValue() 还会丢掉 BigDecimal 的精度，compareTo 不会。");
        }
        return text.toString();
    }

    private String miss(boolean empty, boolean status, boolean money) {
        if (status && !empty && !money) {
            return "常见原因：状态值是从旧表抄来的，空值没有被当成输入。";
        }
        if (money && !empty && !status) {
            return "常见原因：字段已经是 BigDecimal，判断时顺手转成了 double。";
        }
        return "常见原因：调用方当时传入的都是查库结果，空值没有被当成输入。";
    }
}
