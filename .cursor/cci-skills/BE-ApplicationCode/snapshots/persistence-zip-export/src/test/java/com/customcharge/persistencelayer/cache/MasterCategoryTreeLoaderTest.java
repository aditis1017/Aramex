package com.customcharge.persistencelayer.cache;

import com.customcharge.persistencelayer.cache.dto.MasterCategoryValueRow;
import com.customcharge.persistencelayer.common.CategoryNode;
import com.customcharge.persistencelayer.repository.CategoryRepository;
import com.customcharge.persistencelayer.repository.CategoryValueRepository;
import com.customcharge.persistencelayer.repository.CountryRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MasterCategoryTreeLoaderTest {

    @Mock
    private CategoryValueRepository categoryValueRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private EntityRepository entityRepository;

    private MasterCategoryTreeLoader loader;

    @BeforeEach
    void setUp() {
        loader = new MasterCategoryTreeLoader(
                categoryValueRepository,
                categoryRepository,
                countryRepository,
                entityRepository);
    }

    @Test
    void buildTree_shouldReturnEmpty_whenCategoryCodeMissing() {
        MasterCategoryTreeLoader.MasterCategoryIndex index = emptyIndex();

        assertThat(loader.buildTree(" ", index)).isEmpty();
        assertThat(loader.buildTree("CAT1", null)).isEmpty();
    }

    @Test
    void buildTree_shouldBuildParentChildHierarchy() {
        List<MasterCategoryValueRow> rows = List.of(
                new MasterCategoryValueRow(1L, 10L, "ROOT", "Root", "Root", null, null, true, null),
                new MasterCategoryValueRow(2L, 10L, "CHILD", "Child", "Child", 1L, null, true, null));
        MasterCategoryTreeLoader.MasterCategoryIndex index = indexWithClosure("CAT1", rows);

        Optional<CategoryNode> tree = loader.buildTree("CAT1", index);

        assertThat(tree).isPresent();
        assertThat(tree.get().getChildren()).hasSize(1);
        assertThat(tree.get().getChildren().getFirst().getValue()).isEqualTo("ROOT");
        assertThat(tree.get().getChildren().getFirst().getChildren().getFirst().getValue()).isEqualTo("CHILD");
    }

    private static MasterCategoryTreeLoader.MasterCategoryIndex emptyIndex() {
        return new MasterCategoryTreeLoader.MasterCategoryIndex(
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    private static MasterCategoryTreeLoader.MasterCategoryIndex indexWithClosure(
            String categoryCode,
            List<MasterCategoryValueRow> rows) {
        return new MasterCategoryTreeLoader.MasterCategoryIndex(
                Map.of(categoryCode, rows),
                Map.of(categoryCode, Map.of()),
                Map.of(),
                Map.of(),
                Map.of());
    }
}
