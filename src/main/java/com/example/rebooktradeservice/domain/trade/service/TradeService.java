package com.example.rebooktradeservice.domain.trade.service;

import com.example.rebooktradeservice.common.enums.State;
import com.example.rebooktradeservice.common.exception.TradeException;
import com.example.rebooktradeservice.domain.outbox.OutboxWriter;
import com.example.rebooktradeservice.domain.trade.model.dto.BundleTradeRequest;
import com.example.rebooktradeservice.domain.trade.model.dto.BundleTradeResponse;
import com.example.rebooktradeservice.domain.trade.model.dto.BundleTradeResponse.CreatedTradeItem;
import com.example.rebooktradeservice.domain.trade.model.dto.TradeRequest;
import com.example.rebooktradeservice.domain.trade.model.dto.TradeResponse;
import com.example.rebooktradeservice.domain.trade.model.entity.Trade;
import com.example.rebooktradeservice.domain.trade.model.entity.compositekey.TradeUserId;
import com.example.rebooktradeservice.domain.trade.service.reader.TradeReader;
import com.example.rebooktradeservice.domain.trade.service.reader.TradeUserReader;
import com.example.rebooktradeservice.domain.trade.service.writer.TradeWriter;
import com.example.rebooktradeservice.external.rabbitmq.message.NotificationTradeMessage;
import com.example.rebooktradeservice.external.s3.S3Service;
import com.example.rebooktradeservice.clientfeign.book.BookClient;
import com.rebook.common.core.response.PageResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class TradeService {

    private final TradeReader tradeReader;
    private final TradeWriter tradeWriter;
    private final TradeUserReader tradeUserReader;
    private final OutboxWriter outboxWriter;
    private final BookClient bookClient;
    private final S3Service s3Service;


    @Transactional
    public void postTrade(TradeRequest request, String userId, MultipartFile file) throws IOException {
        // 1. S3에 이미지 업로드
        String imageUrl = s3Service.upload(file);

        // 2. Trade 엔티티 생성 및 저장
        Trade trade = request.toEntity(imageUrl, userId);
        tradeWriter.save(trade);

        // 3. 찜한 사용자들에게 알림 발송 (Outbox 패턴)
        String content = "찜한 도서의 새로운 거래가 등록되었습니다.";
        NotificationTradeMessage message = NotificationTradeMessage.of(trade.getId(), content, request.bookId());
        outboxWriter.save(message);
    }

    @Transactional
    public void updateState(Long tradeId, State state, String userId) {
        // 1. Trade 조회
        Trade trade = tradeReader.findById(tradeId);

        // 2. 권한 검증 (본인 거래인지 확인)
        validateOwnership(trade, userId);

        // 3. 상태 변경
        trade.setState(state);
    }

    @Transactional
    public void updateTrade(TradeRequest request, String userId, Long tradeId, MultipartFile file)
        throws IOException {
        // 1. Trade 조회
        Trade trade = tradeReader.findById(tradeId);

        // 2. 권한 검증
        validateOwnership(trade, userId);

        // 3. 가격 변동 시 알림 발송
        if (request.price() != trade.getPrice()) {
            String content = "찜한 제품의 가격이 변동되었습니다.";
            NotificationTradeMessage message = NotificationTradeMessage.of(tradeId, content, request.bookId());
            outboxWriter.save(message);
        }

        // 4. 이미지 업데이트 (새 이미지가 있는 경우)
        if (file != null) {
            String imageUrl = s3Service.upload(file);
            trade.setImageUrl(imageUrl);
        }

        // 5. Trade 정보 업데이트
        trade.update(request, userId);
    }

    @Transactional
    public void deleteTrade(Long tradeId, String userId) {
        // 1. Trade 존재 여부 확인
        if (!tradeReader.existsById(tradeId)) {
            log.error("Data is not found");
            throw TradeException.notFound("Data is not found");
        }

        // 2. Trade 조회
        Trade trade = tradeReader.findById(tradeId);

        // 3. 권한 검증
        validateOwnership(trade, userId);

        // 4. 삭제
        tradeWriter.deleteById(tradeId);
    }


    public TradeResponse getTrade(String userId, Long tradeId) {
        // 1. Trade 조회
        Trade trade = tradeReader.findById(tradeId);

        // 2. DTO 변환 및 찜 여부 체크
        TradeResponse response = TradeResponse.from(trade);
        return checkMarking(response, userId);
    }

    public PageResponse<TradeResponse> getTrades(String userId, Pageable pageable) {
        // 1. 사용자의 Trade 목록 조회
        Page<Trade> trades = tradeReader.findByUserId(userId, pageable);

        // 2. DTO 변환 및 찜 여부 체크
        Page<TradeResponse> responses = trades.map(TradeResponse::from)
            .map(res -> checkMarking(res, userId));

        return PageResponse.from(responses);
    }

    public PageResponse<TradeResponse> getAllTrades(String userId, Long bookId, Pageable pageable) {
        // 1. 책별 Trade 목록 조회
        Page<Trade> trades = tradeReader.findByBookId(bookId, pageable);

        // 2. DTO 변환 및 찜 여부 체크
        Page<TradeResponse> responses = trades.map(TradeResponse::from)
            .map(res -> checkMarking(res, userId));

        return PageResponse.from(responses);
    }

    public PageResponse<TradeResponse> getRecommendations(String userId, Pageable pageable) {
        // 1. 추천 책 ID 목록 조회 (BookClient 호출)
        List<Long> bookIds = bookClient.getRecommendedBooks(userId);
        log.info("Recommendations: {}", bookIds.toString());

        // 2. 추천 책이 없으면 빈 페이지 반환
        if (bookIds.isEmpty()) {
            return PageResponse.from(Page.empty());
        }

        // 3. 추천 책들의 Trade 목록 조회
        Page<Trade> trades = tradeReader.findByBookIdIn(bookIds, pageable);

        // 4. DTO 변환 및 찜 여부 체크
        Page<TradeResponse> responses = trades.map(TradeResponse::from)
            .map(res -> checkMarking(res, userId));

        return PageResponse.from(responses);
    }

    public PageResponse<TradeResponse> getOthersTrades(String userId, Pageable pageable) {
        // 1. 타인의 Trade 목록 조회
        Page<Trade> trades = tradeReader.findByUserId(userId, pageable);

        // 2. DTO 변환
        Page<TradeResponse> responses = trades.map(TradeResponse::from);

        return PageResponse.from(responses);
    }

    // ========== Bundle Trade Registration ==========

    @Transactional
    public BundleTradeResponse postBundleTrades(BundleTradeRequest request, String userId) {
        // 1. 각 TradeItem을 Trade 엔티티로 변환
        List<Trade> trades = request.trades().stream()
            .map(item -> item.toEntity(userId))
            .toList();

        // 2. 일괄 저장
        List<Trade> savedTrades = tradeWriter.saveAll(trades);

        // 3. 응답 생성
        List<CreatedTradeItem> createdItems = savedTrades.stream()
            .map(savedTrade -> new CreatedTradeItem(
                savedTrade.getId(),
                savedTrade.getBookId(),
                savedTrade.getImageUrl()
            ))
            .toList();

        return new BundleTradeResponse(savedTrades.size(), createdItems);
    }

    // ========== Waiting Trades ==========

    public PageResponse<TradeResponse> getWaitingTrades(String userId, Pageable pageable) {
        Page<Trade> trades = tradeReader.findByUserIdAndState(userId, State.WAITING, pageable);
        Page<TradeResponse> responses = trades.map(TradeResponse::from);
        return PageResponse.from(responses);
    }

    // ========== Complete Trade After Assessment ==========

    @Transactional
    public void completeTradeAfterAssessment(Long tradeId, String userId, String title, String content) {
        // 1. Trade 조회
        Trade trade = tradeReader.findById(tradeId);

        // 2. 권한 검증
        validateOwnership(trade, userId);

        // 3. 상태 검증 (AVAILABLE 상태에서만 완료 가능)
        if (trade.getState() != State.AVAILABLE) {
            throw TradeException.invalidStateTransition(
                "Trade must be in AVAILABLE state to complete. Current state: " + trade.getState());
        }

        // 4. 제목과 내용 업데이트
        tradeWriter.updateTitleAndContent(tradeId, title, content);

        // 5. 찜한 사용자들에게 알림 발송 (Outbox 패턴)
        String notificationContent = "찜한 도서의 새로운 거래가 등록되었습니다.";
        NotificationTradeMessage message = NotificationTradeMessage.of(tradeId, notificationContent, trade.getBookId());
        outboxWriter.save(message);

        log.info("Trade {} completed with title: {}", tradeId, title);
    }

    private void validateOwnership(Trade trade, String userId) {
        if (!trade.getUserId().equals(userId)) {
            throw TradeException.unauthorized("Unauthorized user Access");
        }
    }

    private TradeResponse checkMarking(TradeResponse res, String userId) {
        TradeUserId tradeUserId = TradeUserId.of(res.tradeId(), userId);
        if (tradeUserReader.isMarked(tradeUserId)) {
            return res.withMarked(true);
        }
        return res;
    }
}
