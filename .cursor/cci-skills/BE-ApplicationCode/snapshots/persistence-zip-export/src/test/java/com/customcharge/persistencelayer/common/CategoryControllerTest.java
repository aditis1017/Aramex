package com.customcharge.persistencelayer.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @Test
    void masterData_shouldDelegateToService() {
        HierarchyRequest request = new HierarchyRequest();
        request.setCategoryName("Country");
        MasterDropdownResponse response = new MasterDropdownResponse("Country", null, new CategoryNode());
        when(categoryService.loadMasterData(List.of(request))).thenReturn(List.of(response));

        List<MasterDropdownResponse> result = categoryController.masterData(List.of(request));

        verify(categoryService).loadMasterData(List.of(request));
        assertThat(result).containsExactly(response);
    }
}
