package com.globus.book_shop.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class CurrencyRate {
    @JacksonXmlProperty(localName = "ID")
    String id;

    @JacksonXmlProperty(localName = "NumCode")
    String numCode;

    @JacksonXmlProperty(localName = "CharCode")
    String charCode;

    @JacksonXmlProperty(localName = "Nominal")
    Integer nominal;

    @JacksonXmlProperty(localName = "Name")
    String name;

    @JacksonXmlProperty(localName = "Value")
    String value;
}