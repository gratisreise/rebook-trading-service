package com.example.rebooktradeservice.domain.trade.service;

import com.example.rebooktradeservice.common.enums.BookCondition;
import com.example.rebooktradeservice.common.enums.State;
import com.example.rebooktradeservice.common.exception.TradeException;
import com.example.rebooktradeservice.domain.trade.model.dto.ConditionAssessmentResponse;
import com.example.rebooktradeservice.domain.trade.model.entity.Trade;
import com.example.rebooktradeservice.domain.trade.service.reader.TradeReader;
import com.example.rebooktradeservice.domain.trade.service.writer.TradeWriter;
import com.example.rebooktradeservice.external.gemini.GeminiService;
import com.example.rebooktradeservice.external.gemini.ImageSource;
import com.example.rebooktradeservice.external.gemini.dto.GeminiConditionResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConditionAssessmentService {

    private static final int REQUIRED_IMAGE_COUNT = 3;
    private static final String GEMINI_PROMPT = """
        # Role
        당신은 중고 도서의 상태를 평가하는 전문가입니다.
        
        # Task
        입력으로 제공된 3장의 책 이미지를 종합적으로 분석하여 도서의 전체 상태를 하나의 등급으로 평가하세요.
        
        # Rule
        1. 모든 이미지를 개별적으로 확인한 뒤, 전체 상태를 종합적으로 판단합니다.
        2. 표지의 손상 여부(스크래치, 찢김, 오염)를 확인합니다.
        3. 책등(스파인)의 변형, 주름, 훼손 여부를 확인합니다.
        4. 페이지의 변색, 찢김, 필기, 접힘 여부를 확인합니다.
        5. 가장 손상도가 큰 부분을 기준으로 보수적으로 평가합니다.
        6. 아래 기준에 따라 정확히 하나의 등급만 선택합니다:
           - BEST: 새 책 수준, 표면 손상 없음, 페이지 상태 양호
           - GOOD: 약간의 사용감 있으나 전반적으로 양호
           - MEDIUM: 눈에 띄는 사용감, 약간의 손상 있음
           - POOR: 심한 사용감, 눈에 띄는 손상 있음
        
        # Constraints
        - 반드시 아래 JSON 형식으로만 응답해야 합니다.
        {
          "condition": "BEST" 또는 "GOOD" 또는 "MEDIUM" 또는 "POOR"
        }
        - JSON 외의 추가 설명, 텍스트, 주석은 절대 포함하지 않습니다.
        - condition 값은 반드시 4개 중 하나만 선택해야 합니다.
        """;

    private final GeminiService geminiService;
    private final TradeReader tradeReader;
    private final TradeWriter tradeWriter;

    @Transactional
    public ConditionAssessmentResponse assessCondition(Long tradeId, String userId, List<MultipartFile> files)
        throws IOException {
        // 1. 이미지 개수 검증 (정확히 3장 필요)
        validateImageCount(files);

        // 2. Trade 조회 및 권한 검증
        Trade trade = tradeReader.findById(tradeId);
        validateOwnership(trade, userId);
        validateWaitingState(trade);

        // 3. 이미지를 ImageSource로 변환
        List<ImageSource> imageSources = convertToImageSources(files);

        // 4. Gemini API 호출
        GeminiConditionResponse response = callGeminiForConditionAssessment(imageSources);
        BookCondition condition = response.condition();

        // 5. Trade 상태 및 rating 업데이트
        tradeWriter.updateConditionAndState(tradeId, condition, State.AVAILABLE);

        return new ConditionAssessmentResponse(tradeId, condition, State.AVAILABLE);
    }

    private void validateImageCount(List<MultipartFile> files) {
        if (files == null || files.size() != REQUIRED_IMAGE_COUNT) {
            throw TradeException.invalidImageCount(
                "Exactly " + REQUIRED_IMAGE_COUNT + " images are required for condition assessment");
        }
    }

    private void validateOwnership(Trade trade, String userId) {
        if (!trade.getUserId().equals(userId)) {
            throw TradeException.unauthorized("Unauthorized user Access");
        }
    }

    private void validateWaitingState(Trade trade) {
        if (trade.getState() != State.WAITING) {
            throw TradeException.invalidStateTransition(
                "Trade must be in WAITING state for assessment. Current state: " + trade.getState());
        }
    }

    private List<ImageSource> convertToImageSources(List<MultipartFile> files) throws IOException {
        return files.stream()
            .map(file -> {
                try {
                    return ImageSource.of(file.getBytes(), file.getContentType());
                } catch (IOException e) {
                    throw TradeException.s3UploadFailed("Failed to read image file: " + e.getMessage());
                }
            })
            .toList();
    }

    private GeminiConditionResponse callGeminiForConditionAssessment(List<ImageSource> imageSources) {
        try {
            return geminiService.callObjectWithImages(GEMINI_PROMPT, imageSources, GeminiConditionResponse.class);
        } catch (Exception e) {
            log.error("Gemini API 호출 실패: {}", e.getMessage());
            throw TradeException.aiAssessmentFailed("Failed to assess book condition: " + e.getMessage());
        }
    }
}
