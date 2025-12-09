package com.example.rebooktradingservice.service;

import com.example.rebooktradingservice.common.PageResponse;
import com.example.rebooktradingservice.common.ResponseService;
import com.example.rebooktradingservice.enums.State;
import com.example.rebooktradingservice.exception.CMissingDataException;
import com.example.rebooktradingservice.exception.CUnauthorizedException;
import com.example.rebooktradingservice.feigns.BookClient;
import com.example.rebooktradingservice.model.TradingRequest;
import com.example.rebooktradingservice.model.TradingResponse;
import com.example.rebooktradingservice.model.entity.Outbox;
import com.example.rebooktradingservice.model.entity.Trading;
import com.example.rebooktradingservice.model.entity.compositekey.TradingUserId;
import com.example.rebooktradingservice.model.message.NotificationTradeMessage;
import com.example.rebooktradingservice.repository.OutBoxRepository;
import com.example.rebooktradingservice.repository.TradingRepository;
import com.example.rebooktradingservice.repository.TradingUserRepository;
import com.example.rebooktradingservice.utils.NotificationPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class TradingService {

    private final TradingRepository tradingRepository;
    private final TradingReader tradingReader;
    private final BookClient bookClient;
    private final S3Service s3Service;
    private final TradingUserRepository tradingUserRepository;
    private final OutBoxRepository outBoxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void postTrading(TradingRequest request, String userId, MultipartFile file) throws IOException {
        String imageUrl =  s3Service.upload(file);
        Trading trading = new Trading(request, imageUrl, userId);
        tradingRepository.save(trading);

        //거래생성 메세지 발행
        String content = "찜한 도서의 새로운 거래가 등록되었습니다.";
        NotificationTradeMessage message = new NotificationTradeMessage(trading.getId(), content, request.getBookId());
        saveOutBox(message);
    }

    private void saveOutBox(NotificationTradeMessage message) throws JsonProcessingException {
        String payload  = objectMapper.writeValueAsString(message);
        Outbox outBox = new Outbox();
        outBox.setPayload(payload);
        outBoxRepository.save(outBox);
    }

    public TradingResponse getTrading(String userId, Long tradingId) {
        Trading trading = tradingReader.findById(tradingId);
        TradingResponse response = new TradingResponse(trading);
        return checkMarking(response, userId);
    }

    @Transactional
    public void updateState(Long tradingId, State state, String userId) {
        Trading trading = tradingReader.findById(tradingId);
        if(!trading.getUserId().equals(userId)) {
            log.error("Unauthorized updateTrading Access");
            throw new CUnauthorizedException("Unauthorized user Access");
        }
        trading.setState(state);
    }

    @Transactional
    public void updateTrading(TradingRequest request, String userId, Long tradingId, MultipartFile file)
        throws IOException {
        Trading trading = tradingReader.findById(tradingId);
        if(!trading.getUserId().equals(userId)) {
            log.error("Unauthorized updateState Access");
            throw new CUnauthorizedException("Unauthorized user Access");
        }

        if(request.getPrice() != trading.getPrice()) {
            String content = "찜한 제품의 가격이 변동되었습니다.";
            NotificationTradeMessage message =
                new NotificationTradeMessage(tradingId, content, request.getBookId());
            saveOutBox(message);
        }

        if(file != null){
            String imageUrl = s3Service.upload(file);
            trading.setImageUrl(imageUrl);
        }

        trading.update(request, userId);
    }

    public PageResponse<TradingResponse> getTradings(String userId, Pageable pageable) {
        Page<Trading> tradings = tradingReader.readTradings(userId, pageable);
        Page<TradingResponse> responses = tradings.map(TradingResponse::new)
            .map(res -> checkMarking(res, userId));
        return new PageResponse<>(responses);
    }

    @Transactional
    public void deleteTrading(Long tradingId, String userId) {
        if(!tradingRepository.existsById(tradingId)) {
            log.error("Data is not found");
            throw new CMissingDataException("Data is not found");
        }

        Trading trading = tradingReader.findById(tradingId);

        if(!trading.getUserId().equals(userId)) {
            log.error("Unauthorized deleteTrading Access");
            throw new CUnauthorizedException("Unauthorized user Access");
        }

        tradingRepository.deleteById(tradingId);
    }

    public PageResponse<TradingResponse> getAllTradings(String userId, Long bookId, Pageable pageable) {
        Page<Trading> tradings = tradingReader.getAllTradings(bookId, pageable);
        Page<TradingResponse> responses = tradings.map(TradingResponse::new)
            .map(res -> checkMarking(res, userId));
        return new PageResponse<>(responses);
    }

    public PageResponse<TradingResponse> getRecommendations(String userId, Pageable pageable) {
        List<Long> bookIds = bookClient.getRecommendedBooks(userId);
        log.info("Recommendations: {}", bookIds.toString());

        if(bookIds.isEmpty())
            return new PageResponse<>(Page.empty());

        Page<Trading> tradings = tradingReader.getRecommendations(bookIds, pageable);
        Page<TradingResponse> responses = tradings.map(TradingResponse::new)
            .map(res -> checkMarking(res, userId));
        return new PageResponse<>(responses);
    }

    private TradingResponse checkMarking(TradingResponse res, String userId){
        long tradingId = res.getTradingId();
        TradingUserId tradingUserId = new TradingUserId(tradingId, userId);
        if(tradingUserRepository.existsByTradingUserId(tradingUserId)){
            res.setMarked(true);
        }
        return res;
    }

    public PageResponse<TradingResponse> getOthersTradings(String userId, Pageable pageable) {
        return tradingReader.getOthersTradings(userId, pageable);
    }
}
