package name.remal.gradle_plugins.backward_compatible_bytecode.api.compatibility;

import static java.lang.String.format;
import static name.remal.gradle_plugins.backward_compatible_bytecode.api.compatibility.CompatibilityUtils.getSuperTypeArgumentClass;

import java.util.List;

public interface Compatibility<Version extends CompatibilityVersion<?>, Handler extends CompatibilityHandler> {

    String getName();

    default List<String> getAliases() {
        return List.of();
    }

    default Class<Version> getVersionClass() {
        Class<?> versionClass = getSuperTypeArgumentClass(getClass(), Compatibility.class, 0);
        if (versionClass == CompatibilityVersion.class) {
            throw new IllegalStateException(format(
                "Generic %s can't be used",
                CompatibilityHandler.class.getSimpleName()
            ));
        }
        @SuppressWarnings("unchecked")
        var typedVersionClass = (Class<Version>) versionClass;
        return typedVersionClass;
    }

    default Class<Handler> getHandlerClass() {
        Class<?> handlerClass = getSuperTypeArgumentClass(getClass(), Compatibility.class, 1);
        if (handlerClass == CompatibilityHandler.class) {
            throw new IllegalStateException(format(
                "Generic %s can't be used",
                CompatibilityHandler.class.getSimpleName()
            ));
        }
        @SuppressWarnings("unchecked")
        var typedHandlerClass = (Class<Handler>) handlerClass;
        return typedHandlerClass;
    }

}
