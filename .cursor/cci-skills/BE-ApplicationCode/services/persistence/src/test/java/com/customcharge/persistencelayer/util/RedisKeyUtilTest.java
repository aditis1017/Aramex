package com.customcharge.persistencelayer.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisKeyUtilTest {

    @Test
    void key_shouldBuildNamespacedKey() {
        assertThat(RedisKeyUtil.key("ns", "entity", 42L)).isEqualTo("ns:entity:42");
    }

    @Test
    void key_shouldRejectBlankParts() {
        assertThatThrownBy(() -> RedisKeyUtil.key("", "entity", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RedisKeyUtil.key("ns", " ", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RedisKeyUtil.key("ns", "entity", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
