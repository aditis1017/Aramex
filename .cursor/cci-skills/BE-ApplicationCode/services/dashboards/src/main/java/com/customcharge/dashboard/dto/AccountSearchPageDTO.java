package com.customcharge.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable search page payload for Redis {@code @Cacheable}. Avoid caching
 * {@link Page} directly — {@code PageImpl}/{@code Pageable} often fail Jackson round-trip.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountSearchPageDTO {

    private List<AccountSearchResponseDTO> content = new ArrayList<>();
    private long totalElements;
    private int page;
    private int pageSize;

    public static AccountSearchPageDTO from(Page<AccountSearchResponseDTO> page) {
        return new AccountSearchPageDTO(
                new ArrayList<>(page.getContent()),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize());
    }

    public Page<AccountSearchResponseDTO> toPage() {
        List<AccountSearchResponseDTO> rows = content == null ? List.of() : content;
        return new PageImpl<>(rows, PageRequest.of(page, pageSize), totalElements);
    }
}
