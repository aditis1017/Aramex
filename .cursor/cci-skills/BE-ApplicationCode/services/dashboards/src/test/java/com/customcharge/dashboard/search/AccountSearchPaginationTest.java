package com.customcharge.dashboard.search;

import com.customcharge.persistencelayer.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountSearchPaginationTest {

    @Test
    void paginate_shouldUseDefaultsWhenPageAndPageSizeAreNull() {
        List<Integer> results = IntStream.rangeClosed(1, 150).boxed().toList();

        Page<Integer> page = AccountSearchPagination.paginate(results, null, null);

        assertThat(page.getNumber()).isZero();
        assertThat(page.getSize()).isEqualTo(100);
        assertThat(page.getTotalElements()).isEqualTo(150);
        assertThat(page.getContent()).containsExactlyElementsOf(results.subList(0, 100));
    }

    @Test
    void paginate_shouldReturnRequestedPage() {
        List<Integer> results = IntStream.rangeClosed(1, 10).boxed().toList();

        Page<Integer> page = AccountSearchPagination.paginate(results, 1, 3);

        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(3);
        assertThat(page.getTotalElements()).isEqualTo(10);
        assertThat(page.getContent()).containsExactly(4, 5, 6);
    }

    @Test
    void paginate_shouldReturnEmptyContentWhenPageIsBeyondResults() {
        List<Integer> results = List.of(1, 2, 3);

        Page<Integer> page = AccountSearchPagination.paginate(results, 2, 2);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getNumber()).isEqualTo(2);
    }

    @Test
    void resolvePage_shouldRejectNegativePage() {
        assertThatThrownBy(() -> AccountSearchPagination.resolvePage(-1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("page must be zero or greater");
    }

    @Test
    void resolvePageSize_shouldRejectInvalidValues() {
        assertThatThrownBy(() -> AccountSearchPagination.resolvePageSize(0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("pageSize must be at least 1");

        assertThatThrownBy(() -> AccountSearchPagination.resolvePageSize(501))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("pageSize must not exceed 500");
    }
}
