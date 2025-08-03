package name.remal.gradle_plugins.backward_compatible_bytecode;

import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.compile.AbstractCompile;

public abstract class BackwardCompatibleBytecodePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        var processingAction = getObjects().newInstance(BackwardCompatibleBytecodeAction.class);

        project.getTasks().withType(AbstractCompile.class).configureEach(task -> {
            task.doLast(processingAction);
        });
    }


    @Inject
    protected abstract ObjectFactory getObjects();

}
