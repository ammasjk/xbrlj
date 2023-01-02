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
package io.datanapis.xbrl.reader;

import java.util.Map;

public class SimpleContentCache implements ContentCache {
    private final Map<String, byte[]> contentMap;

    public SimpleContentCache(Map<String, byte[]> contentMap) {
        this.contentMap = contentMap;
    }

    @Override
    public byte[] getContents(String key) {
        return contentMap.get(key);
    }

    @Override
    public void putContents(String key, byte[] bytes) {
        contentMap.put(key, bytes);
    }

    public void clear() {
        contentMap.clear();
    }
}
