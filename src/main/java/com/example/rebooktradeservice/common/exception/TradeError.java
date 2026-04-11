package com.example.rebooktradeservice.common.exception;

import com.rebook.common.core.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum TradeError implements ErrorCode {
    TRADE_NOT_FOUND(400, "TRD_001", "Trade 조회에 실패했습니다."),
    UNAUTHORIZED(401, "TRD_002", "권한이 없습니다."),
    INVALID_STATE_TRANSITION(400, "TRD_003", "유효하지 않은 상태 변경입니다."),
    INVALID_IMAGE_COUNT(400, "TRD_004", "이미지 개수가 올바르지 않습니다."),
    S3_UPLOAD_FAILED(500, "TRD_005", "이미지 업로드에 실패했습니다."),
    INVALID_FILE_TYPE(400, "TRD_008", "지원하지 않는 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(400, "TRD_009", "파일 크기가 제한을 초과합니다."),
    AI_ASSESSMENT_FAILED(500, "TRD_006", "AI 상태 평가에 실패했습니다."),
    OUTBOX_SAVE_FAILED(500, "TRD_007", "알림 메시지 저장에 실패했습니다."),
    ;

    private final int status;
    private final String code;
    private final String message;
}
