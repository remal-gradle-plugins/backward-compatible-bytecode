package name.remal.gradle_plugins.backward_compatible_bytecode.impl.compatibility.jdk.handler;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import com.google.auto.service.AutoService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import name.remal.gradle_plugins.backward_compatible_bytecode.api.compatibility.CompatibilityHandlerContext;
import name.remal.gradle_plugins.backward_compatible_bytecode.api.compatibility.CompatibilityImplementation;
import name.remal.gradle_plugins.backward_compatible_bytecode.api.compatibility.jdk.JdkCompatibilityHandler;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

@AutoService(JdkCompatibilityHandler.class)
@SuppressWarnings("checkstyle:TypeName")
public class java_util_stream_Stream_16 implements JdkCompatibilityHandler {

    @Override
    public int[] getMinCompatibleVersion() {
        return new int[]{16};
    }

    @Override
    @SneakyThrows
    public void handle(AbstractInsnNode insn, CompatibilityHandlerContext context) {
        if (!(insn instanceof MethodInsnNode)) {
            return;
        }

        var methodInsnNode = (MethodInsnNode) insn;
        if (!methodInsnNode.owner.equals("java/util/stream/Stream")) {
            return;
        }

        if (methodInsnNode.name.equals("toList")
            && methodInsnNode.desc.equals("()Ljava/util/List;")
        ) {
            context.replace(methodInsnNode, Implementation.class.getMethod(
                "toList",
                Stream.class
            ));

        } else if (methodInsnNode.name.equals("mapMulti")
            && methodInsnNode.desc.equals("(Ljava/util/function/BiConsumer;)Ljava/util/stream/Stream;")
        ) {
            context.replace(methodInsnNode, Implementation.class.getMethod(
                "mapMulti",
                Stream.class,
                BiConsumer.class
            ));

        } else if (methodInsnNode.name.equals("mapMultiToInt")
            && methodInsnNode.desc.equals("(Ljava/util/function/BiConsumer;)Ljava/util/stream/IntStream;")
        ) {
            context.replace(methodInsnNode, Implementation.class.getMethod(
                "mapMultiToInt",
                Stream.class,
                BiConsumer.class
            ));

        } else if (methodInsnNode.name.equals("mapMultiToLong")
            && methodInsnNode.desc.equals("(Ljava/util/function/BiConsumer;)Ljava/util/stream/LongStream;")
        ) {
            context.replace(methodInsnNode, Implementation.class.getMethod(
                "mapMultiToLong",
                Stream.class,
                BiConsumer.class
            ));

        } else if (methodInsnNode.name.equals("mapMultiToDouble")
            && methodInsnNode.desc.equals("(Ljava/util/function/BiConsumer;)Ljava/util/stream/DoubleStream;")
        ) {
            context.replace(methodInsnNode, Implementation.class.getMethod(
                "mapMultiToDouble",
                Stream.class,
                BiConsumer.class
            ));
        }
    }

    @SuppressWarnings({"rawtypes", "Java9CollectionFactory", "unchecked"})
    private static class Implementation implements CompatibilityImplementation {

        public static List toList(Stream stream) {
            return unmodifiableList(new ArrayList<>(asList(stream.toArray())));
        }

        public static Stream mapMulti(Stream stream, BiConsumer<Object, Consumer> mapper) {
            return stream.flatMap(element -> {
                var list = new ArrayList<>();
                mapper.accept(element, list::add);
                return list.stream();
            });
        }

        public static IntStream mapMultiToInt(Stream stream, BiConsumer<Object, IntConsumer> mapper) {
            return stream.flatMap(element -> {
                var list = new ArrayList<>();
                mapper.accept(element, list::add);
                return list.stream();
            }).mapToInt(Integer.class::cast);
        }

        public static LongStream mapMultiToLong(Stream stream, BiConsumer<Object, LongConsumer> mapper) {
            return stream.flatMap(element -> {
                var list = new ArrayList<>();
                mapper.accept(element, list::add);
                return list.stream();
            }).mapToLong(Long.class::cast);
        }

        public static DoubleStream mapMultiToDouble(Stream stream, BiConsumer<Object, DoubleConsumer> mapper) {
            return stream.flatMap(element -> {
                var list = new ArrayList<>();
                mapper.accept(element, list::add);
                return list.stream();
            }).mapToDouble(Long.class::cast);
        }

    }

}
