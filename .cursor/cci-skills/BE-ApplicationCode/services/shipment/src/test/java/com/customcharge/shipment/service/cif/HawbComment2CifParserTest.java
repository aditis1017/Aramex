package com.customcharge.shipment.service.cif;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HawbComment2CifParserTest {

    @Test
    void parse_shouldExtractAmountAndCurrencyFromExtendedFormat() {
        Optional<HawbComment2CifParser.ParsedCif> parsed =
                HawbComment2CifParser.parse("0 TND 757708 Cash 0,0,0,0,0,.000");

        assertThat(parsed).isPresent();
        assertThat(parsed.get().amount()).isEqualTo(757708.0);
        assertThat(parsed.get().currency()).isEqualTo("TND");
    }

    @Test
    void parse_shouldExtractAmountAndCurrencyFromShortFormat() {
        Optional<HawbComment2CifParser.ParsedCif> parsed = HawbComment2CifParser.parse("15 TND");

        assertThat(parsed).isPresent();
        assertThat(parsed.get().amount()).isEqualTo(15.0);
        assertThat(parsed.get().currency()).isEqualTo("TND");
    }

    @Test
    void parse_shouldRejectBlankComment() {
        assertThat(HawbComment2CifParser.parse(null)).isEmpty();
        assertThat(HawbComment2CifParser.parse(" ")).isEmpty();
    }

    @Test
    void parse_shouldRejectSingleToken() {
        assertThat(HawbComment2CifParser.parse("10")).isEmpty();
    }

    @Test
    void parse_shouldRejectInvalidAmount() {
        assertThat(HawbComment2CifParser.parse("abc TND")).isEmpty();
    }

    @Test
    void parse_shouldAcceptNegativeAmount() {
        Optional<HawbComment2CifParser.ParsedCif> parsed = HawbComment2CifParser.parse("-5 TND");

        assertThat(parsed).isPresent();
        assertThat(parsed.get().amount()).isEqualTo(-5.0);
        assertThat(parsed.get().currency()).isEqualTo("TND");
    }
}
