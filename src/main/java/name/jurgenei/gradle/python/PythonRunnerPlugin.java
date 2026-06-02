package name.jurgenei.gradle.python;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Gradle plugin entry point that registers the {@code PythonRunnerTask} task.
 *
 * <p>The task executes Python scripts in an isolated virtual environment and can
 * install dependencies from a requirements file with hash-based change detection.</p>
 */
public class PythonRunnerPlugin implements Plugin<Project> {

    /**
     * Creates the plugin instance.
     */
    public PythonRunnerPlugin() {
    }

    /**
     * Registers the {@code PythonRunnerTask} task in the target project.
     *
     * @param project Gradle project where the task is registered
     */
    @Override
    public void apply(Project project) {
        project.getTasks().register("PythonRunnerTask", PythonRunnerTask.class, task -> {
            task.setGroup("Python");
            task.setDescription("Run a Python script using a cached virtual environment.");
        });
    }
}

