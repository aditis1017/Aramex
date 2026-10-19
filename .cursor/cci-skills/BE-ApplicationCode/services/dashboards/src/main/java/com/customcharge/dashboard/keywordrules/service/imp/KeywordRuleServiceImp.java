package com.customcharge.dashboard.keywordrules.service.imp;

import com.customcharge.dashboard.cache.DashboardCacheNames;
import com.customcharge.dashboard.keywordrules.service.KeywordRuleService;
import com.customcharge.persistencelayer.dao.KeywordRuleDao;
import com.customcharge.persistencelayer.entities.KeywordRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class KeywordRuleServiceImp implements KeywordRuleService {

    private static final Logger log = LoggerFactory.getLogger(KeywordRuleServiceImp.class);

    private final KeywordRuleDao keywordRuleDao;

    public KeywordRuleServiceImp(
            KeywordRuleDao keywordRuleDao
    ) {
        this.keywordRuleDao = keywordRuleDao;
    }

    @Override
    @Cacheable(cacheNames = DashboardCacheNames.KEYWORD_RULES)
    public List<KeywordRule> getAllKeywordRule() {
        log.debug("KeywordRuleService.getAllKeywordRule — querying KeywordRuleDao.findActiveRules()");
        return keywordRuleDao.findActiveRules();
    }

}
