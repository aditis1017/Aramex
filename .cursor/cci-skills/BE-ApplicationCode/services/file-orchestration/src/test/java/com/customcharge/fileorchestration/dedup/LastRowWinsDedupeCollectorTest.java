package com.customcharge.fileorchestration.dedup;

import com.customcharge.fileorchestration.support.FileOrchestrationTestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LastRowWinsDedupeCollectorTest {

    @Test
    void accept_shouldKeepLastRow_whenDuplicateAwbInEstimateTemplate() {
        LastRowWinsDedupeCollector collector = new LastRowWinsDedupeCollector("Estimates_Template", true);

        collector.accept(FileOrchestrationTestDataFactory.row("AWB-1", "first"));
        collector.accept(FileOrchestrationTestDataFactory.row("AWB-1", "second"));

        List<Map<String, String>> rows = collector.toOrderedRows();

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().get("saddad")).isEqualTo("second");
        assertThat(collector.getRawRowCount()).isEqualTo(2);
        assertThat(collector.getDedupedRowCount()).isEqualTo(1);
        assertThat(collector.getDroppedCount()).isEqualTo(1);
        assertThat(collector.getDuplicateRows().getFirst().get("errorMessage"))
                .isEqualTo(FileOrchestrationDedupConstants.DUPLICATE_AWB_ERROR_MESSAGE);
    }

    @Test
    void accept_shouldKeepLastRow_whenDuplicateAwbAndSaddadInActualTemplate() {
        LastRowWinsDedupeCollector collector = new LastRowWinsDedupeCollector("Actual_Template", true);

        collector.accept(FileOrchestrationTestDataFactory.row("AWB-1", "SAD-1"));
        collector.accept(FileOrchestrationTestDataFactory.row("AWB-1", "SAD-1"));

        List<Map<String, String>> rows = collector.toOrderedRows();

        assertThat(rows).hasSize(1);
        assertThat(collector.getDroppedCount()).isEqualTo(1);
        assertThat(collector.getDuplicateRows()).hasSize(1);
        assertThat(collector.getDuplicateRows().getFirst().get("errorMessage"))
                .isEqualTo(FileOrchestrationDedupConstants.DUPLICATE_AWB_SADDAD_ERROR_MESSAGE);
    }

    @Test
    void accept_shouldKeepLastRow_whenDuplicateDeclarationNumberInActualTemplate() {
        LastRowWinsDedupeCollector collector = new LastRowWinsDedupeCollector("Actual_Template", true);

        Map<String, String> first = FileOrchestrationTestDataFactory.row("AWB-1", "SAD-1");
        first.put("declaration number", "BOE-1");
        Map<String, String> second = FileOrchestrationTestDataFactory.row("AWB-2", "SAD-2");
        second.put("declaration number", "BOE-1");

        collector.accept(first);
        collector.accept(second);

        List<Map<String, String>> rows = collector.toOrderedRows();

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().get("awb")).isEqualTo("AWB-2");
        assertThat(rows.getFirst().get("saddad")).isEqualTo("SAD-2");
        assertThat(collector.getDroppedCount()).isEqualTo(1);
        assertThat(collector.getDuplicateRows()).hasSize(1);
        assertThat(collector.getDuplicateRows().getFirst().get("errorMessage"))
                .isEqualTo(FileOrchestrationDedupConstants.DUPLICATE_DECLARATION_NUMBER_ERROR_MESSAGE);
    }

    @Test
    void accept_shouldNotDedupByBlankDeclarationNumber_inActualTemplate() {
        LastRowWinsDedupeCollector collector = new LastRowWinsDedupeCollector("Actual_Template", true);

        Map<String, String> first = FileOrchestrationTestDataFactory.row("AWB-1", "SAD-1");
        first.put("declaration number", " ");
        Map<String, String> second = FileOrchestrationTestDataFactory.row("AWB-2", "SAD-2");
        second.put("declaration number", "");

        collector.accept(first);
        collector.accept(second);

        assertThat(collector.toOrderedRows()).hasSize(2);
        assertThat(collector.getDroppedCount()).isZero();
    }

    @Test
    void accept_shouldPassThrough_whenDedupDisabled() {
        LastRowWinsDedupeCollector collector = new LastRowWinsDedupeCollector("Estimates_Template", false);

        collector.accept(FileOrchestrationTestDataFactory.row("AWB-1", "first"));
        collector.accept(FileOrchestrationTestDataFactory.row("AWB-1", "second"));

        assertThat(collector.toOrderedRows()).hasSize(2);
        assertThat(collector.getDedupedRowCount()).isEqualTo(2);
        assertThat(collector.getDroppedCount()).isZero();
    }

    @Test
    void accept_shouldPassThrough_whenProcessForUnsupported() {
        LastRowWinsDedupeCollector collector = new LastRowWinsDedupeCollector("Other", true);

        collector.accept(FileOrchestrationTestDataFactory.row("AWB-1", "first"));
        collector.accept(FileOrchestrationTestDataFactory.row("AWB-1", "second"));

        assertThat(collector.toOrderedRows()).hasSize(2);
    }

    @Test
    void accept_shouldIncrementRawCount_whenRowNull() {
        LastRowWinsDedupeCollector collector = new LastRowWinsDedupeCollector("Estimates_Template", true);

        collector.accept(null);

        assertThat(collector.getRawRowCount()).isEqualTo(1);
        assertThat(collector.toOrderedRows()).isEmpty();
    }

    @Test
    void accept_shouldReplaceNullFieldValuesWithEmptyString() {
        LastRowWinsDedupeCollector collector = new LastRowWinsDedupeCollector("Other", true);
        Map<String, String> row = new HashMap<>();
        row.put("awb", "AWB-1");
        row.put("saddad", null);

        collector.accept(row);

        assertThat(collector.toOrderedRows().getFirst().get("saddad")).isEmpty();
    }

    @Test
    void accept_shouldCopyRows_soMutationsDoNotAffectStoredState() {
        LastRowWinsDedupeCollector collector = new LastRowWinsDedupeCollector("Other", true);
        Map<String, String> row = FileOrchestrationTestDataFactory.row("AWB-1", "S1");

        collector.accept(row);
        row.put("saddad", "changed");

        assertThat(collector.toOrderedRows().getFirst().get("saddad")).isEqualTo("S1");
    }
}
