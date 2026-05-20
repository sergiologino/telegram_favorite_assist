package com.altacod.favorites.config;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstantAttributeConverterTest {

    private final InstantAttributeConverter converter = new InstantAttributeConverter();

    @Test
    void roundTripsIsoInstant() {
        Instant instant = Instant.parse("2019-07-25T14:29:26Z");
        String stored = converter.convertToDatabaseColumn(instant);
        assertEquals(instant, converter.convertToEntityAttribute(stored));
    }

    @Test
    void readsLegacyEpochMillis() {
        Instant instant = Instant.ofEpochMilli(1779274161964L);
        assertEquals(instant, converter.convertToEntityAttribute("1779274161964"));
    }
}
