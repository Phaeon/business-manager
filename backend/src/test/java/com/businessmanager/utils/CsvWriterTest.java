package com.businessmanager.utils;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvWriterTest {

    @Test
    void escape_quotesValuesContainingSeparator() {
        assertThat(CsvWriter.escape("Dupont; Fils")).isEqualTo("\"Dupont; Fils\"");
    }

    @Test
    void escape_doublesEmbeddedQuotes() {
        assertThat(CsvWriter.escape("Il a dit \"oui\"")).isEqualTo("\"Il a dit \"\"oui\"\"\"");
    }

    @Test
    void escape_neutralisesSpreadsheetFormulas() {
        assertThat(CsvWriter.escape("=CMD()")).startsWith("'");
    }

    @Test
    void escape_keepsNegativeNumbersUntouched() {
        assertThat(CsvWriter.escape("-12,50")).isEqualTo("-12,50");
    }

    @Test
    void toBytes_startsWithBomAndUsesCrLf() {
        String csv = new String(CsvWriter.toBytes(List.of("A", "B"), List.of(List.of("1", "2"))), StandardCharsets.UTF_8);
        assertThat(csv).isEqualTo("\uFEFFA;B\r\n1;2\r\n");
    }
}
