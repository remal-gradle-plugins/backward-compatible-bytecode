package name.remal.gradle_plugins.backward_compatible_bytecode.api.compatibility.jdk;

import name.remal.gradle_plugins.backward_compatible_bytecode.api.compatibility.CompatibilityVersion;

public class JdkCompatibilityVersion implements CompatibilityVersion<Integer> {

    @Override
    public Integer parseVersion(int[] numbers) {
        if (numbers.length != 1) {
            throw new AssertionError("Only major Java versions are supported");
        }

        return numbers[0];
    }

    @Override
    public Integer getCurrentVersion() {
        return Runtime.version().feature();
    }

    @Override
    public int compareVersions(Integer version1, Integer version2) {
        return Integer.compare(version1, version2);
    }

}
