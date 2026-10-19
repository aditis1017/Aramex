package com.customcharge.persistencelayer.controller;

import com.customcharge.persistencelayer.dto.AbstractDto;
import com.customcharge.persistencelayer.service.AbstractService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractController<D extends AbstractDto> {

    protected final AbstractService<D> abstractService;

    protected AbstractController(AbstractService<D> abstractService) {
        this.abstractService = abstractService;
    }

    @PostMapping
    public D create(@Valid @RequestBody D dto) {
        return abstractService.create(dto);
    }

    @PutMapping("/{id}")
    public Optional<D> update(@PathVariable String id, @Valid @RequestBody D dto) {
        return abstractService.update(UUID.fromString(id), dto);
    }

    @GetMapping("/{id}")
    public Optional<D> getById(@PathVariable String id) {
        return abstractService.findById(UUID.fromString(id));
    }

    @GetMapping
    public List<D> findAll() {
        return abstractService.findAll();
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable String id) {
        abstractService.deleteById(UUID.fromString(id));
    }
}