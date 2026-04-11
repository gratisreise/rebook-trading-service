package com.example.rebooktradeservice.common.exception;

import com.rebook.common.core.exception.BusinessException;
import com.rebook.common.core.exception.ErrorCode;

public class TradeException extends BusinessException {

    public TradeException(ErrorCode code) {
        super(code);
    }
}
