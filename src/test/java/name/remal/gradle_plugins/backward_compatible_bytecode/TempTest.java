package name.remal.gradle_plugins.backward_compatible_bytecode;

import static org.objectweb.asm.Type.getMethodDescriptor;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class TempTest {

    @Test
    @SuppressWarnings("java:S2699")
    void temp() throws Throwable {
        var method = Stream.class.getMethod("toArray");
        System.out.println(getMethodDescriptor(method));
    }

}
