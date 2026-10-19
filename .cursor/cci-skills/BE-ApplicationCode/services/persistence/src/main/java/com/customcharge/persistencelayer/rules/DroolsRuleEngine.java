package com.customcharge.persistencelayer.rules;


import org.kie.api.KieServices;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import org.kie.api.builder.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;

public class DroolsRuleEngine implements RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(DroolsRuleEngine.class);

    private final KieServices kieServices;

    private final Map<String, KieContainer> containerCache = new ConcurrentHashMap<>();

    public DroolsRuleEngine(KieServices kieServices) {
        this.kieServices = kieServices;
    }

    @Override
    public <T> T execute(T fact, String rules) {
        countRulesFired(fact, rules);
        return fact;
    }

    @Override
    public <T> int countRulesFired(T fact, String rules) {
        if (fact == null) {
            throw new IllegalArgumentException("Fact must not be null");
        }
        if (rules == null || rules.isBlank()) {
            throw new IllegalArgumentException("Rules must not be empty");
        }

        KieContainer kieContainer = containerCache.computeIfAbsent(rules, this::compileRules);

        KieSession kieSession = kieContainer.newKieSession();

        try {
            log.debug("Executing drools rules");

            kieSession.insert(fact);
            return kieSession.fireAllRules();
        } finally {
            kieSession.dispose();
        }
    }

    @Override
    public <T> int executeUntil(
            T fact,
            List<String> drls,
            Function<T, Boolean> stopCondition,
            Function<String, String> drlAdapter) {
        if (fact == null) {
            throw new IllegalArgumentException("Fact must not be null");
        }
        if (drls == null || drls.isEmpty()) {
            return 0;
        }
        Function<String, String> adapter = drlAdapter != null ? drlAdapter : Function.identity();
        Function<T, Boolean> stop = stopCondition != null ? stopCondition : ignored -> false;

        int rulesExecuted = 0;
        for (String drl : drls) {
            if (drl == null || drl.isBlank()) {
                continue;
            }
            try {
                execute(fact, adapter.apply(drl));
            } catch (RuntimeException ex) {
                log.debug("Drools rule execution failed: {}", ex.getMessage());
            }
            rulesExecuted++;
            if (Boolean.TRUE.equals(stop.apply(fact))) {
                break;
            }
        }
        return rulesExecuted;
    }

    private KieContainer compileRules(String rules) {

        log.debug("Compiling drools rules");

        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        kieFileSystem.write(
                "src/main/resources/dynamic-rules.drl",
                kieServices.getResources()
                        .newByteArrayResource(rules.getBytes(StandardCharsets.UTF_8))
                        .setResourceType(ResourceType.DRL));

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem).buildAll();

        Results results = kieBuilder.getResults();

        if (results.hasMessages(Message.Level.ERROR)) {
            log.error("Drools compilation error {}", results);
            throw new RuleExecutionException("Rule compilation failed: " + results);
        }

        ReleaseId releaseId = kieBuilder.getKieModule().getReleaseId();

        return kieServices.newKieContainer(releaseId);
    }
}
