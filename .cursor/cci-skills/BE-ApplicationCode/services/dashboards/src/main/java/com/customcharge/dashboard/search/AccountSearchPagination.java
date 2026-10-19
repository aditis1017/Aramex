package com.customcharge.dashboard.search;

import com.customcharge.dashboard.constants.AccountConstants;
import com.customcharge.dashboard.constants.ValidationConstants;
import com.customcharge.persistencelayer.exception.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public final class AccountSearchPagination {

    private AccountSearchPagination() {
    }

    public static <T> Page<T> paginate(List<T> results, Integer page, Integer pageSize) {
        int resolvedPage = resolvePage(page);
        int resolvedPageSize = resolvePageSize(pageSize);
        PageRequest pageable = PageRequest.of(resolvedPage, resolvedPageSize);
        long totalElements = results.size();
        int fromIndex = resolvedPage * resolvedPageSize;
        if (fromIndex >= totalElements) {
            return new PageImpl<>(List.of(), pageable, totalElements);
        }
        int toIndex = Math.min(fromIndex + resolvedPageSize, results.size());
        return new PageImpl<>(results.subList(fromIndex, toIndex), pageable, totalElements);
    }

    public static int resolvePage(Integer page) {
        int resolvedPage = page == null ? AccountConstants.DEFAULT_SEARCH_PAGE : page;
        if (resolvedPage < 0) {
            throw new BadRequestException(ValidationConstants.PAGE_ZERO_OR_GREATER);
        }
        return resolvedPage;
    }

    public static int resolvePageSize(Integer pageSize) {
        int resolvedPageSize = pageSize == null
                ? AccountConstants.DEFAULT_SEARCH_PAGE_SIZE
                : pageSize;
        if (resolvedPageSize < 1) {
            throw new BadRequestException(ValidationConstants.PAGE_SIZE_AT_LEAST_ONE);
        }
        if (resolvedPageSize > AccountConstants.MAX_SEARCH_PAGE_SIZE) {
            throw new BadRequestException(
                    ValidationConstants.PAGE_SIZE_MAX_EXCEEDED_PREFIX
                            + AccountConstants.MAX_SEARCH_PAGE_SIZE
                            + ValidationConstants.PAGE_SIZE_MAX_EXCEEDED_SUFFIX);
        }
        return resolvedPageSize;
    }
}
