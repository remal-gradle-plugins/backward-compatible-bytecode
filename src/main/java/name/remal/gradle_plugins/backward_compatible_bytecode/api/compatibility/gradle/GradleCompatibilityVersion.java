package name.remal.gradle_plugins.backward_compatible_bytecode.api.compatibility.gradle;

import static java.lang.Math.max;

import name.remal.gradle_plugins.backward_compatible_bytecode.api.compatibility.CompatibilityVersion;
import org.gradle.util.GradleVersion;

public class GradleCompatibilityVersion implements CompatibilityVersion<GradleVersion> {

    @Override
    public GradleVersion parseVersion(int[] numbers) {
        var version = new StringBuilder();
        for (var i = 0; i < max(numbers.length, 2); i++) {
            if (i > 0) {
                version.append('.');
            }

            if (i < numbers.length) {
                version.append(numbers[i]);
            } else {
                version.append('0');
            }
        }

        return GradleVersion.version(version.toString());
    }

    @Override
    public GradleVersion getCurrentVersion() {
        return GradleVersion.current().getBaseVersion();
    }

    @Override
    public int compareVersions(GradleVersion version1, GradleVersion version2) {
        return version1.compareTo(version2);
    }

}
