package com.customcharge.global_settings.rulesmanager;

import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.entities.GlobalRuleCriteria;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.persistencelayer.exception.AbstractApiException;
import com.customcharge.persistencelayer.exception.InternalServerException;
import com.customcharge.persistencelayer.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JavaDrlRuleConverter implements DrlRuleConverter {

    private static final Logger log = LoggerFactory.getLogger(JavaDrlRuleConverter.class);
    private final CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;

    public JavaDrlRuleConverter(CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService) {
        this.categoryCodesGlobalDataCacheService = categoryCodesGlobalDataCacheService;
    }


    @Override
    public <T> String convert(GlobalSetting setting, Class<T> targetClass) {
        log.debug("Entering JavaDrlRuleConverter.convert settingId={} targetClass={}",
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

            List<GlobalRuleCriteria> ruleCriteriaList =
                    DrlRuleResolutionHelper.buildCriteriaForDrl(setting,categoryCodesGlobalDataCacheService,false);

            String conditions = RuleConditionBuilder.build(ruleCriteriaList);
            String className = targetClass.getName();
            String targetValue = categoryCodesGlobalDataCacheService.getCategoryCodeByCode(setting.getTargetValue()).getName();

            return """
                    rule "GlobalRule"
                    no-loop true
                    import %s
                    when
                        $shipment : Shipment(
                            %s
                        )
                    then
                        $shipment.setShipmentType("%s");
                        update($shipment);
                    end
                    """.formatted(
                    className,
                    conditions,
                    targetValue.replace("\"", "\\\"")
            );
        } catch (AbstractApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed in JavaDrlRuleConverter.convert settingId={} targetClass={}",
                    setting != null ? setting.getId() : null,
                    targetClass != null ? targetClass.getName() : null,
                    ex
            );
            throw new InternalServerException(
                    "Unable to generate Drools rules from this global setting. Verify rule definitions and referenced category codes.");
        } finally {
            log.debug("Exiting JavaDrlRuleConverter.convert");
        }
    }

}
