package com.customcharge.fileorchestration.dedup;

import com.customcharge.fileorchestration.support.FileOrchestrationTestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FileRowDedupKeyResolverTest {

    @Test
    void resolveKey_shouldReturnNull_whenProcessForOrRowMissing() {
        assertThat(FileRowDedupKeyResolver.resolveKey(null, FileOrchestrationTestDataFactory.row("A1", "S1")))
                .isNull();
        assertThat(FileRowDedupKeyResolver.resolveKey("Estimates_Template", null)).isNull();
    }

    @Test
    void resolveKey_shouldUseAwbOnly_forEstimateTemplate() {
        Map<String, String> row = FileOrchestrationTestDataFactory.row(" AWB-1 ", "ignored");

        assertThat(FileRowDedupKeyResolver.resolveKey("Estimates_Template", row))
                .isEqualTo("awb-1");
    }

    @Test
    void resolveKey_shouldUseAwbAndSaddad_forActualTemplate() {
        Map<String, String> row = FileOrchestrationTestDataFactory.row("AWB-1", " SAD-9 ");

        assertThat(FileRowDedupKeyResolver.resolveKey("Actual_Template", row))
                .isEqualTo("awb-1|sad-9");
    }

    @Test
    void resolveKey_shouldUseMissingAwbPlaceholder_whenAwbBlank() {
        Map<String, String> row = FileOrchestrationTestDataFactory.row(" ", "SAD-9");

        assertThat(FileRowDedupKeyResolver.resolveKey("Actual_Template", row))
                .isEqualTo("__missing_awb__|sad-9");
    }

    @Test
    void resolveKey_shouldUseEmptySaddad_whenSaddadBlank() {
        Map<String, String> row = FileOrchestrationTestDataFactory.row("AWB-1", "  ");

        assertThat(FileRowDedupKeyResolver.resolveKey("Actual_Template", row))
                .isEqualTo("awb-1|");
    }

    @Test
    void resolveKey_shouldReturnNull_forUnsupportedProcessFor() {
        assertThat(FileRowDedupKeyResolver.resolveKey("Other_Template", FileOrchestrationTestDataFactory.row("A", "B")))
                .isNull();
    }

    @Test
    void resolveKey_shouldMatchColumnsCaseInsensitively() {
        Map<String, String> row = Map.of("AWB", "X1", "SaDdAd", "Y1");

        assertThat(FileRowDedupKeyResolver.resolveKey("Actual_Template", row))
                .isEqualTo("x1|y1");
    }

    @Test
    void resolveDeclarationNumberKey_shouldReturnNormalizedValue_forActualTemplate() {
        Map<String, String> row = FileOrchestrationTestDataFactory.row("AWB-1", "SAD-1");
        row.put("declaration number", " DECL-9 ");

        assertThat(FileRowDedupKeyResolver.resolveDeclarationNumberKey("Actual_Template", row))
                .isEqualTo("decl-9");
    }

    @Test
    void resolveDeclarationNumberKey_shouldReturnNull_whenDeclarationBlankOrNotActual() {
        Map<String, String> blankDeclaration = FileOrchestrationTestDataFactory.row("AWB-1", "SAD-1");
        blankDeclaration.put("declaration number", "  ");

        assertThat(FileRowDedupKeyResolver.resolveDeclarationNumberKey("Actual_Template", blankDeclaration))
                .isNull();
        assertThat(FileRowDedupKeyResolver.resolveDeclarationNumberKey(
                        "Estimates_Template",
                        FileOrchestrationTestDataFactory.row("AWB-1", "SAD-1")))
                .isNull();
    }

    @Test
    void resolveDeclarationNumberKey_shouldMatchColumnsCaseInsensitively() {
        Map<String, String> row = Map.of("AWB", "X1", "saddad", "Y1", "Declaration Number", "BOE-1");

        assertThat(FileRowDedupKeyResolver.resolveDeclarationNumberKey("Actual_Template", row))
                .isEqualTo("boe-1");
    }

    @Test
    void dedupApplies_shouldReturnTrue_forSupportedTemplates() {
        assertThat(FileRowDedupKeyResolver.dedupApplies(" Estimates_Template ")).isTrue();
        assertThat(FileRowDedupKeyResolver.dedupApplies("actual_template")).isTrue();
    }

    @Test
    void dedupApplies_shouldReturnFalse_whenUnsupportedOrNull() {
        assertThat(FileRowDedupKeyResolver.dedupApplies(null)).isFalse();
        assertThat(FileRowDedupKeyResolver.dedupApplies("Other")).isFalse();
    }
}
