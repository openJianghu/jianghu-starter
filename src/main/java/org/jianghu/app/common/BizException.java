package org.jianghu.app.common;

import lombok.Data;

@Data
public class BizException extends RuntimeException {
    private String errorCode;
    private String errorReason;
    private String errorReasonSupplement;

    public BizException(BizEnum bizEnum) {
        super(String.format("errorCode: \"%s\", errorReason: \"%s\" }", bizEnum.getErrorCode(), bizEnum.getErrorReason()));
        this.errorCode = bizEnum.getErrorCode();
        this.errorReason = bizEnum.getErrorReason();
    }

    public BizException(BizEnum bizEnum, String errorReasonSupplement) {
        super(String.format("errorCode: \"%s\", errorReason: \"%s\", errorReasonSupplement: \"%s\"", bizEnum.getErrorCode(), bizEnum.getErrorReason(), errorReasonSupplement));
        this.errorCode = bizEnum.getErrorCode();
        this.errorReason = bizEnum.getErrorReason();
        this.errorReasonSupplement = errorReasonSupplement;
    }

    public BizException(String errorCode, String errorReason) {
        super(String.format("errorCode: \"%s\", errorReason: \"%s\"", errorCode, errorReason));
        this.errorCode = errorCode;
        this.errorReason = errorReason;
    }

    public BizException(String errorCode, String errorReason, String errorReasonSupplement) {
        super(String.format("errorCode: \"%s\", errorReason: \"%s\", errorReasonSupplement: \"%s\"", errorCode, errorReason, errorReasonSupplement));
        this.errorCode = errorCode;
        this.errorReason = errorReason;
        this.errorReasonSupplement = errorReasonSupplement;
    }
}

