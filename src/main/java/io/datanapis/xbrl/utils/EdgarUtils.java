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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EdgarUtils {
    public static final Pattern TYPE_EXTRACTOR = Pattern.compile("^(\\w+)\\s+-\\s+(\\w+)\\s+-\\s+(.*)$");

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
}
