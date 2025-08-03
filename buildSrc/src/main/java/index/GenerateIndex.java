package index;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.isStatic;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.FileSystems.newFileSystem;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.readAttributes;
import static java.nio.file.Files.walk;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import groovy.json.JsonSlurper;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class GenerateIndex<Version, Artifact> extends DefaultTask {

    //#region API for inheritors

    protected abstract Stream<Version> streamAllVersions();

    protected abstract int compareVersions(Version version1, Version version2);

    protected abstract void forPaths(Version version, Action<Collection<Path>> action);

    //#endregion


    //#region inputs

    @Input
    public abstract Property<Version> getMinVersion();

    @Input
    @org.gradle.api.tasks.Optional
    public abstract Property<Version> getMaxVersion();

    @Input
    public abstract Property<Boolean> getIncludePublicClassesOnly();

    {
        getIncludePublicClassesOnly().convention(true);
    }

    //#endregion


    //#region outputs

//    @OutputDirectory
//    public abstract DirectoryProperty getOutputDirectory();

    //#endregion


    //#region logic

    {
        setGroup("bytecode index");
    }

    @TaskAction
    @SneakyThrows
    @SuppressWarnings({"java:S3776", "java:S6541"})
    public final void execute() {
        var minVersion = getMinVersion().getOrNull();
        var maxVersion = getMaxVersion().getOrNull();
        var versions = streamAllVersions()
            .distinct()
            .filter(version -> minVersion == null || compareVersions(minVersion, version) <= 0)
            .filter(version -> maxVersion == null || compareVersions(version, maxVersion) <= 0)
            .sorted(this::compareVersions)
            .collect(toUnmodifiableList());


        var index = new Index<Version>();

        Map<String, Set<String>> prevAllPublicMembers = null;
        for (var version : versions) {
            getLogger().quiet("Version: {}", version);

            var allPublicMembers = new TreeMap<String, Set<String>>();
            forPaths(version, paths ->
                paths.forEach(path ->
                    getPublicMembers(path).forEach((classInternalName, publicMembers) -> {
                        var allClassPublicMembers = allPublicMembers.computeIfAbsent(
                            classInternalName,
                            __ -> new TreeSet<>()
                        );
                        allClassPublicMembers.addAll(publicMembers);
                    })
                )
            );

            var versionIndex = new VersionIndex();
            index.versions.put(version, versionIndex);

            if (prevAllPublicMembers == null) {
                versionIndex.added.putAll(allPublicMembers);

            } else {
                for (var publicMembersEntry : allPublicMembers.entrySet()) {
                    var classInternalName = publicMembersEntry.getKey();
                    var publicMembers = publicMembersEntry.getValue();
                    var prevPublicMembers = prevAllPublicMembers.get(classInternalName);
                    if (prevPublicMembers == null) {
                        versionIndex.added.put(classInternalName, publicMembers);
                    } else {
                        var addedPublicMembers = new LinkedHashSet<String>();
                        for (var member : publicMembers) {
                            if (!prevPublicMembers.contains(member)) {
                                addedPublicMembers.add(member);
                            }
                        }
                        if (!addedPublicMembers.isEmpty()) {
                            versionIndex.added.put(classInternalName, addedPublicMembers);
                        }
                    }
                }

                for (var prevPublicMembersEntry : prevAllPublicMembers.entrySet()) {
                    var classInternalName = prevPublicMembersEntry.getKey();
                    var prevPublicMembers = prevPublicMembersEntry.getValue();
                    var publicMembers = allPublicMembers.get(classInternalName);
                    if (publicMembers == null) {
                        versionIndex.removed.put(classInternalName, prevPublicMembers);
                    } else {
                        var removedPublicMembers = new LinkedHashSet<String>();
                        for (var member : prevPublicMembers) {
                            if (!publicMembers.contains(member)) {
                                removedPublicMembers.add(member);
                            }
                        }
                        if (!removedPublicMembers.isEmpty()) {
                            versionIndex.removed.put(classInternalName, removedPublicMembers);
                        }
                    }
                }
            }

            prevAllPublicMembers = allPublicMembers;
        }
    }

    @SneakyThrows
    @SuppressWarnings("UnusedMethod")
    private Map<String, Set<String>> getPublicMembers(Path path) {
        final BasicFileAttributes attrs;
        try {
            attrs = readAttributes(path, BasicFileAttributes.class);
        } catch (NoSuchFileException e) {
            return Map.of();
        }

        if (attrs.isDirectory()) {
            return getPublicMembersForDir(path);
        }

        var fileName = path.getFileName().toString();
        if (fileName.endsWith(".jar")) {
            return getPublicMembersForZip(path, null);
        } else if (fileName.endsWith(".jmod")) {
            return getPublicMembersForZip(path, "/classes/");
        } else {
            throw new IllegalStateException(format(
                "Unsupported classpath file path (doesn't end with `.jar` or `.jmod`): %s",
                path
            ));
        }
    }

    @SneakyThrows
    private Map<String, Set<String>> getPublicMembersForDir(Path path) {
        var result = new TreeMap<String, Set<String>>();
        try (var paths = walk(path)) {
            paths
                .filter(classPath -> classPath.getFileName().toString().endsWith(".class"))
                .filter(Files::isRegularFile)
                .forEach(classPath ->
                    getPublicMembersForClass(classPath).forEach((classInternalName, publicMembers) -> {
                        var allClassPublicMembers = result.computeIfAbsent(
                            classInternalName,
                            __ -> new TreeSet<>()
                        );
                        allClassPublicMembers.addAll(publicMembers);
                    })
                );
        }
        return result;
    }

    @SneakyThrows
    private Map<String, Set<String>> getPublicMembersForZip(Path path, @Nullable String namePrefix) {
        var fileSystemUri = format(
            "jar:%s!/",
            path.toUri()
        );
        var fileSystemEnv = Map.of(
            "releaseVersion", 9999,
            "multi-release", 9999
        );
        try (var fileSystem = newFileSystem(URI.create(fileSystemUri), fileSystemEnv)) {
            var result = new TreeMap<String, Set<String>>();
            try (var paths = walk(fileSystem.getPath("/"))) {
                paths
                    .filter(classPath ->
                        namePrefix == null || classPath.toString().startsWith(namePrefix)
                    )
                    .filter(classPath -> classPath.getFileName().toString().endsWith(".class"))
                    .filter(Files::isRegularFile)
                    .forEach(classPath ->
                        getPublicMembersForClass(classPath).forEach((classInternalName, publicMembers) -> {
                            var allPublicMembers = result.computeIfAbsent(
                                classInternalName,
                                __ -> new TreeSet<>()
                            );
                            allPublicMembers.addAll(publicMembers);
                        })
                    );
            }
            return result;
        }
    }

    @SneakyThrows
    private Map<String, Set<String>> getPublicMembersForClass(Path path) {
        try (var inputStream = newInputStream(path)) {
            var classReader = new ClassReader(inputStream);
            if (getIncludePublicClassesOnly().get()
                && ((classReader.getAccess() & ACC_PUBLIC) == 0)
            ) {
                return Map.of();
            }

            var members = new TreeSet<String>();
            var classVisitor = new ClassVisitor(LATEST_ASM_API) {
                @Nullable
                @Override
                public FieldVisitor visitField(
                    int access,
                    @Nonnull String name,
                    @Nonnull String descriptor,
                    @Nullable String signature,
                    @Nullable Object value
                ) {
                    members.add(name);
                    return null;
                }

                @Nullable
                @Override
                public MethodVisitor visitMethod(
                    int access,
                    @Nonnull String name,
                    @Nonnull String descriptor,
                    @Nullable String signature,
                    @Nullable String[] exceptions
                ) {
                    members.add(name + descriptor);
                    return null;
                }
            };
            classReader.accept(classVisitor, 0);

            if (members.isEmpty()) {
                return Map.of();
            }

            return Map.of(classReader.getClassName(), members);
        }
    }

    //#endregion


    //#region utils

    private static final Charset DEFAULT_CHARSET = UTF_8;

    @Nullable
    protected static Object loadJsonUrlContent(URL url, Charset charset) {
        var content = loadTextUrlContent(url, charset);
        var slurper = new JsonSlurper();
        return slurper.parseText(content);
    }

    @Nullable
    protected static Object loadJsonUrlContent(URL url) {
        return loadJsonUrlContent(url, DEFAULT_CHARSET);
    }


    protected static String loadTextUrlContent(URL url, Charset charset) {
        var content = loadBinaryUrlContent(url);
        return new String(content, charset);
    }

    protected static String loadTextUrlContent(URL url) {
        return loadTextUrlContent(url, DEFAULT_CHARSET);
    }


    @SneakyThrows
    protected static byte[] loadBinaryUrlContent(URL url) {
        var connection = url.openConnection();
        connection.setConnectTimeout(5_000);
        connection.setReadTimeout(30_000);
        connection.setUseCaches(false);
        try {
            if (connection instanceof HttpURLConnection) {
                var responseCode = ((HttpURLConnection) connection).getResponseCode();
                if (responseCode != 200) {
                    throw new IllegalStateException(format(
                        "Could not %s '%s'. Received status code %d from server.",
                        "GET",
                        url,
                        responseCode
                    ));
                }
            }

            try (var in = connection.getInputStream()) {
                return in.readAllBytes();
            }

        } finally {
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
        }
    }

    private static final Pattern ASM_API_FIELD_NAME = Pattern.compile("^ASM(\\d+)$");

    private static final int LATEST_ASM_API;

    static {
        var latestAsmApiField = stream(Opcodes.class.getFields())
            .filter(field -> !field.isSynthetic())
            .filter(field -> isStatic(field.getModifiers()))
            .filter(field ->
                field.getType() == int.class
                    && ASM_API_FIELD_NAME.matcher(field.getName()).matches()
            )
            .max(comparingInt(field -> {
                var matcher = ASM_API_FIELD_NAME.matcher(field.getName());
                if (!matcher.matches()) {
                    throw new AssertionError("unreachable");
                }

                var apiStr = matcher.group(1);
                var apiVersion = parseInt(apiStr);
                return apiVersion;
            }))
            .orElseThrow(() -> new IllegalStateException("Latest ASM API field not found"));

        try {
            LATEST_ASM_API = latestAsmApiField.getInt(null);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    //#endregion

}
