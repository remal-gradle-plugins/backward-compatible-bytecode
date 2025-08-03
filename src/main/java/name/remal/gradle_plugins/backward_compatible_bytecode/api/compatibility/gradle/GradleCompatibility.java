package name.remal.gradle_plugins.backward_compatible_bytecode.api.compatibility.gradle;

import com.google.auto.service.AutoService;
import name.remal.gradle_plugins.backward_compatible_bytecode.api.compatibility.Compatibility;

@AutoService(Compatibility.class)
@SuppressWarnings({"rawtypes", "RedundantSuppression"})
public class GradleCompatibility implements Compatibility<GradleCompatibilityVersion, GradleCompatibilityHandler> {

    @Override
    public String getName() {
        return "gradle";
    }

}
