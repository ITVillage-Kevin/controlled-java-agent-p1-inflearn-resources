package com.itvillage.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Span;

@Component
public class TraceLoggingFilter extends OncePerRequestFilter {
    //-- 수업 7: 추가 시작
    private static final String TRACE_ID_KEY = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private final Tracer tracer;

    public TraceLoggingFilter(Tracer tracer) {
        this.tracer = tracer;
    }
    //-- 수업 7: 추가 끝

    // 수업 7: 수정
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String traceId = currentTraceId();

        try {
            if (traceId != null) {
                MDC.put(TRACE_ID_KEY, traceId);

                // API 호출자가 HTTP 응답을 받은 뒤
                // 해당 요청을 구분할 수 있도록 traceId를 노출한다.
                response.setHeader(TRACE_ID_HEADER, traceId);
            }

            filterChain.doFilter(request, response);
        } finally {
            // MDC는 ThreadLocal 기반 컨텍스트이기 때문에 정리하지 않으면
            // 이후 같은 worker thread가 처리하는 다른 요청에 이전 요청의 traceId가 섞일 수 있다.
            MDC.remove(TRACE_ID_KEY);
        }
    }

    // 수업 6에서 사용한 doFilterInternal()
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain) throws ServletException, IOException {
//        String traceId = UUID.randomUUID().toString();
//        try {
//            MDC.put("traceId", traceId);
//            // FLOW POINT: 개선 완료
//            // traceId는 현재 HTTP 요청 로그 MDC에만 저장된다.
//            //
//            // 아직 없는 것:
//            // - 응답 헤더 전파
//            // - Micrometer Tracing span 연결
//            // - OpenTelemetry trace 연결
//            // - Langfuse trace 연결
//            // - ChatClient 호출과의 parent-child trace 연결
//            //
//            // 따라서 로그에는 traceId가 보이더라도,
//            // 해당 요청이 AI 호출·Prompt·Tool·RAG를 어떻게 거쳤는지는 알 수 없다.
//            //
//            // 개선 방향:
//            // Micrometer Tracing과 OTEL 기반 표준 traceId 전파로 교체한다.
//            filterChain.doFilter(request, response);
//        } finally {
//            MDC.remove("traceId");
//        }
//    }

    // 수업 7: 추가
    private String currentTraceId() {
        Span currentSpan = tracer.currentSpan();

        if (currentSpan == null) {
            return null;
        }

        // 현재 HTTP 요청 trace에 속한 표준 traceId를 반환
        return currentSpan.context().traceId();
    }
}
