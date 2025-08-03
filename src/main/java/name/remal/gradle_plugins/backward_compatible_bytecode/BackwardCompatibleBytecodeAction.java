package name.remal.gradle_plugins.backward_compatible_bytecode;

import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.gradle.api.Action;
import org.gradle.api.Describable;
import org.gradle.api.Task;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.compile.AbstractCompile;

@RequiredArgsConstructor
abstract class BackwardCompatibleBytecodeAction implements Action<Task>, Describable {

    @Override
    public void execute(Task untypedTask) {
        var task = (AbstractCompile) untypedTask;
        var destinationDir = task.getDestinationDirectory().getAsFile().getOrNull();
        if (destinationDir == null) {
            return;
        }

        var fileTree = getObjects().fileTree().from(destinationDir);
        fileTree.include("**/*.class");
        fileTree.visit(details -> {
            if (!details.isDirectory()) {
                var path = details.getFile().toPath();
            }
        });
    }

    @Override
    public String getDisplayName() {
        return BackwardCompatibleBytecodeAction.class.getName();
    }


    @Inject
    protected abstract ObjectFactory getObjects();

}
