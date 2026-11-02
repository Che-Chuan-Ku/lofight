package com.crowdsiege.api.service;

import com.crowdsiege.api.dto.response.BuffQuantityResponse;
import com.crowdsiege.api.dto.response.ItemsResponse;
import com.crowdsiege.api.model.BuffInventory;
import com.crowdsiege.api.model.BuffItem;
import com.crowdsiege.api.repository.BuffInventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Buff stock per item, always all four items in BuffItem.DISPLAY_ORDER (0 when not held).
 */
@Service
public class BuffInventoryService {

    private final BuffInventoryRepository buffInventoryRepository;

    public BuffInventoryService(BuffInventoryRepository buffInventoryRepository) {
        this.buffInventoryRepository = buffInventoryRepository;
    }

    @Transactional(readOnly = true)
    public ItemsResponse<BuffQuantityResponse> list(Long playerId) {
        return new ItemsResponse<>(BuffItem.DISPLAY_ORDER.stream()
                .map(buffItem -> new BuffQuantityResponse(buffItem, buffInventoryRepository
                        .findByPlayerIdAndBuffItem(playerId, buffItem)
                        .map(BuffInventory::getQuantity)
                        .orElse(0)))
                .toList());
    }
}
