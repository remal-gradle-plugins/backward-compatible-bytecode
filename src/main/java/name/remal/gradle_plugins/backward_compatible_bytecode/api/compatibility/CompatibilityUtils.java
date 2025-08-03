package name.remal.gradle_plugins.backward_compatible_bytecode.api.compatibility;

import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.ParameterizedType;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
abstract class CompatibilityUtils {

    public static <S, C extends S, A> Class<A> getSuperTypeArgumentClass(
        Class<C> clazz,
        Class<S> superClass,
        int argumentIndex
    ) {
        var superType = TypeToken.of(clazz).getSupertype(superClass);
        if (!(superType instanceof ParameterizedType)) {
            throw new IllegalStateException(format(
                "Not a parameterized class: %s",
                clazz
            ));
        }

        var argumentType = ((ParameterizedType) superType).getActualTypeArguments()[argumentIndex];
        @SuppressWarnings("unchecked")
        var argumentClass = (Class<A>) TypeToken.of(argumentType).getRawType();
        return argumentClass;
    }

}
