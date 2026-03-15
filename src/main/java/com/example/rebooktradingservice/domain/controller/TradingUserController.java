package com.example.rebooktradingservice.domain.controller;

import com.example.rebooktradingservice.common.CommonResult;
import com.example.rebooktradingservice.common.ResponseService;
import com.example.rebooktradingservice.common.SingleResult;
import com.example.rebooktradingservice.domain.model.dto.TradingResponse;
import com.example.rebooktradingservice.domain.service.TradingUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tradings")
@Tag(name = "찜거래API")
public class TradingUserController {

    private final TradingUserService tradingUserService;

    @PostMapping("/{tradingId}/marks")
    @Operation(summary = "거래찜토글")
    public CommonResult tradingMark(@RequestHeader("X-User-Id") String userId, @PathVariable Long tradingId) {
        tradingUserService.tradingMark(userId, tradingId);
        return ResponseService.getSuccessResult();
    }

    @GetMapping("/marks")
    @Operation(summary = "찜거래목록조회")
    public SingleResult<Page<TradingResponse>> getMarkedTradings(
        @RequestHeader("X-User-Id") String userId,
        @PageableDefault Pageable pageable
    ) {
        return ResponseService.getSingleResult(tradingUserService.getMarkedTradings(userId, pageable));
    }
}
