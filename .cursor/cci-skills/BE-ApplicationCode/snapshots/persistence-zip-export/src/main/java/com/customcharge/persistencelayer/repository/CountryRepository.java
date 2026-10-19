package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {

    List<Country> findByActiveTrue();

    Optional<Country> findByCodeIgnoreCase(String code);
}
