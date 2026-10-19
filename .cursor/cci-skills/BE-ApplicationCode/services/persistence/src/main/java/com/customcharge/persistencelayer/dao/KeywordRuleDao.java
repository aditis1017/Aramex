package com.customcharge.persistencelayer.dao;

import com.customcharge.persistencelayer.entities.KeywordRule;
import com.customcharge.persistencelayer.repository.KeywordRuleRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KeywordRuleDao {

    private final KeywordRuleRepository keywordRuleRepository;

    public KeywordRuleDao(KeywordRuleRepository keywordRuleRepository) {
        this.keywordRuleRepository = keywordRuleRepository;
    }

    public List<KeywordRule> saveAll(List<KeywordRule> rules) {
        return keywordRuleRepository.saveAll(rules);
    }

    public int deactivateAllActiveRules() {
        return keywordRuleRepository.deactivateAllActiveRules();
    }

    public List<KeywordRule> findActiveRules() {
        return keywordRuleRepository.findByIsActiveTrueOrderByKeywordAsc();
    }

    public boolean evaluateB2BUsingFunction(String input) {
        Integer response = keywordRuleRepository.evaluateB2B(input);
        return response != null && response == 1;
    }
}
