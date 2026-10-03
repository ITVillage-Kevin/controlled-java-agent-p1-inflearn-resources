package com.itvillage.common.exception;

import java.time.OffsetDateTime;

public record ErrorResponse(OffsetDateTime timestamp,
                            String errorCode,
                            String message,
                            String path) {
}
