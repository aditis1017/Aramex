package com.customcharge.chargescalculator.service.batch;

import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Per-row deferred charge and line-item writes. New charges are persisted immediately so later
 * calculators receive charge IDs; line deletes/saves and charge header updates flush once at row end.
 */
@Component
public class RowChargeWriteBuffer {

    private static final ThreadLocal<BufferState> ACTIVE = new ThreadLocal<>();

    private final ChargeRepository chargeRepository;
    private final ChargeLineItemRepository chargeLineItemRepository;

    public RowChargeWriteBuffer(
            ChargeRepository chargeRepository,
            ChargeLineItemRepository chargeLineItemRepository) {
        this.chargeRepository = chargeRepository;
        this.chargeLineItemRepository = chargeLineItemRepository;
    }

    public void open() {
        ACTIVE.set(new BufferState());
    }

    public void flush() {
        BufferState state = ACTIVE.get();
        if (state != null && !state.flushed) {
            flushPending(state);
            state.flushed = true;
        }
    }

    public void close() {
        BufferState state = ACTIVE.get();
        if (state != null) {
            if (!state.flushed) {
                flushPending(state);
            }
            ACTIVE.remove();
        }
    }

    public Charge persistNewCharge(Charge charge) {
        if (charge == null) {
            return null;
        }
        if (charge.getId() != null) {
            return charge;
        }
        return chargeRepository.save(charge);
    }

    public void enqueueLineDeletes(Collection<ChargeLineItem> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        BufferState state = ACTIVE.get();
        if (state == null) {
            deleteLinesImmediately(lines);
            return;
        }
        for (ChargeLineItem line : lines) {
            if (line == null || line.getId() == null) {
                continue;
            }
            state.linesToDeleteById.put(line.getId(), line);
            state.linesToSave.remove(lineKey(line));
        }
    }

    public void enqueueLineSaves(Collection<ChargeLineItem> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        BufferState state = ACTIVE.get();
        if (state == null) {
            chargeLineItemRepository.saveAll(lines);
            return;
        }
        for (ChargeLineItem line : lines) {
            if (line == null) {
                continue;
            }
            if (line.getId() != null) {
                state.linesToDeleteById.remove(line.getId());
            }
            state.linesToSave.put(lineKey(line), line);
        }
    }

    public void enqueueChargeUpdate(Charge charge) {
        if (charge == null || charge.getId() == null) {
            return;
        }
        BufferState state = ACTIVE.get();
        if (state == null) {
            return;
        }
        state.chargesToUpdate.put(charge.getId(), charge);
    }

    private void flushPending(BufferState state) {
        if (!state.linesToDeleteById.isEmpty()) {
            chargeLineItemRepository.deleteAllById(new ArrayList<>(state.linesToDeleteById.keySet()));
            state.linesToDeleteById.clear();
        }
        if (!state.linesToSave.isEmpty()) {
            chargeLineItemRepository.saveAll(new ArrayList<>(state.linesToSave.values()));
            state.linesToSave.clear();
        }
        if (!state.chargesToUpdate.isEmpty()) {
            chargeRepository.saveAll(new ArrayList<>(state.chargesToUpdate.values()));
            state.chargesToUpdate.clear();
        }
    }

    private void deleteLinesImmediately(Collection<ChargeLineItem> lines) {
        List<UUID> ids = new ArrayList<>();
        for (ChargeLineItem line : lines) {
            if (line != null && line.getId() != null) {
                ids.add(line.getId());
            }
        }
        if (!ids.isEmpty()) {
            chargeLineItemRepository.deleteAllById(ids);
        }
    }

    private static LineKey lineKey(ChargeLineItem line) {
        UUID chargeId = line.getCharge() != null ? line.getCharge().getId() : null;
        UUID accountId = line.getAccount() != null ? line.getAccount().getId() : null;
        String serviceCode = line.getServiceCode() == null ? "" : line.getServiceCode().strip().toUpperCase(Locale.ROOT);
        return new LineKey(chargeId, accountId, serviceCode);
    }

    private record LineKey(UUID chargeId, UUID accountId, String serviceCode) {
    }

    private static final class BufferState {
        private final Map<UUID, ChargeLineItem> linesToDeleteById = new LinkedHashMap<>();
        private final Map<LineKey, ChargeLineItem> linesToSave = new LinkedHashMap<>();
        private final Map<UUID, Charge> chargesToUpdate = new LinkedHashMap<>();
        private boolean flushed;
    }
}
