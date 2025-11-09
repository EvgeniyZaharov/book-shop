package com.globus.book_shop.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
@JacksonXmlRootElement(localName = "ValCurs")
public class CurrencyApiResponse {
    @JacksonXmlProperty(localName = "Date", isAttribute = true)
    String date;

    @JacksonXmlProperty(localName = "name", isAttribute = true)
    String name;

    @JacksonXmlProperty(localName = "Valute")
    @JacksonXmlElementWrapper(useWrapping = false)
    List<CurrencyRate> valutes;
}