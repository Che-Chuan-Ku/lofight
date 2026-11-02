package com.crowdsiege.api.service;

import com.crowdsiege.api.common.exception.BusinessException;
import com.crowdsiege.api.common.exception.CommonErrorCode;
import com.crowdsiege.api.common.time.ResponseTimes;
import com.crowdsiege.api.dto.response.EventStatisticsDetailResponse;
import com.crowdsiege.api.dto.response.EventTotalResponse;
import com.crowdsiege.api.dto.response.PurchaseByItemResponse;
import com.crowdsiege.api.model.BuffItem;
import com.crowdsiege.api.model.EventKind;
import com.crowdsiege.api.model.PlayerEvent;
import com.crowdsiege.api.repository.PlayerEventRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Period totals of backend events for operators (ASM-09=B, AMB-04=A): [from, to), compared as absolute instants.
 */
@Service
public class EventStatisticsService {

    private final PlayerEventRepository playerEventRepository;
    private final ResponseTimes responseTimes;

    public EventStatisticsService(PlayerEventRepository playerEventRepository, ResponseTimes responseTimes) {
        this.playerEventRepository = playerEventRepository;
        this.responseTimes = responseTimes;
    }

    @Transactional(readOnly = true)
    public EventStatisticsDetailResponse getStatistics(String fromParam, String toParam) {
        Instant from = parseTime(fromParam, "from");
        Instant to = parseTime(toParam, "to");
        if (from.isAfter(to)) {
            throw new BusinessException(CommonErrorCode.OUT_OF_RANGE, "參數超出範圍 from/to");
        }
        List<PlayerEvent> events = playerEventRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to);
        List<EventTotalResponse> totals = Arrays.stream(EventKind.values())
                .map(kind -> {
                    List<PlayerEvent> ofKind = events.stream().filter(event -> event.getEventKind() == kind).toList();
                    return new EventTotalResponse(kind, ofKind.size(),
                            ofKind.stream().map(PlayerEvent::getPlayerId).distinct().count());
                })
                .toList();
        List<PlayerEvent> purchases = events.stream()
                .filter(event -> event.getEventKind() == EventKind.BUFF_PURCHASE)
                .toList();
        List<PurchaseByItemResponse> byItem = BuffItem.DISPLAY_ORDER.stream()
                .map(buffItem -> {
                    List<PlayerEvent> ofItem = purchases.stream()
                            .filter(event -> event.getBuffItem() == buffItem)
                            .toList();
                    return new PurchaseByItemResponse(buffItem, ofItem.size(),
                            ofItem.stream().mapToLong(PlayerEvent::getPaidCoins).sum());
                })
                .filter(row -> row.count() > 0)
                .toList();
        return new EventStatisticsDetailResponse(responseTimes.toResponse(from), responseTimes.toResponse(to), totals,
                byItem);
    }

    private static Instant parseTime(String value, String parameterName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER, "缺少必填參數 " + parameterName);
        }
        try {
            return OffsetDateTime.parse(value.trim()).toInstant();
        } catch (DateTimeParseException ex) {
            throw new BusinessException(CommonErrorCode.INVALID_TYPE, "參數型別錯誤 " + parameterName);
        }
    }
}
