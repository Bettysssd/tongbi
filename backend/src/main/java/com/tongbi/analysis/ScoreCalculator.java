package com.tongbi.analysis;

import com.tongbi.web.dto.AnalysisView;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.regex.Pattern;

@Component
public class ScoreCalculator {

    private static final Pattern FOR_LOOP = Pattern.compile("\\bfor\\s*\\(");

    public AnalysisView.ScoreView score(String code, Collection<String> issueIds) {
        boolean empty = issueIds.contains("empty");
        boolean money = issueIds.contains("money");
        boolean status = issueIds.contains("status");
        boolean forLoop = code != null && FOR_LOOP.matcher(code).find();
        boolean arrayList = code != null && code.contains("ArrayList");
        boolean stream = code != null && code.contains("stream(");

        int correctness = 90;
        int readability = 74;
        int style = 70;
        if (empty) {
            correctness -= 24;
        }
        if (money) {
            correctness -= 22;
        }
        if (status) {
            correctness -= 22;
            readability -= 24;
        }
        if (forLoop) {
            style += 12;
        }
        if (arrayList) {
            style += 6;
        }
        if (stream) {
            style -= 28;
        }
        correctness = clamp(correctness);
        readability = clamp(readability);
        style = clamp(style);

        AnalysisView.ScoreView view = new AnalysisView.ScoreView();
        view.setCorrectness(correctness);
        view.setReadability(readability);
        view.setStyle(style);
        view.setTotal(clamp((int) Math.round(correctness * 0.45 + readability * 0.3 + style * 0.25)));
        view.setNote(note(empty, money, status, forLoop, arrayList, stream));
        return view;
    }

    public int listScore(Collection<String> statuses) {
        if (statuses.isEmpty()) {
            return 100;
        }
        int score = 52;
        int count = statuses.size();
        int base = 48 / count;
        int remainder = 48 % count;
        int index = 0;
        for (String status : statuses) {
            int share = base + (index < remainder ? 1 : 0);
            if ("已改善".equals(status)) {
                score += share;
            } else if ("已忽略".equals(status)) {
                score += share * 6 / 16;
            }
            index++;
        }
        return clamp(score);
    }

    public int openCount(Collection<String> statuses) {
        int open = 0;
        for (String status : statuses) {
            if ("未解决".equals(status)) {
                open++;
            }
        }
        return open;
    }

    private String note(boolean empty, boolean money, boolean status, boolean forLoop, boolean arrayList, boolean stream) {
        if (stream) {
            return "Stream 能用，但已经不像你现在的写法。";
        }
        if (empty && (money || status)) {
            return "这是你现在的写法：列表没判空，比较方式也还是旧的。";
        }
        if (empty) {
            return "列表判空不在了，正确性分会掉。";
        }
        if (money || status) {
            return "还留着原来的比较方式，风险分上不去。";
        }
        if (forLoop && arrayList) {
            return "这版还保持 for 循环和 ArrayList。";
        }
        return "这版的主要问题已经不在了。";
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
