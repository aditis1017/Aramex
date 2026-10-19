package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.ShipmentXmlData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentXmlDataRepository extends JpaRepository<ShipmentXmlData, Integer> {
}
