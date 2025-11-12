package com.globus.book_shop.service;

import com.globus.book_shop.exception.CurrencyServiceException;
import com.globus.book_shop.service.impl.CurrencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceImplTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @InjectMocks
    private CurrencyServiceImpl currencyService;

    private String testXmlResponse;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(currencyService, "currencyApiUrl", "https://cbr.ru/scripts/XML_daily.asp");
        ReflectionTestUtils.setField(currencyService, "restClient", restClient);

        testXmlResponse = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ValCurs Date="07.11.2025" name="Foreign Currency Market">
                    <Valute ID="R01235">
                        <NumCode>840</NumCode>
                        <CharCode>USD</CharCode>
                        <Nominal>1</Nominal>
                        <Name>Доллар США</Name>
                        <Value>75,1234</Value>
                    </Valute>
                </ValCurs>
                """;
    }

    @Test
    @SuppressWarnings("rawtypes")
    void getUsdRate_ShouldReturnUsdRate() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();
        when(responseSpec.body(String.class)).thenReturn(testXmlResponse);

        BigDecimal result = currencyService.getUsdRate();

        assertNotNull(result);
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
        verify(restClient).get();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void getUsdRate_WhenEmptyResponse_ShouldThrowException() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();
        when(responseSpec.body(String.class)).thenReturn(null);

        assertThrows(CurrencyServiceException.class, () -> currencyService.getUsdRate());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void getUsdRate_WhenUsdNotFound_ShouldThrowException() {
        String xmlWithoutUsd = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ValCurs Date="07.11.2025" name="Foreign Currency Market">
                    <Valute ID="R01239">
                        <NumCode>978</NumCode>
                        <CharCode>EUR</CharCode>
                        <Nominal>1</Nominal>
                        <Name>Евро</Name>
                        <Value>82,5678</Value>
                    </Valute>
                </ValCurs>
                """;

        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();
        when(responseSpec.body(String.class)).thenReturn(xmlWithoutUsd);

        assertThrows(CurrencyServiceException.class, () -> currencyService.getUsdRate());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void convertUsdToRub_ShouldConvertCorrectly() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();
        when(responseSpec.body(String.class)).thenReturn(testXmlResponse);

        BigDecimal usdAmount = new BigDecimal("29.99");
        BigDecimal result = currencyService.convertUsdToRub(usdAmount);

        assertNotNull(result);
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
        assertEquals(2, result.scale());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void convertUsdToRub_WithZeroAmount_ShouldReturnZero() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();
        when(responseSpec.body(String.class)).thenReturn(testXmlResponse);

        BigDecimal result = currencyService.convertUsdToRub(BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO.setScale(2), result);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void getUsdRate_WithVunitRateField_ShouldIgnoreUnknownField() {
        // XML с полем VunitRate, которое возвращает реальный API ЦБ РФ
        String xmlWithVunitRate = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ValCurs Date="07.11.2025" name="Foreign Currency Market">
                    <Valute ID="R01235">
                        <NumCode>840</NumCode>
                        <CharCode>USD</CharCode>
                        <Nominal>1</Nominal>
                        <Name>Доллар США</Name>
                        <Value>75,1234</Value>
                        <VunitRate>75,1234</VunitRate>
                    </Valute>
                </ValCurs>
                """;

        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();
        when(responseSpec.body(String.class)).thenReturn(xmlWithVunitRate);

        BigDecimal result = currencyService.getUsdRate();

        assertNotNull(result);
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
        verify(restClient).get();
    }
}

