package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    List<Currency> findByActiveTrue();

    @Query("""
            SELECT c FROM Currency c
            JOIN FETCH c.country co
            WHERE c.active = true
            ORDER BY UPPER(c.currencyCode), UPPER(co.code)
            """)
    List<Currency> findAllActiveWithCountry();

    boolean existsByCurrencyCodeIgnoreCaseAndActiveTrue(String currencyCode);

    @Query("""
            SELECT DISTINCT UPPER(c.currencyCode) FROM Currency c
            WHERE c.active = true
            ORDER BY UPPER(c.currencyCode)
            """)
    List<String> findDistinctActiveCurrencyCodes();

    Optional<Currency> findByCountry_CodeIgnoreCaseAndActiveTrue(String countryCode);

    @Query("""
            SELECT c FROM Currency c
            JOIN c.country co
            JOIN com.customcharge.persistencelayer.entities.Entity e ON e.country = co
            WHERE UPPER(e.code) = UPPER(:entityCode) AND c.active = true
            """)
    Optional<Currency> findActiveByEntityCode(@Param("entityCode") String entityCode);
}
