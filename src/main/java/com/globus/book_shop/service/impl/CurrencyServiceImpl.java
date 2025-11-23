package com.globus.book_shop.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.globus.book_shop.exception.CurrencyServiceException;
import com.globus.book_shop.model.CurrencyApiResponse;
import com.globus.book_shop.model.CurrencyRate;
import com.globus.book_shop.service.CurrencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

@Slf4j
@Service
public class CurrencyServiceImpl implements CurrencyService {

    private static final String USD_CODE = "USD";

    private final RestClient restClient;
    private final XmlMapper xmlMapper;
    private final String currencyApiUrl;

    public CurrencyServiceImpl(
            RestClient.Builder restClientBuilder,
            @Value("${currency.api.url}") String currencyApiUrl) {
        this.restClient = restClientBuilder.build();
        this.xmlMapper = XmlMapper.builder()
                .defaultUseWrapper(false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
        this.currencyApiUrl = currencyApiUrl;
    }

    @Override
    @Cacheable(value = "currencyCache", key = "'usd_rate'")
    public BigDecimal getUsdRate() {
        log.debug("Fetching USD rate from currency API");
        try {
            String xmlResponse = fetchXmlResponse();
            CurrencyApiResponse response = parseXmlResponse(xmlResponse);
            CurrencyRate usdRate = findUsdRate(response);
            BigDecimal rate = parseCurrencyValue(usdRate.getValue(), usdRate.getNominal());
            log.info("USD rate fetched successfully: {}", rate);
            return rate;
        } catch (CurrencyServiceException e) {
            log.error("Currency service error: {}", e.getMessage(), e);
            throw e;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse XML response from currency API", e);
            throw new CurrencyServiceException("Failed to parse XML response from currency API", e);
        } catch (Exception e) {
            log.error("Failed to fetch USD rate from currency API", e);
            throw new CurrencyServiceException("Failed to fetch USD rate from currency API", e);
        }
    }

    private String fetchXmlResponse() {
        log.debug("Fetching XML from URL: {}", currencyApiUrl);
        String xmlResponse = restClient.get()
                .uri(currencyApiUrl)
                .retrieve()
                .body(String.class);

        if (xmlResponse == null || xmlResponse.isBlank()) {
            log.error("Empty response from currency API");
            throw new CurrencyServiceException("Empty response from currency API");
        }
        return xmlResponse;
    }

    private CurrencyApiResponse parseXmlResponse(String xmlResponse) throws JsonProcessingException {
        log.debug("Parsing XML response");
        return xmlMapper.readValue(xmlResponse, CurrencyApiResponse.class);
    }

    private CurrencyRate findUsdRate(CurrencyApiResponse response) {
        log.debug("Searching for USD rate in API response");
        CurrencyRate usdRate = response.getValutes().stream()
                .filter(rate -> USD_CODE.equals(rate.getCharCode()))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("USD rate not found in API response");
                    return new CurrencyServiceException("USD rate not found in API response");
                });
        log.debug("USD rate found: value={}, nominal={}", usdRate.getValue(), usdRate.getNominal());
        return usdRate;
    }

    @Override
    public BigDecimal convertUsdToRub(BigDecimal usdAmount) {
        log.debug("Converting USD to RUB - amount: {}", usdAmount);
        BigDecimal usdRate = getUsdRate();
        BigDecimal result = usdAmount.multiply(usdRate).setScale(2, RoundingMode.HALF_UP);
        log.debug("Conversion result: {} USD = {} RUB", usdAmount, result);
        return result;
    }

    private BigDecimal parseCurrencyValue(String value, Integer nominal) {
        try {
            NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
            Number number = format.parse(value);
            BigDecimal rate = BigDecimal.valueOf(number.doubleValue());
            
            if (nominal != null && nominal > 0) {
                rate = rate.divide(BigDecimal.valueOf(nominal), 4, RoundingMode.HALF_UP);
            }
            
            return rate;
        } catch (ParseException e) {
            throw new CurrencyServiceException("Failed to parse currency value: " + value, e);
        }
    }
}