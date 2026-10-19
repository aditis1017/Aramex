package com.customcharge.dashboard.rbac;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class RouteAccessCookieValueCodecTest {

  @Test
  void encodeProducesCookieSafeValueWithoutQuotes() {
    String json = "{\"ready\":true,\"moduleGrants\":{\"CUSTOMS\":[\"VIEW\"]}}";
    String encoded = RouteAccessCookieValueCodec.encode(json);
    assertFalse(encoded.contains("\""));
    assertEquals(json, RouteAccessCookieValueCodec.decode(encoded));
  }

  @Test
  void decodeAcceptsLegacyRawJson() {
    String json = "{\"ready\":true}";
    assertEquals(json, RouteAccessCookieValueCodec.decode(json));
  }

  @Test
  void decodeShouldReturnBlankValuesUnchanged() {
    assertNull(RouteAccessCookieValueCodec.decode(null));
    assertEquals("  ", RouteAccessCookieValueCodec.decode("  "));
  }
}
