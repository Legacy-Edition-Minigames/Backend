package net.kyrptonaught.LEMBackend;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileHelper {

    public static boolean deleteDir(Path directory) {
        try {
            FileUtils.deleteDirectory(directory.toFile());
            return true;
        } catch (IOException exception) {
            System.out.println("Failed to delete directory: " + directory);
            exception.printStackTrace();
        }
        return false;
    }

    public static boolean createDir(Path directory) {
        try {
            Files.createDirectories(directory);
            return true;
        } catch (IOException exception) {
            System.out.println("Failed to create directory: " + directory);
            exception.printStackTrace();
        }
        return false;
    }

    public static boolean copyDirectory(Path source, Path target) {
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Files.createDirectories(target.resolve(source.relativize(dir)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
            return true;
        } catch (IOException exception) {
            System.out.println("Failed to copy directory: " + source + " to: " + target);
            exception.printStackTrace();
        }
        return false;
    }

    public static String readFileFromZip(Path zipFile, String fileName) {
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            ZipEntry entry = zip.getEntry(fileName);

            return new String(zip.getInputStream(entry).readAllBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String cleanFileName(String badFileName) {
        StringBuilder cleanName = new StringBuilder();
        int len = badFileName.codePointCount(0, badFileName.length());
        for (int i = 0; i < len; i++) {
            if (isCharValid(badFileName.charAt(i))) {
                cleanName.append(badFileName.charAt(i));
            }
        }
        return cleanName.toString();
    }

    public static boolean isCharValid(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_' || c == ':' || c == '/' || c == '.' || c == '-';
    }

    public static boolean download(String fileURL, Path savePath) {
        try (InputStream in = openFileOrURL(fileURL)) {
            Files.createDirectories(savePath.getParent());
            Files.copy(in, savePath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static InputStream openFileOrURL(String path) throws IOException {
        path = path.trim();
        if (path.startsWith("file:"))
            return Files.newInputStream(Path.of(path.replace("file:", "")), StandardOpenOption.READ);

        return new URL(path).openStream();
    }

    public static void copyFile(Path source, Path destination) {
        try {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.out.println("Failed to copy file: " + source + " -> " + destination);
            e.printStackTrace();
        }
    }

    public static void zipDirectory(Path directory, Path zip) {
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(zip))) {
            try (Stream<Path> files = Files.walk(directory)) {
                files.filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            ZipEntry zipEntry = new ZipEntry(directory.relativize(path).toString());
                            try {
                                zs.putNextEntry(zipEntry);
                                Files.copy(path, zs);
                                zs.closeEntry();
                            } catch (IOException e) {
                                System.err.println(e);
                            }
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean writeFile(Path filePath, String text) {
        try {
            createDir(filePath.getParent());
            Files.writeString(filePath, text);
            return true;
        } catch (Exception e) {
            System.out.println("Error writing file: " + filePath);
            e.printStackTrace();
        }
        return false;
    }

    public static String readFile(Path filePath) {
        try {
            return Files.readString(filePath);
        } catch (Exception e) {
            System.out.println("Error reading file: " + filePath);
            e.printStackTrace();
        }
        return null;
    }

    public static boolean exists(Path filePath) {
        return Files.exists(filePath);
    }

    public static String fixPathSeparator(String name) {
        return name.replaceAll("\\\\", "/");
    }

    public static String fixPathSeparator(Path path) {
        return fixPathSeparator(path.toString());
    }

    public static String hashFile(byte[] data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            return new BigInteger(1, hash).toString(16);
        } catch (Exception e) {
            System.out.println("Failed to hash file");
            e.printStackTrace();
        }
        return null;
    }
}
