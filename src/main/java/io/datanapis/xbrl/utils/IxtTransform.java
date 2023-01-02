/*
 * Copyright (C) 2020 Jayakumar Muthukumarasamy
 *
 * Logic and regex is from https://github.com/Arelle/Arelle/blob/13205cc2f8ca0345dc20afd56ae4414f7509a5a4/arelle/FunctionIxt.py
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datanapis.xbrl.utils;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IxtTransform {
    private static final Logger log = LoggerFactory.getLogger(IxtTransform.class);

    /* Logic and regex is from https://github.com/Arelle/Arelle/blob/13205cc2f8ca0345dc20afd56ae4414f7509a5a4/arelle/FunctionIxt.py */
    private static final Map<String,Integer> MONTH_NUMBER =
            new ImmutableMap.Builder<String,Integer>()
                    .put("january", 1).put("february", 2).put("march", 3).put("april", 4).put("may", 5).put("june", 6)
                    .put("july", 7).put("august", 8).put("september", 9).put("october", 10).put("november", 11).put("december", 12)
                    .put("jan", 1).put("feb", 2).put("mar", 3).put("apr", 4).put("jun", 6).put("jul", 7)
                    .put("aug", 8).put("sep", 9).put("oct", 10).put("nov", 11).put("dec", 12)
                    .build();

    private static final Map<Integer,Integer> MAX_DAYS_IN_MONTH =
            new ImmutableMap.Builder<Integer,Integer>()
                    .put(1, 31).put(2, 29).put(3, 31).put(4, 30).put(5, 31).put(6, 30).put(7, 31).put(8, 31).put(9, 30)
                    .put(10, 31).put(11, 30).put(12, 31).build();

    private static final Pattern ZERO_DASH =
            Pattern.compile("^[ \\t\\n\\r]*([-]|\u002D|\u058A|\u05BE|\u2010|\u2011|\u2012|\u2013|\u2014|\u2015|\uFE58|\uFE63|\uFF0D)[ \\t\\n\\r]*$");

    private static final Pattern NUM_DOT_DECIMAL =
            Pattern.compile("^[ \\t\\n\\r]*[0-9]{1,3}([, \u00A0]?[0-9]{3})*(\\.[0-9]+)?[ \\t\\n\\r]*$");

    private static final Pattern NUM_DOT_DECIMAL_IN =
            Pattern.compile("^(([0-9]{1,2}[, \u00A0])?([0-9]{2}[, \u00A0])*[0-9]{3})([.][0-9]+)?$|^([0-9]+)([.][0-9]+)?$");

    private static final Pattern NUM_COMMA_DECIMAL =
            Pattern.compile("^[ \\t\\n\\r]*[0-9]{1,3}([. \u00A0]?[0-9]{3})*(,[0-9]+)?[ \\t\\n\\r]*$");

    private static final Pattern NUM_UNIT_DECIMAL =
            Pattern.compile("^([0]|([1-9][0-9]{0,2}([.,\uFF0C\uFF0E]?[0-9]{3})*))[^0-9,.\uFF0C\uFF0E]+([0-9]{1,2})[^0-9,.\uFF0C\uFF0E]*$");

    private static final Pattern NUM_UNIT_DECIMAL_IN =
            Pattern.compile("^(([0-9]{1,2}[, \u00A0])?([0-9]{2}[, \u00A0])*[0-9]{3})([^0-9]+)([0-9]{1,2})([^0-9]*)$|^([0-9]+)([^0-9]+)([0-9]{1,2})([^0-9]*)$");

    private static final Pattern MONTH_DAY_YEAR =
            Pattern.compile("^[ \\t\\n\\r]*([0-9]{1,2})[^0-9]+([0-9]{1,2})[^0-9]+([0-9]{4}|[0-9]{1,2})[ \\t\\n\\r]*$");

    private static final Pattern MONTH_DAY_YEAR_EN =
            Pattern.compile("^[ \\t\\n\\r]*(january|february|march|april|may|june|july|august|september|october|november|december" +
                    "|jan|feb|mar|apr|jun|jul|aug|sep|oct|nov|dec)[^0-9]+([0-9]+)[^0-9]+([0-9]{4}|[0-9]{1,2})[ \\t\\n\\r]*$",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern MONTH_DAY =
            Pattern.compile("^[ \\t\\n\\r]*([0-9]{1,2})[^0-9]+([0-9]{1,2})[A-Za-z]*[ \\t\\n\\r]*$");

    private static final Pattern MONTH_DAY_EN =
            Pattern.compile("^[ \\t\\n\\r]*(january|february|march|april|may|june|july|august|september|october|november|december" +
                    "|jan|feb|mar|apr|jun|jul|aug|sep|oct|nov|dec)[^0-9]+([0-9]{1,2})[A-Za-z]{0,2}[ \\t\\n\\r]*$",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern DAY_MONTH_YEAR =
            Pattern.compile("^[ \\t\\n\\r]*([0-9]{1,2})[^0-9]+([0-9]{1,2})[^0-9]+([0-9]{4}|[0-9]{1,2})[ \\t\\n\\r]*$");

    private static final Pattern DAY_MONTH_YEAR_EN =
            Pattern.compile("^[ \\t\\n\\r]*([0-9]{1,2})[^0-9]+(january|february|march|april|may|june|july|august|september|october|november|december" +
                    "|jan|feb|mar|apr|jun|jul|aug|sep|oct|nov|dec)[^0-9]+([0-9]{4}|[0-9]{1,2})[ \\t\\n\\r]*$",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern MONTH_YEAR =
            Pattern.compile("^[ \\t\\n\\r]*([0-9]{1,2})[^0-9]+([0-9]{4}|[0-9]{1,2})[ \\t\\n\\r]*$");

    private static final Pattern MONTH_YEAR_EN =
            Pattern.compile("^[ \\t\\n\\r]*(january|february|march|april|may|june|july|august|september|october|november|december" +
                    "|jan|feb|mar|apr|jun|jul|aug|sep|oct|nov|dec)[^0-9]+([0-9]{1,2}|[0-9]{4})[ \\t\\n\\r]*$",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern YEAR_MONTH =
            Pattern.compile("^[ \\t\\n\\r]*([0-9]{4}|[0-9]{1,2})[^0-9]+([0-9]{1,2})[^0-9]*$");

    private static final Pattern YEAR_MONTH_DAY =
            Pattern.compile("^[ \\t\\n\\r]*([0-9]{4}|[0-9]{1,2})[^0-9]+([0-9]{1,2})[^0-9]+([0-9]{1,2})[^0-9]*$");

    public static String parseDate(String value, String format) {
        switch (format) {
            case "date-month-year":
            case "datemonthyear":
                return new IxtMonthYear.Builder().monthGroup(1).yearGroup(2).pattern(MONTH_YEAR).build().parse(value);

            case "date-monthname-year-en":
            case "datemonthyearen":
                return new IxtMonthYear.Builder().monthGroup(1).yearGroup(2).pattern(MONTH_YEAR_EN).build().parse(value);

            case "datedaymonthyear":
                return new IxtDate.Builder()
                        .dayGroup(1).monthGroup(2).yearGroup(3).pattern(DAY_MONTH_YEAR).build().parse(value).toString();

            case "datedaymonthyearen":
                return new IxtDate.Builder()
                        .dayGroup(1).monthGroup(2).yearGroup(3).pattern(DAY_MONTH_YEAR_EN).build().parse(value).toString();

            case "dateyearmonthday":
                return new IxtDate.Builder()
                        .dayGroup(3).monthGroup(2).yearGroup(1).pattern(YEAR_MONTH_DAY).build().parse(value).toString();

            case "datequarterend":
                return value;

            case "date-month-day":
            case "datemonthday":
                return new IxtMonthDay.Builder().dayGroup(2).monthGroup(1).pattern(MONTH_DAY).build().parse(value);

            case "date-monthname-day-en":
            case "date-month-day-en":
            case "datemonthdayen":
                return new IxtMonthDay.Builder().dayGroup(2).monthGroup(1).pattern(MONTH_DAY_EN).build().parse(value);

            case "date-month-day-year":
            case "datemonthdayyear":
                return new IxtDate.Builder()
                        .dayGroup(2).monthGroup(1).yearGroup(3).pattern(MONTH_DAY_YEAR).build().parse(value).toString();

            case "datemonthdayyearen":
            case "date-monthname-day-year-en":
                return new IxtDate.Builder()
                        .dayGroup(2).monthGroup(1).yearGroup(3).pattern(MONTH_DAY_YEAR_EN).build().parse(value).toString();

            default:
                log.info("Date format [{}] not yet supported", format);
                return LocalDate.parse(value).toString();
        }
    }

    private static String cleanNumber(String value) {
        char[] cleaned = new char[value.length()];
        int j = 0, i = 0;
        while (i < value.length()) {
            switch (value.charAt(i)) {
                case ',':
                case ' ':
                case '\u00A0':
                    i++;
                    continue;

                default:
                    cleaned[j++] = value.charAt(i++);
                    break;
            }
        }
        return new String(cleaned, 0, j);
    }

    public static String transformWithFormat(String qualifiedFormat, String value) {
        if (qualifiedFormat == null)
            return value;

        String prefix;
        String format = qualifiedFormat;
        int index = format.indexOf(':');
        if (index > 0) {
            prefix = format.substring(0, index);
            format = format.substring(index + 1);
        }

        /* TODO - the cases need to be completed either by validating against the spec or by examples */
        switch (format) {
            case "nocontent":
                return "";

            case "fixed-zero":
                return "0";

            case "zerodash": {
                Matcher matcher = ZERO_DASH.matcher(value);
                if (matcher.matches()) {
                    return "0";
                } else {
                    log.info("Value [{}] does not match ZERO_DASH pattern [{}]", value, ZERO_DASH);
                    throw new RuntimeException("Value [" + value + "] does not match ZERO_DASH pattern [" + ZERO_DASH + "]");
                }
            }

            case "fixed-false":
            case "booleanfalse":
                return "false";

            case "fixed-true":
            case "booleantrue":
                return "true";

            case "num-dot-decimal":
            case "numdotdecimal": {
                Matcher matcher = NUM_DOT_DECIMAL.matcher(value);
                if (matcher.matches()) {
                    return cleanNumber(value);
                } else {
                    log.info("Value [{}] does not match format [{}], pattern [{}]", value, format, NUM_DOT_DECIMAL);
                    throw new RuntimeException("Bad value [" + value + "]");
                }
            }

            case "numdotdecimalin": {
                Matcher matcher = NUM_DOT_DECIMAL_IN.matcher(value);
                if (matcher.matches()) {
                    int nOfGroups = matcher.groupCount();

                    int lastValidIndex = nOfGroups;
                    while (lastValidIndex > 0 && matcher.group(lastValidIndex) == null)
                        --lastValidIndex;

                    int firstValidIndex = 0;
                    while (firstValidIndex <= nOfGroups && matcher.group(firstValidIndex) == null)
                        ++firstValidIndex;

                    if (lastValidIndex == 0 || firstValidIndex > nOfGroups) {
                        log.info("Invalid last [{}] or first index [{}] for value [{}]", lastValidIndex, firstValidIndex, value);
                        throw new RuntimeException("Bad value [" + value + "] for format [" + format + "]");
                    }

                    String fraction = matcher.group(lastValidIndex);
                    if (fraction.length() > 0 && fraction.charAt(0) == '.') {
                        return cleanNumber(matcher.group(firstValidIndex)) + fraction;
                    } else {
                        return cleanNumber(matcher.group(firstValidIndex));
                    }
                } else {
                    log.info("Value [{}] does not match format [{}], pattern [{}]", value, format, NUM_DOT_DECIMAL_IN);
                    throw new RuntimeException("Bad value [" + value + "] for format [" + format + "]");
                }
            }

            case "numunitdecimalin": {
                Matcher matcher = NUM_UNIT_DECIMAL_IN.matcher(value);
                if (matcher.matches()) {
                    int nOfGroups = matcher.groupCount();

                    int firstValidIndex = 0;
                    while (firstValidIndex <= nOfGroups && matcher.group(firstValidIndex) == null)
                        ++firstValidIndex;

                    int lastValidIndex = nOfGroups;
                    while (lastValidIndex > 0 && matcher.group(lastValidIndex) == null)
                        --lastValidIndex;

                    if (lastValidIndex <= 1 || firstValidIndex > nOfGroups) {
                        log.info("Invalid last [{}] or first index [{}] for value [{}]", lastValidIndex, firstValidIndex, value);
                        throw new RuntimeException("Bad value [" + value + "] for format [" + format + "]");
                    }

                    return cleanNumber(matcher.group(firstValidIndex)) + "." +
                            String.format("%02d", Integer.parseInt(matcher.group(lastValidIndex - 1)));
                } else {
                    log.info("Value [{}] does not match format [{}], pattern [{}]", value, format, NUM_UNIT_DECIMAL_IN);
                    throw new RuntimeException("Bad value [" + value + "]");
                }
            }

            case "numcommadecimal": {
                Matcher matcher = NUM_COMMA_DECIMAL.matcher(value);
                if (matcher.matches()) {
                    return value.replace(".", "").replace(",", ".")
                            .replace(" ", "").replace("\u00A0", "");
                } else {
                    log.info("Value [{}] does not match format [{}], pattern [{}]", value, format, NUM_COMMA_DECIMAL);
                    throw new RuntimeException("Bad value [" + value + "]");
                }
            }

            case "date-month-year":
            case "datemonthyear":
            case "date-monthname-year-en":
            case "datemonthyearen":
            case "datedaymonthyear":
            case "datedaymonthyearen":
            case "dateyearmonthday":
            case "datequarterend":
            case "date-month-day":
            case "datemonthday":
            case "date-month-day-en":
            case "datemonthdayen":
            case "date-monthname-day-year-en":
            case "date-month-day-year-en":
            case "datemonthdayyearen":
            case "date-month-day-year":
            case "datemonthdayyear":
                return parseDate(value, format);

            case "boolballotbox":
            case "durday":
            case "durhour":
            case "durweek":
            case "durmonth":
            case "durwordsen":
            case "duryear":
            case "entityfilercategoryen":
            case "edgarprovcountryen":
            case "exchnameen":
            case "numwordsen":
            case "stateprovnameen":
            case "countrynameen":
                return value;

            default:
                log.info("Format [{}]", qualifiedFormat);
                return value;
        }
    }

    private static final class IxtDate {
        private final Pattern pattern;
        private final int dayGroup;
        private final int monthGroup;
        private final int yearGroup;
        private final int maxGroups;

        private IxtDate(Pattern pattern, int dayGroup, int monthGroup, int yearGroup, int maxGroups) {
            this.pattern = pattern;
            this.dayGroup = dayGroup;
            this.monthGroup = monthGroup;
            this.yearGroup = yearGroup;
            this.maxGroups = maxGroups;
        }

        private LocalDate parse(String value) {
            Matcher matcher = pattern.matcher(value);
            boolean result = matcher.matches();
            int groups = matcher.groupCount();
            if (matcher.matches() && matcher.groupCount() == maxGroups) {
                String d = matcher.group(dayGroup);
                String y = matcher.group(yearGroup);
                String m = matcher.group(monthGroup);

                int day = Integer.parseInt(d);
                int year = Integer.parseInt(y);
                int month = Objects.requireNonNullElseGet(MONTH_NUMBER.get(m.toLowerCase()), () -> Integer.parseInt(m));
                return LocalDate.of(year, month, day);
            } else {
                log.info("Date [{}] does not match [{}]", value, pattern.toString());
                throw new RuntimeException("Date [" + value + "] does not match [" + pattern.toString() + "]");
            }
        }

        private static final class Builder {
            private Pattern pattern;
            private int dayGroup;
            private int monthGroup;
            private int yearGroup;
            private int maxGroups = 3;

            private Builder pattern(Pattern pattern) {
                this.pattern = pattern;
                return this;
            }

            private Builder dayGroup(int dayGroup) {
                this.dayGroup = dayGroup;
                return this;
            }

            private Builder monthGroup(int monthGroup) {
                this.monthGroup = monthGroup;
                return this;
            }

            private Builder yearGroup(int yearGroup) {
                this.yearGroup = yearGroup;
                return this;
            }

            private Builder maxGroups(int maxGroups) {
                this.maxGroups = maxGroups;
                return this;
            }

            private IxtDate build() {
                return new IxtDate(pattern, dayGroup, monthGroup, yearGroup, maxGroups);
            }
        }
    }

    private static final class IxtMonthDay {
        private final Pattern pattern;
        private final int dayGroup;
        private final int monthGroup;
        private final int maxGroups;

        private IxtMonthDay(Pattern pattern, int dayGroup, int monthGroup, int maxGroups) {
            this.pattern = pattern;
            this.dayGroup = dayGroup;
            this.monthGroup = monthGroup;
            this.maxGroups = maxGroups;
        }

        private String parse(String value) {
            Matcher matcher = pattern.matcher(value);
            boolean result = matcher.matches();
            int groups = matcher.groupCount();
            if (matcher.matches() && matcher.groupCount() == maxGroups) {
                String d = matcher.group(dayGroup);
                String m = matcher.group(monthGroup);

                int day = Integer.parseInt(d);
                int month = Objects.requireNonNullElseGet(MONTH_NUMBER.get(m.toLowerCase()), () -> Integer.parseInt(m));
                Integer maxDays = MAX_DAYS_IN_MONTH.get(month);
                if (day > maxDays) {
                    throw new RuntimeException("Invalid number of days [" + day + "] in month [" + month + "]");
                }

                return String.format("--{%02d}-{%d}", month, day);
            } else {
                log.info("Month day [{}] does not match [{}]", value, pattern);
                throw new RuntimeException("Month day [" + value + "] does not match [" + pattern + "]");
            }
        }

        private static final class Builder {
            private Pattern pattern;
            private int dayGroup;
            private int monthGroup;
            private int maxGroups = 2;

            private Builder pattern(Pattern pattern) {
                this.pattern = pattern;
                return this;
            }

            private Builder dayGroup(int dayGroup) {
                this.dayGroup = dayGroup;
                return this;
            }

            private Builder monthGroup(int monthGroup) {
                this.monthGroup = monthGroup;
                return this;
            }

            private Builder maxGroups(int maxGroups) {
                this.maxGroups = maxGroups;
                return this;
            }

            private IxtMonthDay build() {
                return new IxtMonthDay(pattern, dayGroup, monthGroup, maxGroups);
            }
        }
    }

    private static final class IxtMonthYear {
        private final Pattern pattern;
        private final int monthGroup;
        private final int yearGroup;
        private final int maxGroups;

        private IxtMonthYear(Pattern pattern, int monthGroup, int yearGroup, int maxGroups) {
            this.pattern = pattern;
            this.monthGroup = monthGroup;
            this.yearGroup = yearGroup;
            this.maxGroups = maxGroups;
        }

        private String parse(String value) {
            Matcher matcher = pattern.matcher(value);
            boolean result = matcher.matches();
            int groups = matcher.groupCount();
            if (matcher.matches() && matcher.groupCount() == maxGroups) {
                String m = matcher.group(monthGroup);
                String y = matcher.group(yearGroup);

                int year = Integer.parseInt(y);
                int month = Objects.requireNonNullElseGet(MONTH_NUMBER.get(m.toLowerCase()), () -> Integer.parseInt(m));

                return String.format("%d-%02d", year, month);
            } else {
                log.info("Month year [{}] does not match [{}]", value, pattern);
                throw new RuntimeException("Month year [" + value + "] does not match [" + pattern + "]");
            }
        }

        private static final class Builder {
            private Pattern pattern;
            private int monthGroup;
            private int yearGroup;
            private int maxGroups = 2;

            private Builder pattern(Pattern pattern) {
                this.pattern = pattern;
                return this;
            }

            private Builder monthGroup(int monthGroup) {
                this.monthGroup = monthGroup;
                return this;
            }

            private Builder yearGroup(int yearGroup) {
                this.yearGroup = yearGroup;
                return this;
            }

            private Builder maxGroups(int maxGroups) {
                this.maxGroups = maxGroups;
                return this;
            }

            private IxtMonthYear build() {
                return new IxtMonthYear(pattern, monthGroup, yearGroup, maxGroups);
            }
        }
    }
}
