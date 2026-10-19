package com.customcharge.dashboard.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountSearchPageDTOTest {

    @Test
    void fromAndToPage_roundTripPreservesPagination() {
        AccountSearchResponseDTO row = new AccountSearchResponseDTO(
                "8001", "Test Account", "RUH", "user", 5L, "ACTIVE", "2026-01-01", null);
        Page<AccountSearchResponseDTO> page =
                new PageImpl<>(List.of(row), PageRequest.of(0, 25), 42);

        AccountSearchPageDTO cached = AccountSearchPageDTO.from(page);

        assertThat(cached.getContent()).containsExactly(row);
        assertThat(cached.getTotalElements()).isEqualTo(42);
        assertThat(cached.getPage()).isZero();
        assertThat(cached.getPageSize()).isEqualTo(25);

        Page<AccountSearchResponseDTO> restored = cached.toPage();

        assertThat(restored.getContent()).containsExactly(row);
        assertThat(restored.getTotalElements()).isEqualTo(42);
        assertThat(restored.getNumber()).isZero();
        assertThat(restored.getSize()).isEqualTo(25);
    }

    @Test
    void toPage_handlesNullContentAsEmpty() {
        AccountSearchPageDTO cached = new AccountSearchPageDTO(null, 0, 0, 20);

        Page<AccountSearchResponseDTO> restored = cached.toPage();

        assertThat(restored.getContent()).isEmpty();
        assertThat(restored.getTotalElements()).isZero();
    }
}
