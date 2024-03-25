package io.datanapis.xbrl.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TaxonomyUtils {
    private static final Logger log = LoggerFactory.getLogger(TaxonomyUtils.class);
    private static final BiPredicate<Path, BasicFileAttributes> ZIP_FILTER = (p, a) -> a.isRegularFile() && p.toString().endsWith(".zip");
    private static final BiPredicate<Path, BasicFileAttributes> FILE_FILTER = (p, a) -> a.isRegularFile();

    private static String lastComponentOf(String fileName) {
        int index = fileName.lastIndexOf('/');
        if (index > 0) {
            return fileName.substring(index + 1);
        } else {
            return fileName;
        }
    }

    public static Map<String,byte[]> buildCacheFromZip(Path path) {
        try {
            List<Path> files = null;
            if (Files.isDirectory(path)) {
                try (Stream<Path> stream = Files.find(path, 1, ZIP_FILTER, FileVisitOption.FOLLOW_LINKS)) {
                    files = stream.collect(Collectors.toList());
                }
            } else if (Files.isRegularFile(path) && path.endsWith(".zip")) {
                files = new ArrayList<>();
                files.add(path);
            }

            if (files == null) {
                return null;
            }

            Map<String,byte[]> contentMap = new LinkedHashMap<>();
            for (Path file : files) {
                try (FileInputStream fileInputStream = new FileInputStream(file.toFile());
                     ZipInputStream zipInputStream = new ZipInputStream(fileInputStream)) {
                    ZipEntry entry;
                    while ((entry = zipInputStream.getNextEntry()) != null) {
                        if (entry.getName().contains("META-INF/"))
                            continue;

                        byte[] buffer = zipInputStream.readAllBytes();
                        String name = lastComponentOf(entry.getName());
                        if (name.length() == 0)
                            continue;

                        if (contentMap.containsKey(name)) {
                            log.info("Duplicate key: [{}] when processing [{}, {}, {}]\n", name, entry.getName(), entry.getSize(), buffer.length);
                            throw new RuntimeException("Duplicate key!");
                        }
                        contentMap.put(name, buffer);
                    }
                }
            }

            return contentMap;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Pattern ROOT_DIRECTORY_PATTERN = Pattern.compile("us-gaap-20[012]\\d(-\\d\\d-\\d\\d)?$");

    public static Path getGaapTaxonomyBasePath(Path path) {
        String pathName = path.toString();

        if (!Files.isRegularFile(path)) {
            log.info("Path [{}] is not a regular file", path);
            return null;
        }

        if (!path.toString().endsWith(".xsd")) {
            log.info("Path [{}] is not an XSD file", path);
            return null;
        }

        do {
            path = path.getParent();
            if (path == null) {
                log.info("Reached end without matching pattern for [{}]", pathName);
                return null;
            }

            assert Files.isDirectory(path);
            String name = path.toString();
            int index = name.lastIndexOf(File.separator);
            if (index < 0) {
                index = 0;
            } else {
                ++index;
            }

            if (ROOT_DIRECTORY_PATTERN.matcher(name).find(index)) {
                return path;
            }
        } while (true);
    }

    public static Map<String,byte[]> buildCacheFromRootXsd(Path path, Function<Path,Path> basePathMapper) {
        Path basePath = basePathMapper.apply(path);
        try {
            if (!Files.isDirectory(basePath)) {
                log.info("Path [{}] is not a directory", basePath);
                return null;
            }

            List<Path> files;
            try (Stream<Path> stream = Files.find(basePath, Integer.MAX_VALUE, FILE_FILTER, FileVisitOption.FOLLOW_LINKS)) {
                files = stream.collect(Collectors.toList());
            }

            Map<String,byte[]> contentMap = new LinkedHashMap<>();
            for (Path file : files) {
                byte[] buffer = Files.readAllBytes(file);
                String name = lastComponentOf(file.toString());
                if (name.length() == 0)
                    continue;

                if (contentMap.containsKey(name)) {
                    log.info("Duplicate key: [{}] when processing [{}, {}]\n", name, file, buffer.length);
                    throw new RuntimeException("Duplicate key!");
                }
                contentMap.put(name, buffer);
            }

            return contentMap;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
