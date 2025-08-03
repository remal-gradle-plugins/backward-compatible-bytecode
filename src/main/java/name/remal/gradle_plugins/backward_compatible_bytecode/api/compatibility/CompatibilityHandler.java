package name.remal.gradle_plugins.backward_compatible_bytecode.api.compatibility;

import org.objectweb.asm.tree.AbstractInsnNode;

public interface CompatibilityHandler {

    int[] getMinCompatibleVersion();

    void handle(AbstractInsnNode insn, CompatibilityHandlerContext context);

}
