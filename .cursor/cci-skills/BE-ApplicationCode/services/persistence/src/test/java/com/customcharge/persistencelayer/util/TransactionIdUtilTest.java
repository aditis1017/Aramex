package com.customcharge.persistencelayer.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionIdUtilTest {

    @Test
    void generateRandomId_shouldReturnNumericString() {
        assertThat(TransactionIdUtil.generateRandomId()).matches("\\d+");
    }

    @Test
    void formatToDecimalPoint_shouldRoundValue() {
        assertThat(TransactionIdUtil.formatToDecimalPoint(1.2345, 2)).isEqualTo(1.23);
    }

    @Test
    void createPage_shouldSliceList() {
        PageImpl<String> page = TransactionIdUtil.createPage(List.of("a", "b", "c"), 0, 2);
        assertThat(page.getContent()).containsExactly("a", "b");
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void convertObject_shouldMapValues() {
        Map<String, Object> source = Map.of("name", "value");
        Map converted = TransactionIdUtil.convertObject(source, Map.class);
        assertThat(converted.get("name")).isEqualTo("value");
    }

    @Test
    void localDateTimeFormator_shouldParseDate() {
        assertThat(TransactionIdUtil.localDateTimeFormator("2024-06-01", "yyyy-MM-dd").toLocalDate().toString())
                .isEqualTo("2024-06-01");
    }

    @Test
    void createPage_withTotal_shouldUseProvidedTotal() {
        PageImpl<String> page = TransactionIdUtil.createPage(List.of("a"), 0, 10, 100);
        assertThat(page.getTotalElements()).isEqualTo(100);
    }

    @Test
    void convertList_shouldMapElements() {
        List<Map<String, String>> source = List.of(Map.of("name", "a"));
        List<NamedBean> result = TransactionIdUtil.convertList(source, NamedBean.class);
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("a");
    }

    @Test
    void getObjectMapper_shouldReturnSharedMapper() {
        assertThat(TransactionIdUtil.getObjectMapper()).isSameAs(JsonUtils.getObjectMapper());
    }

    @Test
    void getNullPropertyNames_shouldIgnoreNullAndEmptyValues() {
        SampleBean bean = new SampleBean();
        bean.setName("value");
        bean.setDescription("");

        String[] ignored = TransactionIdUtil.getNullPropertyNames(bean);
        assertThat(ignored).contains("uuid", "count", "description");
        assertThat(ignored).doesNotContain("name");
    }

    @Test
    void copy_shouldSkipNullSourceProperties() {
        SampleBean source = new SampleBean();
        source.setName("copied");
        SampleBean target = new SampleBean();
        target.setCount(5);

        TransactionIdUtil.copy(source, target);

        assertThat(target.getName()).isEqualTo("copied");
        assertThat(target.getCount()).isEqualTo(5);
    }

    @Test
    void readValueString_shouldDeserializeJson() {
        NamedBean bean = TransactionIdUtil.readValueString("{\"name\":\"json\"}", NamedBean.class);
        assertThat(bean.getName()).isEqualTo("json");
    }

    @Test
    void readValueString_shouldFallbackToConvertValue() {
        Map<String, String> source = Map.of("name", "object");
        NamedBean bean = TransactionIdUtil.readValueString(source, NamedBean.class);
        assertThat(bean.getName()).isEqualTo("object");
    }

    @Test
    void convert_shouldDelegateToJsonUtils() {
        Map<?, ?> map = TransactionIdUtil.convert("{\"name\":\"converted\"}", Map.class);
        assertThat(map.get("name")).isEqualTo("converted");
    }

    private static class SampleBean {
        private String name;
        private Integer count;
        private String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    private static class NamedBean {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
