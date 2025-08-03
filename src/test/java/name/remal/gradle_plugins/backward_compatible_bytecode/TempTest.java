package name.remal.gradle_plugins.backward_compatible_bytecode;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

class TempTest {

    @Test
    void temp() throws Throwable {
        var method = Stream.class.getMethod("toArray");
        System.out.println(Type.getMethodDescriptor(method));
    }

}
