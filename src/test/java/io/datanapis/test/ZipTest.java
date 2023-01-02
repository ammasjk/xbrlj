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
package io.datanapis.test;

import okhttp3.*;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipTest {
    private static final String ZIP_URL = "https://www.sec.gov/Archives/edgar/data/320193/000119312512444068/0001193125-12-444068-xbrl.zip";

    @Test
    @Category(io.datanapis.test.SlowTest.class)
    public void testXbrlZip() throws Exception {
        final OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5000, TimeUnit.MILLISECONDS)
                .readTimeout(10000, TimeUnit.MILLISECONDS)
                .callTimeout(25000, TimeUnit.MILLISECONDS)
                .build();

        HttpUrl httpUrl = HttpUrl.parse(ZIP_URL);
        assert httpUrl != null;

        Map<String,byte[]> map = new HashMap<>();

        Request request = new Request.Builder().url(httpUrl).build();
        try (Response response = client.newCall(request).execute(); ResponseBody responseBody = response.body()) {
            assert responseBody != null;

            InputStream inputStream = responseBody.byteStream();
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                byte[] buffer = zipInputStream.readAllBytes();
                System.out.println("Name: " + entry.getName() + ", Size: " + entry.getSize() + ", Length: " + buffer.length);
                map.put(entry.getName(), buffer);
            }
        }
    }
}
