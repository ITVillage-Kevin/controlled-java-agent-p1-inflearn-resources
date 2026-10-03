package com.itvillage.incident.application.quality;

public record QualityControlledResult<T>(T output, QualityControlStatus status) {
    public static <T> QualityControlledResult<T> passed(T output) {
        return new QualityControlledResult<>(output, QualityControlStatus.PASSED);
    }

    public static <T> QualityControlledResult<T> improved(T output) {
        return new QualityControlledResult<>(output, QualityControlStatus.IMPROVED);
    }

    public static <T> QualityControlledResult<T> notResolved(T output) {
        return new QualityControlledResult<>(output, QualityControlStatus.NOT_RESOLVED);
    }
}
