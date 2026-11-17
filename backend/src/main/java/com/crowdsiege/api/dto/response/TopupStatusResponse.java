package com.crowdsiege.api.dto.response;

import java.util.List;

/**
 * listShopItems topup state: mode AIO (preset packages through ECPay AIO) or DONATE (streamer payment link donateUrl,
 * any amount, no packages); closed (isEnabled=false, no packages) when no ECPay merchant id is configured.
 */
public record TopupStatusResponse(boolean isEnabled, boolean requiresLogin, int minAmountTwd, int coinsPerTwd,
                                  List<TopupPackageResponse> packages, String mode, String donateUrl) {
}
