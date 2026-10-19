package com.customcharge.persistencelayer.common;

import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.cache.MasterCategoryTreeLoader;
import com.customcharge.persistencelayer.cache.MasterDataCacheKeys;
import com.customcharge.persistencelayer.entities.Category;
import com.customcharge.persistencelayer.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ObjectProvider<CacheService> cacheServiceProvider;
    @Mock
    private ObjectProvider<CategoryCodesGlobalDataCacheService> categoryCacheServiceProvider;
    @Mock
    private MasterCategoryTreeLoader treeLoader;
    @Mock
    private CacheService cacheService;
    @Mock
    private CategoryCodesGlobalDataCacheService categoryCacheService;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(
                categoryRepository,
                cacheServiceProvider,
                categoryCacheServiceProvider,
                treeLoader);
    }

    @Test
    void loadMasterData_shouldWarmCacheAndReturnTree_whenCacheMisses() {
        CategoryNode dbTree = treeWithChild("ROOT", "CH1");
        MasterCategoryTreeLoader.MasterCategoryIndex index =
                new MasterCategoryTreeLoader.MasterCategoryIndex(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(categoryCacheServiceProvider.getIfAvailable()).thenReturn(categoryCacheService);
        when(categoryRepository.findByNameIgnoreCase("MyCategory"))
                .thenReturn(Optional.of(category("MyCategory", "CAT1")));
        when(cacheService.get(MasterDataCacheKeys.categoryTree("CAT1"), CategoryNode.class))
                .thenReturn(Optional.empty());
        when(treeLoader.loadIndex()).thenReturn(index);
        when(treeLoader.buildTree("CAT1", index)).thenReturn(Optional.of(dbTree));

        List<MasterDropdownResponse> responses = categoryService.loadMasterData(List.of(request("MyCategory")));

        verify(categoryCacheService).ensureCategoryTreeCached("CAT1");
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().tree().getChildren()).hasSize(1);
    }

    @Test
    void loadMasterData_shouldFail_whenRequestNull() {
        assertThatThrownBy(() -> categoryService.loadMasterData(null))
                .isInstanceOf(com.customcharge.persistencelayer.exception.BadRequestException.class)
                .hasMessage("Request body is required");
    }

    @Test
    void loadMasterData_shouldResolveSubtree_whenPathProvided() {
        CategoryNode root = treeWithChild("ROOT", "CHILD");
        CategoryNode child = root.getChildren().getFirst();
        MasterCategoryTreeLoader.MasterCategoryIndex index =
                new MasterCategoryTreeLoader.MasterCategoryIndex(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(categoryCacheServiceProvider.getIfAvailable()).thenReturn(null);
        when(categoryRepository.findByNameIgnoreCase("MyCategory"))
                .thenReturn(Optional.of(category("MyCategory", "CAT1")));
        when(cacheService.get(
                MasterDataCacheKeys.categoryTreePath("CAT1", List.of("CHILD")),
                CategoryNode.class)).thenReturn(Optional.empty());
        when(treeLoader.loadIndex()).thenReturn(index);
        when(treeLoader.buildTree("CAT1", index)).thenReturn(Optional.of(root));

        HierarchyRequest request = request("MyCategory");
        request.setPath(List.of("CHILD"));

        List<MasterDropdownResponse> responses = categoryService.loadMasterData(List.of(request));

        assertThat(responses.getFirst().tree()).isSameAs(child);
    }

    @Test
    void loadMasterData_shouldFail_whenCategoryNameMissing() {
        HierarchyRequest request = new HierarchyRequest();
        request.setCategoryName("  ");

        assertThatThrownBy(() -> categoryService.loadMasterData(List.of(request)))
                .isInstanceOf(com.customcharge.persistencelayer.exception.BadRequestException.class)
                .hasMessage("categoryName is required for each item");
    }

    @Test
    void loadMasterData_shouldFail_whenCategoryNotFound() {
        when(categoryRepository.findByNameIgnoreCase("Missing"))
                .thenReturn(Optional.empty());
        when(categoryRepository.findByCodeIgnoreCase("Missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.loadMasterData(List.of(request("Missing"))))
                .isInstanceOf(com.customcharge.persistencelayer.exception.ResourceNotFoundException.class);
    }

    @Test
    void loadMasterData_shouldReturnFromCache_whenTreeCached() {
        CategoryNode cachedTree = treeWithChild("ROOT", "CH1");

        when(cacheServiceProvider.getIfAvailable()).thenReturn(cacheService);
        when(cacheService.get(MasterDataCacheKeys.categoryTree("Country"), CategoryNode.class))
                .thenReturn(Optional.of(cachedTree));

        List<MasterDropdownResponse> responses = categoryService.loadMasterData(List.of(request("Country")));

        assertThat(responses.getFirst().tree().getChildren()).hasSize(1);
    }

    @Test
    void loadMasterData_shouldFail_whenTreeMissing() {
        MasterCategoryTreeLoader.MasterCategoryIndex index =
                new MasterCategoryTreeLoader.MasterCategoryIndex(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

        when(cacheServiceProvider.getIfAvailable()).thenReturn(null);
        when(categoryCacheServiceProvider.getIfAvailable()).thenReturn(null);
        when(categoryRepository.findByNameIgnoreCase("MyCategory"))
                .thenReturn(Optional.of(category("MyCategory", "CAT1")));
        when(treeLoader.loadIndex()).thenReturn(index);
        when(treeLoader.buildTree("CAT1", index)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.loadMasterData(List.of(request("MyCategory"))))
                .isInstanceOf(com.customcharge.persistencelayer.exception.ResourceNotFoundException.class);
    }

    @Test
    void loadMasterData_shouldReturnFromDb_whenCacheUnavailable() {
        CategoryNode dbTree = treeWithChild("ROOT", "CH1");
        MasterCategoryTreeLoader.MasterCategoryIndex index =
                new MasterCategoryTreeLoader.MasterCategoryIndex(
                        Map.of("CAT1", List.of()),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of());

        when(cacheServiceProvider.getIfAvailable()).thenReturn(null);
        when(categoryCacheServiceProvider.getIfAvailable()).thenReturn(null);
        when(categoryRepository.findByNameIgnoreCase("CustomCategory"))
                .thenReturn(Optional.of(category("CustomCategory", "CAT1")));
        when(treeLoader.loadIndex()).thenReturn(index);
        when(treeLoader.buildTree("CAT1", index)).thenReturn(Optional.of(dbTree));

        List<MasterDropdownResponse> responses = categoryService.loadMasterData(List.of(request("CustomCategory")));

        assertThat(responses.getFirst().tree().getChildren()).hasSize(1);
    }

    private static Category category(String name, String code) {
        Category category = new Category();
        category.setName(name);
        category.setCode(code);
        category.setIsActive(true);
        return category;
    }

    private static HierarchyRequest request(String categoryName) {
        HierarchyRequest request = new HierarchyRequest();
        request.setCategoryName(categoryName);
        return request;
    }

    private static CategoryNode treeWithChild(String rootValue, String childValue) {
        CategoryNode child = new CategoryNode();
        child.setValue(childValue);
        child.setLabel(childValue);
        CategoryNode root = new CategoryNode();
        root.setValue(rootValue);
        root.setLabel(rootValue);
        root.getChildren().add(child);
        return root;
    }
}
