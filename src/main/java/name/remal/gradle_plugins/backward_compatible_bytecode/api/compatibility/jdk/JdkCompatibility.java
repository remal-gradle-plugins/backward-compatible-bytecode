package name.remal.gradle_plugins.backward_compatible_bytecode.api.compatibility.jdk;

import com.google.auto.service.AutoService;
import java.util.List;
import name.remal.gradle_plugins.backward_compatible_bytecode.api.compatibility.Compatibility;

@AutoService(Compatibility.class)
@SuppressWarnings({"rawtypes", "RedundantSuppression"})
public class JdkCompatibility implements Compatibility<JdkCompatibilityVersion, JdkCompatibilityHandler> {

    @Override
    public String getName() {
        return "jdk";
    }

    @Override
    public List<String> getAliases() {
        return List.of("java", "jre");
    }

}
