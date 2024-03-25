/*
 * Copyright (C) 2020 Jayakumar Muthukumarasamy
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

import io.datanapis.xbrl.model.RoleType;

import java.time.DateTimeException;
import java.time.MonthDay;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EdgarUtils {
    /**
     * Pattern to extract sortCode - type - title from a role type. This standard is specific to Edgar
     */
    public static final Pattern TYPE_EXTRACTOR = Pattern.compile("^(\\w+)\\s+-\\s+(\\w+)\\s+-\\s+(.*)$");

    private static final String DISCLOSURE = "Disclosure";
    private static final String DOCUMENT = "Document";
    private static final String SCHEDULE = "Schedule";
    private static final String STATEMENT = "Statement";


    /**
     * Given a role types, split its definition into its constituent parts i.e. sortCode, type and title.
     *
     * @param roleType the role type to process
     * @return a string array of length 3, where the 0th index contains the sort code, the 1st index contains
     * the type (i.e., Document, Disclosure, Schedule or Statement) and the 2nd index contains the title.
     */
    public static String[] splitDefinition(RoleType roleType) {
        Matcher matcher = TYPE_EXTRACTOR.matcher(roleType.getDefinition());
        if (matcher.matches()) {
            String[] groups = new String [3];
            for (int i = 0; i < groups.length; i++) {
                groups[i] = matcher.group(i+1);
            }
            return groups;
        }

        return null;
    }

    /**
     * Compare two types and return -1, 0 or 1 as per the following ordering definition:
     *     Document < Statement < Disclosure < Schedule
     *
     * @param lhs the left type
     * @param rhs the right type
     * @return -1, 0 or 1 as lhs is less than, equal to or greater than rhs
     */
    public static int compareType(String lhs, String rhs) {
        switch (lhs) {
            case DOCUMENT -> {
                if (Objects.equals(rhs, DOCUMENT)) {
                    return 0;
                } else {
                    return -1;
                }
            }
            case STATEMENT -> {
                return switch (rhs) {
                    case DOCUMENT -> 1;
                    case STATEMENT -> 0;
                    default -> -1;
                };
            }
            case DISCLOSURE -> {
                return switch (rhs) {
                    case DISCLOSURE -> 0;
                    case DOCUMENT, STATEMENT -> 1;
                    default -> -1;
                };
            }
            default -> {
                return lhs.compareTo(rhs);
            }
        }
    }

    private static final Pattern MONTH_DAY = Pattern.compile(
            "^--\\{?(\\d+)\\}?-\\{?(\\d+)\\}?$");
    private static final Pattern MONTH_DAY_EN_1 = Pattern.compile(
            "^(january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|oct|nov|dec)[^a-zA-Z0-9]+(\\d+)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MONTH_DAY_EN_2 = Pattern.compile(
            "^(\\d+)[^a-zA-Z0-9]+(january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|oct|nov|dec)$",
            Pattern.CASE_INSENSITIVE);

    public static MonthDay parseMonthDay(String monthDayString) {
        if (Objects.isNull(monthDayString))
            return null;

        String monthString = null, dayString = null;
        Matcher matcher;
        matcher = MONTH_DAY.matcher(monthDayString);
        if (matcher.matches()) {
            monthString = matcher.group(1);
            dayString = matcher.group(2);
        } else {
            matcher = MONTH_DAY_EN_1.matcher(monthDayString);
            if (matcher.matches()) {
                monthString = matcher.group(1);
                dayString = matcher.group(2);
            } else {
                matcher = MONTH_DAY_EN_2.matcher(monthDayString);
                if (matcher.matches()) {
                    monthString = matcher.group(1);
                    dayString = matcher.group(2);
                }
            }
            if (Objects.nonNull(monthString) && !monthString.isEmpty()) {
                monthString = switch (monthString.toLowerCase()) {
                    case "jan", "january" -> "1";
                    case "feb", "february" -> "2";
                    case "mar", "march" -> "3";
                    case "apr", "april" -> "4";
                    case "may" -> "5";
                    case "jun", "june" -> "6";
                    case "jul", "july" -> "7";
                    case "aug", "august" -> "8";
                    case "sep", "september" -> "9";
                    case "oct", "october" -> "10";
                    case "nov", "november" -> "11";
                    case "dec", "december" -> "12";
                    default -> null;
                };
            }
        }

        MonthDay monthDay = null;
        if (Objects.nonNull(monthString) && Objects.nonNull(dayString) && !monthString.isEmpty() && !dayString.isEmpty()) {
            try {
                int month = Integer.parseInt(monthString);
                int day = Integer.parseInt(dayString);
                monthDay = MonthDay.of(month, day);
            } catch (NumberFormatException | DateTimeException ignored) {
            }
        }

        return monthDay;
    }
}
