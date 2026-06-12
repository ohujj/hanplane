package com.hanplane.domain.payment.controller;

import com.hanplane.domain.payment.dto.PaymentManualReviewResponse;
import com.hanplane.domain.payment.entity.PayStatus;
import com.hanplane.domain.payment.service.PaymentManualReviewService;
import com.hanplane.domain.payment.service.PaymentService;
import com.hanplane.domain.payment.service.RefundService;
import com.hanplane.domain.user.entity.Role;
import com.hanplane.global.exception.BusinessException;
import com.hanplane.global.exception.GlobalExceptionHandler;
import com.hanplane.global.jwt.UserPrincipal;
import com.hanplane.global.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @InjectMocks
    private PaymentController paymentController;

    @Mock
    private PaymentService paymentService;

    @Mock
    private RefundService refundService;

    @Mock
    private PaymentManualReviewService paymentManualReviewService;

    private MockMvc mockMvc;
    private UserPrincipal currentPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        authenticationPrincipalResolver()
                )
                .build();
    }

    @Test
    void adminCanGetManualReviewPayments() {
        // given
        PageRequest pageable = PageRequest.of(0, 20);
        UserPrincipal admin = new UserPrincipal(1L, Role.ADMIN);
        PaymentManualReviewResponse response = new PaymentManualReviewResponse(
                100L,
                200L,
                PayStatus.CANCEL_REQUIRED,
                "pg-payment-001",
                10000,
                3,
                "cancel failed",
                LocalDateTime.of(2026, 6, 12, 20, 0),
                "batch-trace-001",
                true,
                LocalDateTime.of(2026, 6, 12, 19, 0),
                LocalDateTime.of(2026, 6, 12, 20, 5)
        );
        Page<PaymentManualReviewResponse> page = new PageImpl<>(List.of(response), pageable, 1);

        when(paymentManualReviewService.getManualReviewPayments(PayStatus.CANCEL_REQUIRED, pageable))
                .thenReturn(page);

        // when
        ResponseEntity<ApiResponse<Page<PaymentManualReviewResponse>>> result =
                paymentController.getManualReviewPayments(PayStatus.CANCEL_REQUIRED, pageable, admin);

        // then
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData().getContent()).containsExactly(response);
        verify(paymentManualReviewService).getManualReviewPayments(PayStatus.CANCEL_REQUIRED, pageable);
    }

    @Test
    void userCannotGetManualReviewPayments() {
        // given
        PageRequest pageable = PageRequest.of(0, 20);
        UserPrincipal user = new UserPrincipal(2L, Role.USER);

        // when & then
        assertThatThrownBy(() -> paymentController.getManualReviewPayments(null, pageable, user))
                .isInstanceOf(BusinessException.class);
        verify(paymentManualReviewService, never()).getManualReviewPayments(null, pageable);
    }

    @Test
    void unauthenticatedUserCannotGetManualReviewPayments() {
        // given
        PageRequest pageable = PageRequest.of(0, 20);

        // when & then
        assertThatThrownBy(() -> paymentController.getManualReviewPayments(null, pageable, null))
                .isInstanceOf(BusinessException.class);
        verify(paymentManualReviewService, never()).getManualReviewPayments(null, pageable);
    }

    @Test
    void manualReviewHttpRequestBindsPayStatusAndPageable() throws Exception {
        // given
        currentPrincipal = new UserPrincipal(1L, Role.ADMIN);
        PageRequest pageable = PageRequest.of(1, 5);
        PaymentManualReviewResponse response = new PaymentManualReviewResponse(
                100L,
                200L,
                PayStatus.CANCEL_REQUIRED,
                "pg-payment-001",
                10000,
                3,
                "cancel failed",
                LocalDateTime.of(2026, 6, 12, 20, 0),
                "batch-trace-001",
                true,
                LocalDateTime.of(2026, 6, 12, 19, 0),
                LocalDateTime.of(2026, 6, 12, 20, 5)
        );
        Page<PaymentManualReviewResponse> page = new PageImpl<>(List.of(response), pageable, 6);

        when(paymentManualReviewService.getManualReviewPayments(eq(PayStatus.CANCEL_REQUIRED), any(Pageable.class)))
                .thenReturn(page);

        // when & then
        mockMvc.perform(get("/api/payments/manual-reviews")
                        .param("payStatus", "CANCEL_REQUIRED")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].paymentId").value(100L))
                .andExpect(jsonPath("$.data.content[0].payStatus").value("CANCEL_REQUIRED"))
                .andExpect(jsonPath("$.data.content[0].lastTraceId").value("batch-trace-001"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentManualReviewService).getManualReviewPayments(eq(PayStatus.CANCEL_REQUIRED), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    private HandlerMethodArgumentResolver authenticationPrincipalResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().equals(UserPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                return currentPrincipal;
            }
        };
    }
}
