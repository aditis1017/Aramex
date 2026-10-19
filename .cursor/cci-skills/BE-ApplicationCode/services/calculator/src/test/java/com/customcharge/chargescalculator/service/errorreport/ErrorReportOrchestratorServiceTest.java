package com.customcharge.chargescalculator.service.errorreport;

import com.customcharge.chargescalculator.config.CustomChargeProperties;
import com.customcharge.chargescalculator.model.ErrorReportStorageResult;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.entities.CustomChargeFileError;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorReportOrchestratorServiceTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private ErrorExcelBuilder errorExcelBuilder;
    @Mock
    private ErrorReportStorageService errorReportStorageService;
    @Mock
    private ErrorReportMailService errorReportMailService;

    private CustomChargeProperties customChargeProperties;

    @InjectMocks
    private ErrorReportOrchestratorService service;

    @BeforeEach
    void setUp() {
        customChargeProperties = new CustomChargeProperties();
        service = new ErrorReportOrchestratorService(
                entityManager,
                customChargeProperties,
                errorExcelBuilder,
                errorReportStorageService,
                errorReportMailService);
    }

    @Test
    void generateAndSendReportAsync_shouldNoOp_whenNoErrors() {
        mockTypedQuery(List.of());

        service.generateAndSendReportAsync(CalculatorTestDataFactory.fileId());

        verify(errorReportStorageService, never()).uploadAndGenerateLink(any(), any(), any());
        verify(errorReportMailService, never()).sendReportMail(any(), any(), any());
    }

    @Test
    void generateAndSendReportAsync_shouldNoOp_whenHeadersMissing() {
        List<Map<String, String>> rows = List.of(Map.of("awb", "AWB-1"));
        mockTypedQuery(List.of(CalculatorTestDataFactory.fileError(rows)));
        when(errorExcelBuilder.collectHeaders(any(), any())).thenReturn(new LinkedHashSet<>());
        when(errorExcelBuilder.finalizeHeaderOrder(any())).thenReturn(List.of());

        service.generateAndSendReportAsync(CalculatorTestDataFactory.fileId());

        verify(errorReportStorageService, never()).uploadAndGenerateLink(any(), any(), any());
        verify(errorReportMailService, never()).sendReportMail(any(), any(), any());
    }

    @Test
    void generateAndSendReportAsync_shouldFetchMultiplePages_whenChunkSizeReached() throws Exception {
        customChargeProperties.getErrorReport().setPageSize(50);
        List<Map<String, String>> rows = List.of(Map.of("awb", "AWB-1"));
        List<CustomChargeFileError> fullPage = java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> CalculatorTestDataFactory.fileError(rows))
                .toList();
        java.util.concurrent.atomic.AtomicInteger pageNumber = new java.util.concurrent.atomic.AtomicInteger();

        @SuppressWarnings("unchecked")
        TypedQuery<CustomChargeFileError> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(CustomChargeFileError.class))).thenReturn(query);
        when(query.setParameter(eq("fileId"), any())).thenReturn(query);
        when(query.setFirstResult(anyInt())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenAnswer(invocation -> {
            int page = pageNumber.getAndIncrement();
            if (page == 0 || page == 2) {
                return fullPage;
            }
            if (page == 1 || page == 3) {
                return List.of(CalculatorTestDataFactory.fileError(List.of(Map.of("awb", "AWB-2"))));
            }
            return List.of();
        });

        doAnswer(invocation -> {
            Set<String> headers = invocation.getArgument(1);
            headers.add("awb");
            return headers;
        }).when(errorExcelBuilder).collectHeaders(any(), any());
        when(errorExcelBuilder.finalizeHeaderOrder(any())).thenReturn(List.of("awb"));
        ErrorExcelBuilder.StreamingWorkbook workbook = mock(ErrorExcelBuilder.StreamingWorkbook.class);
        when(errorExcelBuilder.createStreamingWorkbook(any(), anyInt())).thenReturn(workbook);
        when(errorReportStorageService.uploadAndGenerateLink(any(), any(), any()))
                .thenReturn(new ErrorReportStorageResult("bucket", "key", "http://download"));

        service.generateAndSendReportAsync(CalculatorTestDataFactory.fileId());

        verify(query, org.mockito.Mockito.atLeast(3)).getResultList();
        verify(errorReportMailService).sendReportMail(
                CalculatorTestDataFactory.fileId(), "upload.xlsx", "http://download");
    }

    @Test
    void generateAndSendReportAsync_shouldSwallowExceptions() {
        List<Map<String, String>> rows = List.of(Map.of("awb", "AWB-1"));
        mockTypedQuery(List.of(CalculatorTestDataFactory.fileError(rows)));
        when(errorExcelBuilder.collectHeaders(any(), any())).thenReturn(new LinkedHashSet<>(Set.of("awb")));
        when(errorExcelBuilder.finalizeHeaderOrder(any())).thenReturn(List.of("awb"));
        when(errorExcelBuilder.createStreamingWorkbook(any(), anyInt()))
                .thenThrow(new RuntimeException("workbook failed"));

        assertThatCode(() -> service.generateAndSendReportAsync(CalculatorTestDataFactory.fileId()))
                .doesNotThrowAnyException();

        verify(errorReportMailService, never()).sendReportMail(any(), any(), any());
    }

    @Test
    void generateAndSendReportAsync_shouldStopAfterPartialPage_whenCollectingHeaders() throws Exception {
        customChargeProperties.getErrorReport().setPageSize(50);
        List<Map<String, String>> rows = List.of(Map.of("awb", "AWB-1"));
        java.util.concurrent.atomic.AtomicInteger firstResult = new java.util.concurrent.atomic.AtomicInteger();

        @SuppressWarnings("unchecked")
        TypedQuery<CustomChargeFileError> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(CustomChargeFileError.class))).thenReturn(query);
        when(query.setParameter(eq("fileId"), any())).thenReturn(query);
        when(query.setFirstResult(anyInt())).thenAnswer(invocation -> {
            firstResult.set(invocation.getArgument(0));
            return query;
        });
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenAnswer(invocation ->
                firstResult.get() == 0
                        ? List.of(CalculatorTestDataFactory.fileError(rows))
                        : List.of());

        doAnswer(invocation -> {
            Set<String> headers = invocation.getArgument(1);
            headers.add("awb");
            return headers;
        }).when(errorExcelBuilder).collectHeaders(any(), any());
        when(errorExcelBuilder.finalizeHeaderOrder(any())).thenReturn(List.of("awb"));
        ErrorExcelBuilder.StreamingWorkbook workbook = mock(ErrorExcelBuilder.StreamingWorkbook.class);
        when(errorExcelBuilder.createStreamingWorkbook(any(), anyInt())).thenReturn(workbook);
        when(errorReportStorageService.uploadAndGenerateLink(any(), any(), any()))
                .thenReturn(new ErrorReportStorageResult("bucket", "key", "http://download"));

        service.generateAndSendReportAsync(CalculatorTestDataFactory.fileId());

        verify(errorExcelBuilder).finalizeHeaderOrder(any());
        verify(workbook).appendRows(rows);
    }

    @Test
    void generateAndSendReportAsync_shouldBuildUploadAndMail_whenErrorsExist() throws Exception {
        List<Map<String, String>> rows = List.of(Map.of("awb", "AWB-1", "errorMessage", "failed"));
        CustomChargeFileError fileError = CalculatorTestDataFactory.fileError(rows);
        mockTypedQuery(List.of(fileError));
        doAnswer(invocation -> {
            Set<String> headers = invocation.getArgument(1);
            headers.addAll(Set.of("awb", "errorMessage"));
            return headers;
        }).when(errorExcelBuilder).collectHeaders(any(), any());
        when(errorExcelBuilder.finalizeHeaderOrder(any())).thenAnswer(invocation -> {
            Set<String> headers = invocation.getArgument(0);
            return List.copyOf(headers);
        });
        ErrorExcelBuilder.StreamingWorkbook workbook = mock(ErrorExcelBuilder.StreamingWorkbook.class);
        when(errorExcelBuilder.createStreamingWorkbook(any(), anyInt())).thenReturn(workbook);
        when(errorReportStorageService.uploadAndGenerateLink(any(), any(), any()))
                .thenReturn(new ErrorReportStorageResult("bucket", "key", "http://download"));

        service.generateAndSendReportAsync(CalculatorTestDataFactory.fileId());

        verify(workbook).appendRows(rows);
        verify(workbook).writeTo(any());
        verify(workbook).close();
        verify(errorReportMailService).sendReportMail(
                CalculatorTestDataFactory.fileId(), "upload.xlsx", "http://download");
    }

    @SuppressWarnings("unchecked")
    private TypedQuery<CustomChargeFileError> mockTypedQuery(List<CustomChargeFileError> results) {
        TypedQuery<CustomChargeFileError> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(CustomChargeFileError.class))).thenReturn(query);
        when(query.setParameter(eq("fileId"), any())).thenReturn(query);
        when(query.setFirstResult(anyInt())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenReturn(results);
        return query;
    }
}
