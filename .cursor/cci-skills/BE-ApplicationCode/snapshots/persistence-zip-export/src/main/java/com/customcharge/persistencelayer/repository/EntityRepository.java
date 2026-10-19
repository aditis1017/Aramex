package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EntityRepository extends JpaRepository<Entity, Long> {
    Optional<Entity> findById(Long id);
    List<Entity> findByActiveTrue();
    Optional<Entity> findByCode(String code);
    Optional<Entity> findByCodeIgnoreCase(String code);

    Optional<Entity> findByInfoAxsId(String infoAxsId);

    @Query("SELECT e.id FROM Entity e WHERE UPPER(e.code) IN :codes")
    List<Long> findIdsByCodeInIgnoreCase(@Param("codes") List<String> codes);

    @Query("select e from Entity e join fetch e.country c where e.active = true and c.active = true")
    List<Entity> findAllActiveWithActiveCountry();

    @Query("SELECT e.id FROM Entity e WHERE upper(e.code) IN :codes")
    List<Long> findIdsByCodeInIgnoreCase(@Param("codes") Collection<String> codes);
}
