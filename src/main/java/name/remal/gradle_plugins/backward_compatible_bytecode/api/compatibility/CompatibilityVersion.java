package name.remal.gradle_plugins.backward_compatible_bytecode.api.compatibility;

public interface CompatibilityVersion<VersionImpl> {

    VersionImpl parseVersion(int[] numbers);

    VersionImpl getCurrentVersion();

    int compareVersions(VersionImpl version1, VersionImpl version2);

}
