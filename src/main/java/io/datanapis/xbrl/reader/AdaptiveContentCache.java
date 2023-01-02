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

import io.datanapis.xbrl.DiscoverableTaxonomySet;
import org.apache.commons.lang3.NotImplementedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AdaptiveContentCache implements ContentCache {
    private final Map<String,byte[]> contentMap;
    private final Map<String,String> keyMap;
    private final int capacity;
    private final Map<String,Content> cache;
    private final Deque<String> recentlyUsed;

    public AdaptiveContentCache(String directory, Map<String,byte[]> contentMap) {
        this(50, directory, contentMap);
    }

    public AdaptiveContentCache(int capacity, String directory, Map<String,byte[]> contentMap) {
        this.contentMap = contentMap;
        this.keyMap = getAvailableKeys(directory);
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.recentlyUsed = new ArrayDeque<>(capacity);
    }

    public void clear() {
        /* Not yet implemented */
    }

    @Override
    public byte[] getContents(String key) {
        byte[] bytes = contentMap.get(key);
        if (bytes != null) {
            return bytes;
        }

        Content content = get(key);
        if (content != null) {
            return content.bytes;
        }

        if (!keyMap.containsKey(key)) {
            return null;
        }

        content = getContent(key);
        if (content != null)
            return content.bytes;

        return null;
    }

    @Override
    public void putContents(String key, byte[] bytes) {
        throw new NotImplementedException();
    }

    private Content get(String key) {
        Content content = cache.get(key);
        if (content != null) {
            recentlyUsed.push(key);
            if (recentlyUsed.size() > 2 * capacity) {
                recentlyUsed.removeLast();
            }
        }

        return content;
    }

    private void pruneCache() {
        Set<String> usedKeys = new HashSet<>(recentlyUsed);
        Set<String> keys = new HashSet<>(cache.keySet());
        keys.removeAll(usedKeys);
        if (keys.size() > 0) {
            /* keys = cache.keySet() - recently used keys, i.e., potentially unused keys */
            cache.keySet().removeAll(keys);
        } else {
            /* Since no keys were removed, remove 20% of the keys randomly and re-sync the two structures */
            List<String> l = new ArrayList<>(cache.keySet());
            Collections.shuffle(l);
            l = l.subList(0, l.size() / 5);
            l.forEach(cache.keySet()::remove);
            recentlyUsed.clear();
            recentlyUsed.addAll(cache.keySet());
        }
    }

    private Content getContent(String key) {
        String tf = keyMap.get(key);
        if (tf == null)
            return null;

        try (InputStream inputStream = new FileInputStream(tf);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().endsWith(key)) {
                    byte[] buffer = zipInputStream.readAllBytes();
                    return new Content(key, buffer);
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private static Map<String,String> getAvailableKeys(String directory) {
        Map<String,String> keyMap = new HashMap<>();

        FileSystem fs = FileSystems.getDefault();
        Path path = fs.getPath(directory);
        File file = path.toFile();
        if (!file.isDirectory())
            return keyMap;

        Set<String> collisions = new HashSet<>();
        for (String taxonomy : DiscoverableTaxonomySet.TAXONOMIES) {
            Path tf = path.resolve(taxonomy);
            try (InputStream inputStream = new FileInputStream(tf.toString());
                 ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    if (keyMap.containsKey(entry.getName())) {
                        collisions.add(entry.getName());
                    } else {
                        keyMap.put(entry.getName(), tf.toAbsolutePath().normalize().toString());
                    }
                }
            } catch (Exception ignored) {
            }
        }

        keyMap.keySet().removeAll(collisions);
        return keyMap;
    }

    private static class Content {
        private final String key;
        private final byte[] bytes;

        private Content(String key, byte[] bytes) {
            this.key = key;
            this.bytes = bytes;
        }
    }
}
