package com.customcharge.shipment.controller;

import com.customcharge.persistencelayer.dao.KeywordRuleDao;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/keyword")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class KeywordRuleController {

    KeywordRuleDao keywordRuleDao;

    @GetMapping("/{test}")
    public boolean evaluateB2b(@PathVariable("test") String test) {
        return keywordRuleDao.evaluateB2BUsingFunction(test);
    }
}
