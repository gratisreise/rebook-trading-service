package com.example.rebooktradingservice.controller;

import com.example.rebooktradingservice.common.CommonResult;
import com.example.rebooktradingservice.common.PageResponse;
import com.example.rebooktradingservice.common.ResponseService;
import com.example.rebooktradingservice.common.SingleResult;
import com.example.rebooktradingservice.enums.State;
import com.example.rebooktradingservice.model.TradingRequest;
import com.example.rebooktradingservice.model.TradingResponse;
import com.example.rebooktradingservice.passport.PassportUser;
import com.example.rebooktradingservice.service.TradingReader;
import com.example.rebooktradingservice.service.TradingService;
import com.rebook.passport.PassportProto.Passport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tradings")
@Tag(name="거래API")
public class TradingController {

    private final TradingService tradingService;
    private final TradingReader tradingReader;


    @GetMapping("/test")
    public String test(@PassportUser Passport passport){
        return passport.toString();
    }

    @PostMapping
    @Operation(summary = "거래등록")
    public CommonResult postTrading(
        @RequestHeader("X-User-Id") String userId,
        @RequestPart TradingRequest request,
        @RequestPart MultipartFile file)
        throws IOException {
        tradingService.postTrading(request, userId, file);
        return ResponseService.getSuccessResult();
    }

    @GetMapping("/{tradingId}")
    @Operation(summary = "거래상세조회")
    public SingleResult<TradingResponse> getTrading(
        @RequestHeader("X-User-Id") String userId,@PathVariable Long tradingId){
        return ResponseService.getSingleResult(tradingService.getTrading(userId, tradingId));
    }

    @PatchMapping("/{tradingId}")
    @Operation(summary = "거래상태수정")
    public CommonResult updateState(
        @PathVariable Long tradingId,
        @RequestParam State state,
        @RequestHeader("X-User-Id") String userId
    ){
        tradingService.updateState(tradingId, state, userId);
        return ResponseService.getSuccessResult();
    }

    @PutMapping("/{tradingId}")
    @Operation(summary = "거래수정")
    public CommonResult updateTrading(
        @PathVariable Long tradingId, @RequestHeader("X-User-Id") String userId,
        @RequestPart TradingRequest request,
        @RequestPart(required = false) MultipartFile file
    ) throws IOException {
        tradingService.updateTrading(request, userId, tradingId, file);
        return ResponseService.getSuccessResult();
    }

    @GetMapping("/me")
    @Operation(summary = "거래목록조회")
    public SingleResult<PageResponse<TradingResponse>> getTradings(
        @RequestHeader("X-User-Id") String userId, @PageableDefault Pageable pageable
    ){
        return ResponseService.getSingleResult(tradingService.getTradings(userId, pageable));
    }

    @DeleteMapping("/{tradingId}")
    @Operation(summary = "거래삭제")
    public CommonResult deleteTrading(@PathVariable Long tradingId, @RequestHeader("X-User-Id") String userId){
        tradingService.deleteTrading(tradingId, userId);
        return ResponseService.getSuccessResult();
    }

    @GetMapping("/books/{bookId}")
    @Operation(summary = "모든거래조회")
    public SingleResult<PageResponse<TradingResponse>> getAllTradings(
        @RequestHeader("X-User-Id") String userId,
        @PathVariable Long bookId, @PageableDefault Pageable pageable){
        return ResponseService.getSingleResult(tradingService.getAllTradings(userId, bookId, pageable));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "추천거래목록조회")
    public SingleResult<PageResponse<TradingResponse>> getRecommendations(
        @RequestHeader("X-User-Id") String userId, @PageableDefault Pageable pageable
    ){
        return ResponseService.getSingleResult(tradingService.getRecommendations(userId, pageable));
    }
    @GetMapping("/others/{userId}")
    @Operation(summary ="타인의 거래목록")
    public SingleResult<PageResponse<TradingResponse>> getOthersTradings(
        @PathVariable String userId,
        @PageableDefault Pageable pageable
    ){
        return ResponseService.getSingleResult(tradingService.getOthersTradings(userId, pageable));
    }

}
