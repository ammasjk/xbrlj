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
package io.datanapis.xbrl;

import com.ctc.wstx.sax.WstxSAXParser;
import io.datanapis.xbrl.reader.ContentCache;
import io.datanapis.xbrl.reader.SimpleContentCache;
import io.datanapis.xbrl.utils.TaxonomyUtils;
import okhttp3.*;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class XbrlReader {
    private static final Logger log = LoggerFactory.getLogger(XbrlReader.class);
    private static final String USER_AGENT;
    private static final File cacheFolder = new File("okHttpCache");

    private final static OkHttpClient client;
    static {
        USER_AGENT = System.getProperty("user.agent", "");
        if (USER_AGENT.isEmpty()) {
            log.error("user.agent is not defined");
        } else {
            log.info("HTTP Client - using [{}] as User-Agent", USER_AGENT);
        }
        client = new OkHttpClient.Builder()
                .cache(new Cache(cacheFolder, 1024 * 1024 * 1024))
                .connectTimeout(5000, TimeUnit.MILLISECONDS)
                .readTimeout(10000, TimeUnit.MILLISECONDS)
                .callTimeout(25000, TimeUnit.MILLISECONDS)
                .addNetworkInterceptor(chain -> {
                    Request originalRequest = chain.request();
                    log.debug("URL: [{}]", originalRequest.url());
                    Request requestWithUserAgent = originalRequest.newBuilder()
                            .header("User-Agent", USER_AGENT)
                            .build();
                    return chain.proceed(requestWithUserAgent);
                })
                .build();
    }

    public XbrlReader() {
    }

    public static int requestCount() {
        return (client.cache() != null) ? client.cache().requestCount() : 0;
    }

    public static int networkCount() {
        return (client.cache() != null) ? client.cache().networkCount() : 0;
    }

    public static int hitCount() {
        return (client.cache() != null) ? client.cache().hitCount() : 0;
    }

    public XbrlInstance getInstance(String url) throws Exception {
        return getInstance(null, url);
    }

    private static SAXReader saxReader() {
        XMLReader reader = null;
//        try {
//            WstxSAXParser saxParser = new WstxSAXParser();
//            reader = saxParser.getXMLReader();
//            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
//            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
//            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
//        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
//            log.info("Exception: " + e);
//            reader = null;
//        }
        SAXReader saxReader = new SAXReader(reader);
        return saxReader;
    }

    /**
     * Used to get the entire zip of the XBRL filing as well as the summary document Financial_Report.xlsx
     *
     * @param url URL of the document to fetch
     * @return A byte array containing the contents of the document
     * @throws Exception Throws an Exception for any network or IO failure
     */
    public byte[] getContents(String url) throws Exception {
        log.info("Reading contents from [{}]", url);
        HttpUrl httpUrl = HttpUrl.parse(url);
        return getContents(httpUrl);
    }

    private static final String FILE = "file://";
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";

    private static final Predicate<String> isFile = url -> url.startsWith(FILE);
    private static final Predicate<String> isHttp = url -> url.startsWith(HTTP) || url.startsWith(HTTPS);

    /**
     * Returns the XBRL instance located at path. Path can either be
     * 1) HTTP url containing an XBRL instance XML, or
     * 2) HTTP url to a Zip file containing the XBRL instance, or
     * 3) A local file containing an XBRL instance XML. We don't support local Zip files yet (i.e. Mode 4 in ResolverImpl)
     *
     * @param dateFiled An optional parameter indicating the date the XBRL instance was filed
     * @param path URL to either the XBRL instance or a Zip containing the XBRL instance
     * @return An XBRL Instance
     * @throws Exception If there is an error accessing the data or parsing the XML
     */
    public XbrlInstance getInstance(LocalDate dateFiled, String path) throws Exception {
        log.info("Reading XBRL from [{}]", path);
        if (isHttp.test(path)) {
            HttpUrl httpUrl = HttpUrl.parse(path);
            return this.getInstance(dateFiled, httpUrl);
        } else {
            FileSystem fs = FileSystems.getDefault();
            Path rootPath = fs.getPath(path).toAbsolutePath();
            return this.fromPath(dateFiled, rootPath);
        }
    }

    /**
     * Returns the taxonomy rooted at path. Path should be a local file. This is useful for analyzing
     * the base US GAAP taxonomy.
     *
     * @param path a file - presumably the root of the taxonomy
     * @return a DiscoverableTaxonomyInstance
     * @throws Exception if there is an error accessing the data or parsing the taxonomy
     */
    public DiscoverableTaxonomySet getTaxonomy(String path, boolean withCaching) throws Exception {
        log.info("Reading taxonomy from [{}]", path);
        FileSystem fs = FileSystems.getDefault();
        Path rootPath = fs.getPath(path).toAbsolutePath();
        if (!Files.exists(rootPath) || !Files.isRegularFile(rootPath))
            throw new FileNotFoundException(path);

        ContentCache contentCache = null;
        if (withCaching) {
            Map<String,byte[]> contentMap = TaxonomyUtils.buildCacheFromRootXsd(rootPath, TaxonomyUtils::getGaapTaxonomyBasePath);
            contentCache = new SimpleContentCache(contentMap);
        }

        Resolver resolver = new ResolverImpl(client, rootPath.getParent(), contentCache);
        return DiscoverableTaxonomySet.fromPath(resolver, rootPath.toString());
    }

    /**
     * Returns the XBRL instance from a Zip stream whose original url is zipUrl. The original url is important to
     * resolve documents referenced from it. Newer zip files contain an iXBRL instance rather than an XBRL instance.
     *
     * @param dateFiled An optional parameter indicating the date the XBRL instance was filed
     * @param zipUrl URL to the zip file
     * @param zipStream A Zip input stream containing the data
     * @return An XBRL Instance
     * @throws Exception If there is an error parsing the XML
     */
    public XbrlInstance getInstanceFromZipStream(LocalDate dateFiled, String zipUrl, InputStream zipStream) throws Exception {
        log.info("Reading XBRL from Zip stream for URL [{}]", zipUrl);
        HttpUrl httpUrl = HttpUrl.parse(zipUrl);
        return this.getInstanceFromZipStream(dateFiled, httpUrl, zipStream);
    }

    private XbrlInstance fromPath(LocalDate dateFiled, Path rootPath) throws Exception {
        if (rootPath.toString().endsWith(".zip")) {
            return fromZip(dateFiled, rootPath);
        } else {
            SAXReader saxReader = XbrlReader.saxReader();
            Document document = saxReader.read(Files.newBufferedReader(rootPath));
            Element root = document.getRootElement();
            Resolver resolver = new ResolverImpl(client, rootPath.getParent());
            return this.getInstance(dateFiled, resolver, root);
        }
    }

    private XbrlInstance fromInstanceXml(LocalDate dateFiled, HttpUrl httpUrl, Reader reader) throws Exception {
        SAXReader saxReader = XbrlReader.saxReader();
        Document document = saxReader.read(reader);
        Element root = document.getRootElement();
        Resolver resolver = new ResolverImpl(client, httpUrl);
        return this.getInstance(dateFiled, resolver, root);
    }

    static final Predicate<String> xmlFile = a -> a.endsWith(".xml");
    static final Predicate<String> htmlFile = a -> a.endsWith(".htm") || a.endsWith(".html");
    static final Predicate<String> auxiliaryXbrlFile =
            a -> a.endsWith("_cal.xml") || a.endsWith("_def.xml") || a.endsWith("_lab.xml") || a.endsWith("_pre.xml");

    /**
     * Returns true if name is the potentially an XBRL instance entry. This is just a rule-based check that
     * seems to work for SEC Edgar.
     *
     * @param name The name to check
     * @return True if the name is potentially an XBRL instance, false otherwise.
     */
    private static boolean isXbrlInstance(String name) {
        if (!xmlFile.test(name))
            return false;

        return !auxiliaryXbrlFile.test(name);
    }

    private static List<Map.Entry<String,byte[]>> getInstanceEntries(Map<String,byte[]> contentMap) {
        /* XBRL logic - look for an XML that is not a Calculation, Definition, Label or Presentation */
        List<Map.Entry<String,byte[]>> candidates =
                contentMap.entrySet().stream().filter(e -> isXbrlInstance(e.getKey())).collect(Collectors.toList());
        if (candidates.size() == 1) {
            return candidates;
        } else if (candidates.size() > 1) {
            log.info("Found multiple XML files in XBRL instance");
            throw new RuntimeException("Multiple files in Zip match XbrlInstance condition [" + candidates.size() + "]");
        }

        /* iXBRL logic - return all HTML files, iXBRL instances can be split across multiple HTML files */
        candidates = contentMap.entrySet().stream().filter(e -> htmlFile.test(e.getKey())).collect(Collectors.toList());
        if (candidates.isEmpty())
            return null;

        return candidates;
    }

    private interface ResolverFactory {
        Resolver create(Map<String,byte[]> contentMap);
    }

    private XbrlInstance fromZip(LocalDate dateFiled, HttpUrl httpUrl, InputStream inputStream) throws Exception {
        ResolverFactory factory = contentMap -> new ResolverImpl(client, httpUrl, new SimpleContentCache(contentMap));
        return fromZip(dateFiled, httpUrl.toString(), factory, inputStream);
    }

    private XbrlInstance fromZip(LocalDate dateFiled, Path rootPath) throws Exception {
        ResolverFactory factory = contentMap -> new ResolverImpl(client, rootPath, new SimpleContentCache(contentMap));
        try (InputStream inputStream = new FileInputStream(rootPath.toString())) {
            return fromZip(dateFiled, rootPath.toString(), factory, inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private XbrlInstance fromZip(LocalDate dateFiled, String sourcePath, ResolverFactory factory, InputStream inputStream) throws Exception {
        Map<String,byte[]> contentMap = new LinkedHashMap<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                byte[] buffer = zipInputStream.readAllBytes();
                log.debug("Name: [{}], Size: [{}], Length: [{}]", entry.getName(), entry.getSize(), buffer.length);
                contentMap.put(entry.getName(), buffer);
            }
            zipInputStream.closeEntry();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<Map.Entry<String,byte[]>> instanceEntries = getInstanceEntries(contentMap);
        if (instanceEntries == null)
            throw new RuntimeException("Instance file missing in Zip [" + sourcePath + "]");

        if (instanceEntries.size() == 1) {
            Map.Entry<String,byte[]> instanceEntry = instanceEntries.get(0);

            Element root = null;
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(instanceEntry.getValue())) {
                SAXReader saxReader = XbrlReader.saxReader();
                Document document = saxReader.read(byteArrayInputStream);
                root = document.getRootElement();
            } catch (DocumentException e) {
                log.info("Error parsing XBRL Instance [{}] for [{}]", instanceEntry.getKey(), sourcePath);
                throw e;
            }

            if (root == null) {
                log.info("Root element is null for [{}, {}]", instanceEntry.getKey(), sourcePath);
                throw new RuntimeException("Null root element");
            }

            Resolver resolver = factory.create(contentMap);
            XbrlInstance instance = this.getInstance(dateFiled, resolver, root);
            resolver.clear();

            return instance;
        } else {
            List<Element> roots = new ArrayList<>();
            for (Map.Entry<String,byte[]> instanceEntry : instanceEntries) {
                try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(instanceEntry.getValue())) {
                    SAXReader saxReader = XbrlReader.saxReader();
                    Document document = saxReader.read(byteArrayInputStream);
                    Element root = document.getRootElement();
                    if (XbrlInstance.isInlineXBRL(root)) {
                        log.info("Adding [{}] as an iXBRL root for [{}]", instanceEntry.getKey(), sourcePath);
                        roots.add(root);
                    } else {
                        log.info("Skipping HTML file [{}] for [{}]. Not an iXBRL instance", instanceEntry.getKey(), sourcePath);
                    }
                } catch (DocumentException e) {
                    log.info("Skipping HTML file [{}] for [{}]. [{}]", instanceEntry.getKey(), sourcePath, e.toString());
                }
            }

            if (roots.isEmpty()) {
                log.info("No root elements [{}]", sourcePath);
                throw new RuntimeException("Zero root elements");
            }

            Resolver resolver = factory.create(contentMap);
            XbrlInstance instance = this.getInstance(dateFiled, resolver, roots);
            resolver.clear();

            return instance;
        }
    }

    private XbrlInstance getInstance(LocalDate dateFiled, HttpUrl httpUrl) throws Exception {
        Request request = new Request.Builder().cacheControl(CacheControl.FORCE_NETWORK).url(httpUrl).build();

        try (Response response = client.newCall(request).execute(); ResponseBody responseBody = response.body()) {
            assert (responseBody != null);

            String url = httpUrl.toString();
            if (url.endsWith(".zip")) {
                return fromZip(dateFiled, httpUrl, responseBody.byteStream());
            } else {
                return fromInstanceXml(dateFiled, httpUrl, responseBody.charStream());
            }
        } catch (Exception e) {
            log.info("Error getting XBRL instance [{}]: [{}]", httpUrl.toString(), e.toString());
            throw e;
        }
    }

    private XbrlInstance getInstanceFromZipStream(LocalDate dateFiled, HttpUrl httpUrl, InputStream zipStream) throws Exception {
        try {
            return fromZip(dateFiled, httpUrl, zipStream);
        } catch (Exception e) {
            log.info("Error getting XBRL instance from Zip stream for [{}]: [{}]", httpUrl.toString(), e.toString());
            throw e;
        }
    }

    private byte[] getContents(HttpUrl httpUrl) throws Exception {
        Request request = new Request.Builder().cacheControl(CacheControl.FORCE_NETWORK).url(httpUrl).build();

        try (Response response = client.newCall(request).execute(); ResponseBody responseBody = response.body()) {
            assert (responseBody != null);
            return responseBody.bytes();
        } catch (Exception e) {
            log.info("Error getting contents of [{}]: [{}]", httpUrl.toString(), e.toString());
            throw e;
        }
    }

    private XbrlInstance getInstance(LocalDate dateFiled, Resolver resolver, Element root) {
        XbrlInstance xbrl;
        if (XbrlInstance.isXBRL(root)) {
            xbrl = XbrlInstance.fromXbrlElement(dateFiled, resolver, root);
            return xbrl;
        } else if (XbrlInstance.isInlineXBRL(root)) {
            /* An iXBRL document can have multiple HTML files and therefore multiple roots - using a single root may not always work */
            List<Element> roots = new ArrayList<>();
            roots.add(root);

            xbrl = XbrlInstance.fromiXBRLElement(dateFiled, resolver, roots);
            return xbrl;
        } else {
            throw new RuntimeException("xbrl instance not found");
        }
    }

    private XbrlInstance getInstance(LocalDate dateFiled, Resolver resolver, List<Element> roots) {
        XbrlInstance xbrl = XbrlInstance.fromiXBRLElement(dateFiled, resolver, roots);
        return xbrl;
    }

    public interface Resolver {
        /**
         * Returns the root path / url for this resolver
         * @return The root path / url for this resolver
         */
        String getRootPath();

        /**
         * Converts path into an absolute url with respect to the root url. Href may be relative or absolute.
         * If path is absolute, this method will return path itself. This is useful when trying to normalize
         * urls embedded inside the root document and those urls may be either relative or absolute with respect
         * to the root document.
         *
         * @param path The url to normalize
         * @return The absolute url
         */
        String getAbsolutePath(String path);

        /**
         * Similar to getAbsolutePath above except the conversion is relative to parentPath
         *
         * @param parentPath The url to use as base for the conversion
         * @param path The url to make absolute
         * @return The absolute url
         */
        String getAbsolutePath(String parentPath, String path);

        /**
         * Reads the XML document at absolutePath and returns the root element in that document.
         *
         * @param absolutePath The url to read from
         * @return The root element if successful
         * @throws Exception Any exceptions encountered
         */
        Element getRootElement(String absolutePath) throws Exception;

        /**
         * Clears all state in this resolver. Actual behavior is implementation dependent
         */
        void clear();
    }

    /**
     * Implementation of the Resolver interface. The resolver works in one of three modes
     * Mode 1. This is the simplest mode where every URL is a http URL and the contents of the URL are fetched remotely
     * Mode 2. This is a variation of the first mode where the fetch mode is still predominantly HTTP based. However,
     *         some contents have already been fetched as part of a zip file. This content needs to be resolved in memory
     * Mode 3. This is predominantly a local mode where the starting file exists locally. However, not all resources
     *         may exist locally and therefore, some resources will still need to be fetched over HTTP
     * Mode 4. This is a variation of Mode 3 where some content exists in a zip file. i.e., Mode 1:Mode 2 as Mode 3:Mode 4
     */
    private static class ResolverImpl implements Resolver {
        private final OkHttpClient client;
        private final HttpUrl rootUrl;
        private final FileSystem fs;
        private final Path rootPath;
        private final ContentCache contentCache;

        /**
         * Mode 1: Implementation where every request will be fetched remotely (save for any caching)
         *
         * @param client the http client
         * @param rootUrl the root url
         */
        private ResolverImpl(OkHttpClient client, HttpUrl rootUrl) {
            this(client, rootUrl, null);
        }

        /**
         * Mode 2: Implementation where some resources have already been fetched as part of a zip and are available
         * in contentMap. Therefore, not all requests will involve a http request.
         *
         * @param client the http client
         * @param rootUrl the root url
         * @param contentCache a map of filenames to the content of those files
         */
        private ResolverImpl(OkHttpClient client, HttpUrl rootUrl, ContentCache contentCache) {
            this.client = client;
            this.rootUrl = rootUrl;
            this.contentCache = contentCache;
            this.fs = FileSystems.getDefault();
            this.rootPath = null;
        }

        /**
         * Mode 3: Implementation where requests will mostly be fetched from the local filesystem. However, some requests
         * will still need to be fetched over http
         *
         * @param client the http client
         * @param rootPath the root path. if a file, the root path becomes the folder containing the file
         */
        private ResolverImpl(OkHttpClient client, Path rootPath) {
            this(client, rootPath, null);
        }

        /**
         * Mode 4: Implementation where requests will mostly be fetched from the local filesystem. However, some requests
         * will still need to be fetched over http while some of them have already been fetched as part of a zip file
         * and are available in memory
         *
         * @param client the http client
         * @param rootPath the root path. if a file, the root path becomes the folder containing the file
         */
        private ResolverImpl(OkHttpClient client, Path rootPath, ContentCache contentCache) {
            this.client = client;
            this.fs = FileSystems.getDefault();
            File file = rootPath.toFile();
            if (!file.isDirectory()) {
                this.rootPath = rootPath.getParent();
            } else {
                this.rootPath = rootPath;
            }
            this.rootUrl = null;
            this.contentCache = contentCache;
        }

        public String getRootPath() {
            if (rootUrl != null) {
                return rootUrl.toString();
            } else {
                return rootPath.toAbsolutePath().normalize().toString();
            }
        }

        public String getAbsolutePath(String path) {
            if (isHttp.test(path)) {
                /* if path is a http url, then it is already fully qualified and needs to be fetched remotely */
                return path;
            } else if (rootUrl != null) {
                /* this is a http based resolver */
                HttpUrl absoluteUrl = rootUrl.resolve(path);
                assert (absoluteUrl != null);

                return absoluteUrl.toString();
            } else {
                /* this is a file based resolver */
                return rootPath.resolve(path).toAbsolutePath().normalize().toString();
            }
        }

        public String getAbsolutePath(String parentPath, String path) {
            if (isHttp.test(parentPath)) {
                /* if parent path is remote, then path will also be remote */
                HttpUrl httpUrl = HttpUrl.parse(parentPath);
                assert (httpUrl != null);

                HttpUrl absoluteUrl = httpUrl.resolve(path);
                assert (absoluteUrl != null);

                return absoluteUrl.toString();
            } else if (isHttp.test(path)) {
                /* however, the parent path can be local while path is remote */
                return path;
            } else {
                /* parent and path are both local */
                Path parent = fs.getPath(parentPath);
                File file = parent.toFile();
                if (!file.isDirectory()) {
                    parent = parent.getParent();
                }
                file = parent.toFile();
                assert file.isDirectory();
                return parent.resolve(path).toAbsolutePath().normalize().toString();
            }
        }

        private static String lastComponentOf(String url) {
            int index = url.lastIndexOf('/');
            if (index > 0) {
                return url.substring(index + 1);
            } else {
                return url;
            }
        }

        private Element fromUrl(HttpUrl httpUrl) throws Exception {
            Request request = new Request.Builder().url(httpUrl).build();
            try (Response response = client.newCall(request).execute(); ResponseBody responseBody = response.body()) {
                assert (responseBody != null);

                if (log.isDebugEnabled()) {
                    int httpCode = response.code();
                    MediaType contentType = responseBody.contentType();
                    if (contentType != null) {
                        log.debug("URL: [{}], Status Code: [{}] - [{}]/[{}]", httpUrl, httpCode, contentType.type(), contentType.subtype());
                    } else {
                        log.debug("URL: [{}], Status Code: [{}]", httpUrl, httpCode);
                    }

                    if (response.networkResponse() != null) {
                        log.debug("Network hit [{}]. Response code: [{}]", httpUrl, response.code());
                    } else if (response.cacheResponse() != null) {
                        log.debug("Cache hit [{}]", httpUrl);
                    }
                }
                Reader reader = responseBody.charStream();
                SAXReader saxReader = XbrlReader.saxReader();
                Document document = saxReader.read(reader);
                return document.getRootElement();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public Element getRootElement(String absolutePath) throws Exception {
            if (isHttp.test(absolutePath)) {
                HttpUrl httpUrl = HttpUrl.parse(absolutePath);
                assert (httpUrl != null);

                if (contentCache != null) {
                    String name = lastComponentOf(absolutePath);
                    byte[] buffer = contentCache.getContents(name);
                    if (buffer != null) {
                        return fromBytes(buffer);
                    }
                }

                return fromUrl(httpUrl);
            } else {
                Path path = fs.getPath(absolutePath);
                if (contentCache != null) {
                    String name = lastComponentOf(absolutePath);
                    byte[] buffer = contentCache.getContents(name);
                    if (buffer != null) {
                        return fromBytes(buffer);
                    }
                }

                log.debug("Reading [{}] from [{}]", absolutePath, path);
                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    SAXReader saxReader = XbrlReader.saxReader();
                    Document document = saxReader.read(reader);
                    return document.getRootElement();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Element fromBytes(byte[] buffer) throws Exception {
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer)) {
                SAXReader saxReader = XbrlReader.saxReader();
                Document document = saxReader.read(byteArrayInputStream);
                return document.getRootElement();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void clear() {
            if (contentCache != null)
                contentCache.clear();
        }
    }
}
