package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.entities.ShipmentMawbDetails;
import com.customcharge.persistencelayer.repository.ShipmentMawbDetailsRepository;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentMawbLinkageService {

    ShipmentMawbDetailsRepository shipmentMawbDetailsRepository;
    ShipmentRepository shipmentRepository;

    @Transactional(rollbackFor = Exception.class)
    public void applyLinkage(ShipmentDto message) {
        String hawb = message.getHawb();
        String sourceEntity = message.getHawbOriginEntity();
        String targetEntity = message.getDestinationEntity();
        String mawbNumber = resolveMawbNumber(message);

        if (!StringUtils.hasText(hawb)
                || !StringUtils.hasText(sourceEntity)
                || !StringUtils.hasText(targetEntity)
                || !StringUtils.hasText(mawbNumber)) {
            log.warn("SH022 MAWB linkage skipped for hawb={}: missing hawb, origin, destination, or mawb", hawb);
            return;
        }

        if (isDuplicateDetails(hawb, mawbNumber, sourceEntity, targetEntity)) {
            log.warn(
                    "SH022 MAWB linkage skipped for hawb={}: duplicate hawb, mawb, origin={}, destination={}",
                    hawb, sourceEntity, targetEntity);
            return;
        }

        saveMawbDetails(hawb, mawbNumber, sourceEntity, targetEntity);
        String firstMawbNumber = findFirstMawbNumber(hawb, sourceEntity, targetEntity).orElse(mawbNumber);
        applyFirstMawbToShipment(hawb, sourceEntity, targetEntity, firstMawbNumber);
    }

    /**
     * When MAWB details were recorded before the shipment row exists (e.g. SH022), use the earliest MAWB on persist.
     */
    public void applyFirstMawbConsoleFromDetails(Shipment shipment) {
        if (shipment == null) {
            return;
        }
        String hawb = trimToNull(shipment.getHawbNumber());
        String sourceEntity = trimToNull(shipment.getOriginEntity());
        String targetEntity = trimToNull(shipment.getHAWBDestinationEntity());
        if (hawb == null || sourceEntity == null || targetEntity == null) {
            return;
        }
        findFirstMawbNumber(hawb, sourceEntity, targetEntity).ifPresent(shipment::setMAWBConsole);
    }

    private boolean isDuplicateDetails(String hawb, String mawbNumber, String sourceEntity, String targetEntity) {
        return shipmentMawbDetailsRepository
                .findByHawbNumberAndMawbNumberAndSourceEntityAndTargetEntity(
                        hawb, mawbNumber, sourceEntity, targetEntity)
                .isPresent();
    }

    private void saveMawbDetails(String hawb, String mawbNumber, String sourceEntity, String targetEntity) {
        ShipmentMawbDetails details = new ShipmentMawbDetails();
        details.setHawbNumber(hawb);
        details.setMawbNumber(mawbNumber);
        details.setSourceEntity(sourceEntity);
        details.setTargetEntity(targetEntity);
        shipmentMawbDetailsRepository.save(details);
        log.info(
                "SH022 saved MAWB details hawb={} mawb={} origin={} destination={}",
                hawb, mawbNumber, sourceEntity, targetEntity);
    }

    private Optional<String> findFirstMawbNumber(String hawb, String sourceEntity, String targetEntity) {
        return shipmentMawbDetailsRepository
                .findFirstByHawbNumberAndSourceEntityAndTargetEntityOrderByCreatedTimeAsc(
                        hawb, sourceEntity, targetEntity)
                .map(ShipmentMawbDetails::getMawbNumber);
    }

    private void applyFirstMawbToShipment(
            String hawb, String sourceEntity, String targetEntity, String firstMawbNumber) {
        shipmentRepository
                .findByHawbNumberAndOriginEntityAndHAWBDestinationEntity(hawb, sourceEntity, targetEntity)
                .ifPresentOrElse(
                        shipment -> updateShipmentMawbConsole(shipment, firstMawbNumber, hawb),
                        () -> log.info("SH022 MAWB details saved; shipment not found for hawb={}", hawb));
    }

    private void updateShipmentMawbConsole(Shipment shipment, String mawbNumber, String hawb) {
        shipment.setMAWBConsole(mawbNumber);
        shipmentRepository.save(shipment);
        log.info("SH022 updated shipment MAWB console hawb={} mawb={}", hawb, mawbNumber);
    }

    static String resolveMawbNumber(ShipmentDto message) {
        if (message == null || !StringUtils.hasText(message.getMawbConsole())) {
            return null;
        }
        return message.getMawbConsole().trim();
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
