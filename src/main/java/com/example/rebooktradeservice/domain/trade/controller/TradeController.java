package com.example.rebooktradeservice.domain.trade.controller;

import com.rebook.common.auth.PassportProto.Passport;
import com.rebook.common.core.response.PageResponse;
import com.rebook.common.core.response.SuccessResponse;
import com.example.rebooktradeservice.common.enums.State;
import com.example.rebooktradeservice.domain.trade.model.dto.BundleTradeRequest;
import com.example.rebooktradeservice.domain.trade.model.dto.BundleTradeResponse;
import com.example.rebooktradeservice.domain.trade.model.dto.CompleteTradeRequest;
import com.example.rebooktradeservice.domain.trade.model.dto.ConditionAssessmentResponse;
import com.example.rebooktradeservice.domain.trade.model.dto.TradeRequest;
import com.example.rebooktradeservice.domain.trade.model.dto.TradeResponse;
import com.rebook.common.auth.PassportUser;
import com.example.rebooktradeservice.domain.trade.service.ConditionAssessmentService;
import com.example.rebooktradeservice.domain.trade.service.TradeService;
import com.example.rebooktradeservice.domain.trade.service.TradeUserService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeService tradeService;
    private final TradeUserService tradeUserService;
    private final ConditionAssessmentService conditionAssessmentService;

    @GetMapping("/test")
    public String test(@PassportUser Passport passport) {
        return passport.toString();
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<Void>> postTrade(
        @PassportUser String userId,
        @RequestPart TradeRequest request,
        @RequestPart MultipartFile file)
        throws IOException {
        tradeService.postTrade(request, userId, file);
        return SuccessResponse.toNoContent();
    }

    @GetMapping("/{tradeId}")
    public ResponseEntity<SuccessResponse<TradeResponse>> getTrade(
        @PassportUser String userId, @PathVariable Long tradeId) {
        return SuccessResponse.toOk(tradeService.getTrade(userId, tradeId));
    }

    @PatchMapping("/{tradeId}")
    public ResponseEntity<SuccessResponse<Void>> updateState(
        @PathVariable Long tradeId,
        @RequestParam State state,
        @PassportUser String userId
    ) {
        tradeService.updateState(tradeId, state, userId);
        return SuccessResponse.toNoContent();
    }

    @PutMapping("/{tradeId}")
    public ResponseEntity<SuccessResponse<Void>> updateTrade(
        @PathVariable Long tradeId, @PassportUser String userId,
        @RequestPart TradeRequest request,
        @RequestPart(required = false) MultipartFile file
    ) throws IOException {
        tradeService.updateTrade(request, userId, tradeId, file);
        return SuccessResponse.toNoContent();
    }

    @GetMapping("/me")
    public ResponseEntity<SuccessResponse<PageResponse<TradeResponse>>> getTrades(
        @PassportUser String userId, @PageableDefault Pageable pageable
    ) {
        return SuccessResponse.toOk(tradeService.getTrades(userId, pageable));
    }

    @DeleteMapping("/{tradeId}")
    public ResponseEntity<SuccessResponse<Void>> deleteTrade(@PathVariable Long tradeId, @PassportUser String userId) {
        tradeService.deleteTrade(tradeId, userId);
        return SuccessResponse.toNoContent();
    }

    @GetMapping("/books/{bookId}")
    public ResponseEntity<SuccessResponse<PageResponse<TradeResponse>>> getAllTrades(
        @PassportUser String userId,
        @PathVariable Long bookId, @PageableDefault Pageable pageable) {
        return SuccessResponse.toOk(tradeService.getAllTrades(userId, bookId, pageable));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<SuccessResponse<PageResponse<TradeResponse>>> getRecommendations(
        @PassportUser String userId, @PageableDefault Pageable pageable
    ) {
        return SuccessResponse.toOk(tradeService.getRecommendations(userId, pageable));
    }

    @GetMapping("/others/{userId}")
    public ResponseEntity<SuccessResponse<PageResponse<TradeResponse>>> getOthersTrades(
        @PathVariable String userId,
        @PageableDefault Pageable pageable
    ) {
        return SuccessResponse.toOk(tradeService.getOthersTrades(userId, pageable));
    }

    @PostMapping("/{tradeId}/marks")
    public ResponseEntity<SuccessResponse<Void>> tradeMark(@PassportUser String userId, @PathVariable Long tradeId) {
        tradeUserService.tradeMark(userId, tradeId);
        return SuccessResponse.toNoContent();
    }

    @GetMapping("/marks")
    public ResponseEntity<SuccessResponse<Page<TradeResponse>>> getMarkedTrades(
        @PassportUser String userId,
        @PageableDefault Pageable pageable
    ) {
        return SuccessResponse.toOk(tradeUserService.getMarkedTrades(userId, pageable));
    }

    // ========== Bundle Trade Registration ==========

    @PostMapping("/bundle")
    public ResponseEntity<SuccessResponse<BundleTradeResponse>> postBundleTrades(
        @PassportUser String userId,
        @Valid @RequestBody BundleTradeRequest request
    ) {
        return SuccessResponse.toOk(tradeService.postBundleTrades(request, userId));
    }

    // ========== AI Condition Assessment ==========

    @PostMapping("/{tradeId}/assessment")
    public ResponseEntity<SuccessResponse<ConditionAssessmentResponse>> assessCondition(
        @PassportUser String userId,
        @PathVariable Long tradeId,
        @RequestPart List<MultipartFile> files
    ) throws IOException {
        return SuccessResponse.toOk(conditionAssessmentService.assessCondition(tradeId, userId, files));
    }

    // ========== Complete Trade After Assessment ==========

    @PatchMapping("/{tradeId}/complete")
    public ResponseEntity<SuccessResponse<Void>> completeTrade(
        @PassportUser String userId,
        @PathVariable Long tradeId,
        @Valid @RequestBody CompleteTradeRequest request
    ) {
        tradeService.completeTradeAfterAssessment(tradeId, userId, request.title(), request.content());
        return SuccessResponse.toNoContent();
    }

    // ========== Get Waiting Trades ==========

    @GetMapping("/waiting")
    public ResponseEntity<SuccessResponse<PageResponse<TradeResponse>>> getWaitingTrades(
        @PassportUser String userId,
        @PageableDefault Pageable pageable
    ) {
        return SuccessResponse.toOk(tradeService.getWaitingTrades(userId, pageable));
    }
}
