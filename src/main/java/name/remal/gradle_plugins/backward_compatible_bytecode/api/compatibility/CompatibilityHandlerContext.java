package name.remal.gradle_plugins.backward_compatible_bytecode.api.compatibility;

import java.lang.reflect.Method;
import org.objectweb.asm.tree.AbstractInsnNode;

public interface CompatibilityHandlerContext {

    void replace(AbstractInsnNode instructionToReplace, Method newInstructions);

}
