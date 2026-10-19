package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.CategoryValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryValueRepository extends JpaRepository<CategoryValue, Long> {
    List<CategoryValue> findByCategory_NameAndIsActiveTrue(String name);

    Optional<CategoryValue> findByNameIgnoreCaseAndIsActiveTrue(String name);

    List<CategoryValue> findByCategory_CodeAndIsActiveTrue(String code);

    Optional<CategoryValue> findByCode(String code);

    List<CategoryValue> findByParent_IdInAndIsActiveTrue(Collection<Long> parentIds);
}
