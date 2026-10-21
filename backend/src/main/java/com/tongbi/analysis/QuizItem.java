package com.tongbi.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizItem {
    private final String id;
    private final String prompt;
    private final List<Option> options;
    private final String answer;

    public QuizItem(String id, String prompt, List<Option> options, String answer) {
        this.id = id;
        this.prompt = prompt;
        this.options = Collections.unmodifiableList(new ArrayList<Option>(options));
        this.answer = answer;
    }

    public String getId() { return id; }
    public String getPrompt() { return prompt; }
    public List<Option> getOptions() { return options; }
    public String getAnswer() { return answer; }

    public static final class Option {
        private final String value;
        private final String label;

        public Option(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() { return value; }
        public String getLabel() { return label; }
    }
}
