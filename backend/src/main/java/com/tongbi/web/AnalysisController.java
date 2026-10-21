package com.tongbi.web;

import com.tongbi.analysis.AnalysisService;
import com.tongbi.web.dto.AnalysisView;
import com.tongbi.web.dto.AnalyzeRequest;
import com.tongbi.web.dto.IssueStatusRequest;
import com.tongbi.web.dto.QuizResultView;
import com.tongbi.web.dto.QuizSubmitRequest;
import com.tongbi.web.dto.ScoreRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService service;

    public AnalysisController(AnalysisService service) {
        this.service = service;
    }

    @PostMapping
    public AnalysisView create(@RequestBody AnalyzeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请先贴一段 Java 代码。");
        }
        return service.create(request.getCode(), request.getFileName());
    }

    @PostMapping("/sample")
    public AnalysisView sample() {
        return service.createSample();
    }

    @GetMapping("/{id}")
    public AnalysisView get(@PathVariable("id") String id) {
        return service.get(id);
    }

    @PostMapping("/{id}/score")
    public AnalysisView score(@PathVariable("id") String id, @RequestBody ScoreRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请先留下要评分的改写。");
        }
        return service.score(id, request.getCode());
    }

    @PostMapping("/{id}/issues/{issueId}")
    public AnalysisView issue(@PathVariable("id") String id,
                              @PathVariable("issueId") String issueId,
                              @RequestBody IssueStatusRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("状态只能是未解决、已改善或已忽略。");
        }
        return service.updateIssue(id, issueId, request.getStatus());
    }

    @PostMapping("/{id}/quiz")
    public QuizResultView quiz(@PathVariable("id") String id, @RequestBody QuizSubmitRequest request) {
        if (request == null || request.getAnswers() == null) {
            return service.submitQuiz(id, Collections.<String, String>emptyMap());
        }
        return service.submitQuiz(id, request.getAnswers());
    }
}
