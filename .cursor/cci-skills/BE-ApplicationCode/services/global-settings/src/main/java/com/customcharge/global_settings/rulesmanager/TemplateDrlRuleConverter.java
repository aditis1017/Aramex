package com.customcharge.global_settings.rulesmanager;

import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.entities.GlobalRuleCriteria;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.persistencelayer.exception.AbstractApiException;
import com.customcharge.persistencelayer.exception.InternalServerException;
import com.customcharge.persistencelayer.exception.ValidationException;
import org.drools.template.ObjectDataCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TemplateDrlRuleConverter implements DrlRuleConverter {

    private static final Logger log = LoggerFactory.getLogger(TemplateDrlRuleConverter.class);
    private final CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;

    public TemplateDrlRuleConverter(CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService) {
        this.categoryCodesGlobalDataCacheService = categoryCodesGlobalDataCacheService;
    }

    @Override
    public <T> String convert(GlobalSetting setting, Class<T> targetClass) {
        log.debug("Entering TemplateDrlRuleConverter.convert settingId={} targetClass={}",
                setting != null ? setting.getId() : null,
                targetClass != null ? targetClass.getName() : null
        );
        try {
            if (setting == null) {
                throw new ValidationException("Global setting is required to build Drools rules.");
            }
            if (targetClass == null) {
                throw new ValidationException("Target class is required to build Drools rules.");
            }
            return convertAll(List.of(setting), targetClass);
        } catch (AbstractApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed in TemplateDrlRuleConverter.convert settingId={} targetClass={}",
                    setting != null ? setting.getId() : null,
                    targetClass != null ? targetClass.getName() : null,
                    ex
            );
            throw new InternalServerException(
                    "Unable to generate Drools rules from this global setting. Verify rule definitions and referenced category codes.");
        } finally {
            log.debug("Exiting TemplateDrlRuleConverter.convert");
        }
    }

    public <T> String convertAll(List<GlobalSetting> settings, Class<T> targetClass) {
        log.debug("Entering TemplateDrlRuleConverter.convertAll settingsCount={} targetClass={}",
                settings != null ? settings.size() : null,
                targetClass != null ? targetClass.getName() : null
        );
        try {
            if (settings == null) {
                throw new ValidationException("Global settings list is required to build Drools rules.");
            }
            if (targetClass == null) {
                throw new ValidationException("Target class is required to build Drools rules.");
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            String classPath = targetClass.getName();

            for (GlobalSetting setting : settings) {
                if (setting == null) {
                    continue;
                }

                Map<String, Object> row = new HashMap<>();
                row.put("className", targetClass.getSimpleName());
                row.put("classPath", classPath);
                row.put("targetValue", categoryCodesGlobalDataCacheService.getCategoryCodeByCode(setting.getTargetValue()).getName());
                if("ddu".equalsIgnoreCase(setting.getTargetValue()) || "ddp".equalsIgnoreCase(setting.getTargetValue())
                || "ddo".equalsIgnoreCase(setting.getTargetValue())){
                    row.put("setterMethod","setShipmentType");
                }else{
                    row.put("setterMethod","setCategoryType");
                }
                List<GlobalRuleCriteria> ruleCriteriaList =
                        DrlRuleResolutionHelper.buildCriteriaForDrl(setting,categoryCodesGlobalDataCacheService,false);

                String conditions = RuleConditionBuilder.build(ruleCriteriaList);

                row.put("conditions", conditions);

                rows.add(row);
            }

            InputStream template =
                    getClass().getResourceAsStream("/templates/global-rule-template.drt");
            if (template == null) {
                throw new InternalServerException(
                        "Drools rule template is missing on the server. Contact support if this persists.");
            }

            ObjectDataCompiler compiler = new ObjectDataCompiler();
            return compiler.compile(rows, template);
        } catch (AbstractApiException ex) {
            log.error("Failed in TemplateDrlRuleConverter.convertAll settingsCount={} targetClass={}",
                    settings != null ? settings.size() : null,
                    targetClass != null ? targetClass.getName() : null,
                    ex
            );
            throw ex;
        } catch (Exception ex) {
            log.error("Failed in TemplateDrlRuleConverter.convertAll settingsCount={} targetClass={}",
                    settings != null ? settings.size() : null,
                    targetClass != null ? targetClass.getName() : null,
                    ex
            );
            throw new InternalServerException(
                    "Unable to generate Drools rules from the given global settings. Verify rule definitions and referenced category codes.");
        } finally {
            log.debug("Exiting TemplateDrlRuleConverter.convertAll");
        }
    }
}
