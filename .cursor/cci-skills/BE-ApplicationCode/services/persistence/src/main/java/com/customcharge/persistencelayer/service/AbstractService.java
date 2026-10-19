package com.customcharge.persistencelayer.service;

import com.customcharge.persistencelayer.dto.AbstractDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AbstractService<D extends AbstractDto> {

    D create(D dto);

    Optional<D> update(UUID id, D dto);

    Optional<D> findById(UUID id);

    List<D> findAll();

    boolean deleteById(UUID id);
}
