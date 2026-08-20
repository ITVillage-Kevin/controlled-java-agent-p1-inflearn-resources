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

@Component
public class TraceLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        try {
            MDC.put("traceId", traceId);
            // FLOW POINT:
            // traceId는 현재 HTTP 요청 로그 MDC에만 저장된다.
            //
            // 아직 없는 것:
            // - 응답 헤더 전파
            // - Micrometer Tracing span 연결
            // - OpenTelemetry trace 연결
            // - Langfuse trace 연결
            // - ChatClient 호출과의 parent-child trace 연결
            //
            // 따라서 로그에는 traceId가 보이더라도,
            // 해당 요청이 AI 호출·Prompt·Tool·RAG를 어떻게 거쳤는지는 알 수 없다.
            //
            // 개선 방향:
            // Micrometer Tracing과 OTEL 기반 표준 traceId 전파로 교체한다.
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }
}
