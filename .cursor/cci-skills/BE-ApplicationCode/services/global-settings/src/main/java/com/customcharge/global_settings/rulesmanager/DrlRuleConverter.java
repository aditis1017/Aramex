package com.customcharge.global_settings.rulesmanager;

import com.customcharge.persistencelayer.entities.GlobalSetting;

public interface DrlRuleConverter {

    <T> String convert(GlobalSetting setting, Class<T> targetClass);

}
