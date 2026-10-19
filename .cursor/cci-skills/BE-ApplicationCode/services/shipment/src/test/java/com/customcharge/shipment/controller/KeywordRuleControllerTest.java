package com.customcharge.shipment.controller;

import com.customcharge.persistencelayer.dao.KeywordRuleDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeywordRuleControllerTest {

    @Mock
    private KeywordRuleDao keywordRuleDao;

    @InjectMocks
    private KeywordRuleController controller;

    @Test
    void evaluateB2b_shouldDelegateToDao() {
        when(keywordRuleDao.evaluateB2BUsingFunction("ACME")).thenReturn(true);

        assertThat(controller.evaluateB2b("ACME")).isTrue();
    }
}
