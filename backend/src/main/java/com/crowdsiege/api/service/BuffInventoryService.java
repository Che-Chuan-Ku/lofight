package com.crowdsiege.api.service;

import com.crowdsiege.api.dto.response.BuffQuantityResponse;
import com.crowdsiege.api.dto.response.ItemsResponse;
import com.crowdsiege.api.model.BuffInventory;
import com.crowdsiege.api.model.BuffItem;
import com.crowdsiege.api.repository.BuffInventoryRepository;
import java.util.Arrays;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Buff stock per item, always all three items in ATTACK_SPEED, CANNON, LASER order (0 when not held).
 */
@Service
public class BuffInventoryService {

    private final BuffInventoryRepository buffInventoryRepository;

    public BuffInventoryService(BuffInventoryRepository buffInventoryRepository) {
        this.buffInventoryRepository = buffInventoryRepository;
    }

    @Transactional(readOnly = true)
    public ItemsResponse<BuffQuantityResponse> list(Long playerId) {
        return new ItemsResponse<>(Arrays.stream(BuffItem.values())
                .map(buffItem -> new BuffQuantityResponse(buffItem, buffInventoryRepository
                        .findByPlayerIdAndBuffItem(playerId, buffItem)
                        .map(BuffInventory::getQuantity)
                        .orElse(0)))
                .toList());
    }
}
