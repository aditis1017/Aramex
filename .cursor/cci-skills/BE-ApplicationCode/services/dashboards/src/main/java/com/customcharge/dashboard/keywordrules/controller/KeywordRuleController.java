package com.customcharge.dashboard.keywordrules.controller;

import com.customcharge.dashboard.keywordrules.dto.KeywordRuleResponse;
import com.customcharge.dashboard.keywordrules.service.KeywordRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/keyword-rules")
public class KeywordRuleController {

    private static final Logger log = LoggerFactory.getLogger(KeywordRuleController.class);

    private final KeywordRuleService keywordRuleService;

    public KeywordRuleController(KeywordRuleService keywordRuleService) {
        this.keywordRuleService = keywordRuleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('B2B_KEYWORDS_MANAGER_VIEW')")
    public List<KeywordRuleResponse> getAllKeywordRules() {
        log.debug("GET /api/keyword-rules — listing active keyword rules");
        return keywordRuleService.getAllKeywordRule().stream()
                .map(KeywordRuleResponse::from)
                .toList();
    }

}
