package com.businessmanager.utils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Export CSV compatible Excel FR : séparateur ';' + BOM UTF-8. */
public final class CsvWriter {

    private static final char SEPARATOR = ';';

    private CsvWriter() {
    }

    public static byte[] toBytes(List<String> header, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        appendRow(sb, header);
        for (List<String> row : rows) {
            appendRow(sb, row);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendRow(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(escape(cells.get(i)));
        }
        sb.append("\r\n");
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        String v = value;
        // Neutralise l'injection de formules dans Excel / LibreOffice
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0 && !v.matches("-?\\d+([.,]\\d+)?")) {
            v = "'" + v;
        }
        if (v.indexOf(SEPARATOR) >= 0 || v.indexOf('"') >= 0 || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
