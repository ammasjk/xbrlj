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

import io.datanapis.xbrl.TagNames;
import io.datanapis.xbrl.model.Concept;
import io.datanapis.xbrl.model.ExplicitMember;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;

public class Utils {
    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    public static <T> Set<T> getDistinct(Collection<Concept> concepts, Function<? super Concept, ? extends T> fn) {
        Set<T> set = new HashSet<>();
        for (Concept concept : concepts) {
            T key = fn.apply(concept);
            set.add(key);
        }
        return set;
    }

    public static <T> void print(PrintStream ps, String message, Collection<T> items) {
        boolean first = true;
        ps.print(message);
        for (T item : items) {
            if (!first)
                ps.print(", ");
            ps.print(item.toString());
            first = false;
        }
        ps.println();
    }

    public static <T> void print(PrintWriter writer, String message, Collection<T> items) {
        boolean first = true;
        writer.print(message);
        for (T item : items) {
            if (!first)
                writer.print(", ");
            writer.print(item.toString());
            first = false;
        }
        writer.println();
    }

    public static String getKey(String href) {
        int index = href.lastIndexOf('/');
        if (index > 0) {
            return href.substring(index + 1);
        } else {
            return href;
        }
    }

    public static Collection<String> splitQualifiedName(Concept concept) {
        assert concept != null;
        return splitQualifiedName(concept.getQualifiedName());
    }

    public static Collection<String> splitQualifiedName(String qualifiedName) {

        List<String> strings = new ArrayList<>();

        int index = qualifiedName.indexOf(':');
        if (index > 0) {
            qualifiedName = qualifiedName.substring(index + 1);
        }

        int length = qualifiedName.length();
        int start = 0;
        for (int i = 1; i < length; i++) {
            if (Character.isUpperCase(qualifiedName.charAt(i))) {
                strings.add(qualifiedName.substring(start, i).toLowerCase());
                start = i;
            }
        }
        if (start < length) {
            strings.add(qualifiedName.substring(start).toLowerCase());
        }

        return strings;
    }

    public static List<String> splitString(String refs) {
        List<String> strings = new ArrayList<>();
        int prev = 0, index;

        index = refs.indexOf(' ', prev);
        while (index > 0) {
            strings.add(refs.substring(prev, index));
            prev = index + 1;
            index = refs.indexOf(' ', prev);
        }

        strings.add(refs.substring(prev));
        return strings;
    }

    public static long asLong(Element element) {
        String name = element.getName();
        if (name.equals(TagNames.NON_FRACTION_TAG)) {
            IxNonFraction nonFraction = new IxNonFraction.Builder(element).build();
            return nonFraction.asScaledLong();
        } else {
            return asLong(element.getText());
        }
    }

    public static long asLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException le) {
            // Sometimes the conversion to long will fail just because the value has a .0 added at the end.
            // There is special case for handling this and for treating such values as longs
            try {
                double doubleValue = Double.parseDouble(value);
                return (long)doubleValue;
            } catch (NumberFormatException de) {
                log.info("asLong: Not a number [{}]", value);
                return 0;
            }
        }
    }

    public static int asInt(String value) {
        value = trim(value);
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            if (value.startsWith("\"") || value.startsWith("'")) {
                value = value.substring(1);
                return Integer.parseInt(value);
            }

            throw e;
        }
    }

    public static String trim(String value) {
        if (value == null)
            return null;

        return value.trim();
    }

    public static DateTimeFormatter getDateTimeFormatter(String ixFormat) {
        int index = ixFormat.indexOf(':');
        if (index > 0) {
            ixFormat = ixFormat.substring(index + 1);
        }
        switch (ixFormat) {
            case "datemonthdayyear":
            case "datemonthdayyearen":
            case "date-monthname-day-year-en":
                return DateTimeFormatter.ofPattern("MMMM d, yyyy");
            default:
                throw new RuntimeException("Unhandled date time format [" + ixFormat + "]");
        }
    }

    public static String replaceHtmlChars(String value) {
        value = value.replace((char)160, ' ');  // Replace &nbsp; with a space
        return value;
    }

    public static LocalDate asDate(Element element) {
        String name = element.getName();
        if (name.equals(TagNames.NON_NUMERIC_TAG) || name.equals(TagNames.NON_FRACTION_TAG)) {
            String format = element.attributeValue(TagNames.FORMAT_TAG);
            if (format != null) {
                return asDate(element.getStringValue(), format);
            }
        }

        return asDate(element.getText());
    }

    public static LocalDate asDate(String value, String format) {
        DateTimeFormatter dateTimeFormatter = getDateTimeFormatter(format);
        value = replaceHtmlChars(value);
        try {
            return LocalDate.parse(value, dateTimeFormatter);
        } catch (DateTimeParseException e) {
            log.info("Exception parsing date [{}] using format [{}]. [{}]", value, format, e.toString());
            throw e;
        }
    }

    public static LocalDate asDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            try {
                /* Try parsing as a LocalDateTime. Rarely, date values are formatted as DateTime values */
                LocalDateTime dateTime = LocalDateTime.parse(value);
                return dateTime.toLocalDate();
            } catch (DateTimeParseException ignored) {
            }

            /* This doesn't seem to be a date or a datetime, rethrow the original exception */
            throw e;
        }
    }

    /**
     * Insert a new element into a sorted list. O(log n)
     * @param list list of sorted elements of type <T></T>
     * @param element the element to be inserted
     * @param comparator the comparator to compare two elements
     * @param <T> the types of the elements in the list and the element to be inserted
     */
    public static <T> void insert(List<T> list, T element, Comparator<T> comparator) {
        // Trivial case
        if (list.size() == 0) {
            list.add(element);
            return;
        }

        int min = 0, max = list.size();
        while (min < max - 1) {
            int k = min + (max - min) / 2;
            int result = comparator.compare(list.get(k), element);
            if (result == 0) {
                list.add(k, element);
                return;
            } else if (result < 0) {
                min = k;
            } else {    // result > 0
                max = k;
            }
        }

        assert (max - min) == 1 : "Unhandled condition?";
        /* Element needs to be inserted between min and max */

        int result = comparator.compare(list.get(min), element);
        if (result == 0) {
            list.add(min, element);
        } else if (result < 0) {
            list.add(max, element);
        } else {
            list.add(min, element);
        }
    }

    public static String join(Collection<ExplicitMember> l) {
        StringBuilder builder = new StringBuilder();
        for (ExplicitMember m : l) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(m.toString());
        }
        return "{" + builder + "}";
    }
}
