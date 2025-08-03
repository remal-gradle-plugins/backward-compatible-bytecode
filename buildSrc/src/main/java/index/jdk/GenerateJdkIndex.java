package index.jdk;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.nio.file.FileSystems.newFileSystem;
import static java.nio.file.Files.list;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableList;

import index.GenerateIndex;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.SneakyThrows;
import org.gradle.api.Action;
import org.gradle.jvm.toolchain.JavaCompiler;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;

public abstract class GenerateJdkIndex extends GenerateIndex<Integer, JavaCompiler> {

    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    protected Stream<Integer> streamAllVersions() {
        var response = (Map<String, Object>) loadJsonUrlContent(new URL(
            "https://api.foojay.io/disco/v3.0/major_versions"
                + "?ea=false"
                + "&ga=true"
                + "&discovery_scope_id=public"
                + "&include_versions=false"
        ));

        var versions = (Collection<Map<String, Object>>) requireNonNull(response).get("result");
        return versions.stream()
            .filter(version -> !parseBoolean(version.getOrDefault("early_access_only", "").toString()))
            .filter(version -> !"ea".equals(version.getOrDefault("release_status", "").toString()))
            .map(version -> version.get("major_version").toString())
            .map(Integer::parseInt);
    }

    @Override
    protected int compareVersions(Integer version1, Integer version2) {
        return Integer.compare(version1, version2);
    }

    @Override
    @SneakyThrows
    protected void forPaths(Integer version, Action<Collection<Path>> action) {
        var compiler = getJavaToolchainService().compilerFor(spec -> {
            spec.getLanguageVersion().set(JavaLanguageVersion.of(version));
        }).get();

        var jdkInstallationPath = compiler.getMetadata().getInstallationPath().getAsFile().toPath().toAbsolutePath();
        var fileSystemEnv = Map.of("java.home", jdkInstallationPath.toString());
        try (var fileSystem = newFileSystem(URI.create("jrt:/"), fileSystemEnv)) {
            var modulesPath = fileSystem.getPath("/modules");
            try (var stream = list(modulesPath)) {
                var paths = stream.collect(toUnmodifiableList());
                if (paths.isEmpty()) {
                    throw new IllegalStateException(format(
                        "Unsupported JVM installation dir (no modules found): %s",
                        jdkInstallationPath
                    ));
                }

                action.execute(paths);
            }
        }
    }


    @Inject
    protected abstract JavaToolchainService getJavaToolchainService();

}
