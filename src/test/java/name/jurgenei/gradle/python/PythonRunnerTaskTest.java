package name.jurgenei.gradle.python;

import org.gradle.api.Project;
import org.gradle.api.GradleException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PythonRunnerTaskTest {

    private final String pythonExecutable = System.getenv().getOrDefault("PYTHON_EXECUTABLE", "python3");

    @Test
    void should_register_python_runner_task_from_plugin() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply(PythonRunnerPlugin.class);

        assertNotNull(project.getTasks().findByName("PythonRunnerTask"));
    }

    @Test
    void should_run_python_script() throws Exception {
        File dir = Files.createTempDirectory("py-basic").toFile();

        write(dir, "script.py", "from pathlib import Path\nPath('out.txt').write_text('Hello from Python')\n");

        PythonRunnerTask task = newTask(dir);
        task.setScript(new File(dir, "script.py"));

        task.runPython();

        String out = Files.readString(new File(dir, "out.txt").toPath());
        assertEquals("Hello from Python", out);
    }

    @Test
    void should_run_python_script_with_args() throws Exception {
        File dir = Files.createTempDirectory("py-args").toFile();

        write(dir, "script.py", "import sys\nfrom pathlib import Path\nPath('args.txt').write_text(sys.argv[1] + ',' + sys.argv[2])\n");

        PythonRunnerTask task = newTask(dir);
        task.setScript(new File(dir, "script.py"));
        task.setArgs(List.of("foo", "bar"));

        task.runPython();

        String argsText = Files.readString(new File(dir, "args.txt").toPath());
        assertEquals("foo,bar", argsText);
    }

    @Test
    void should_capture_python_errors() throws Exception {
        File dir = Files.createTempDirectory("py-fail").toFile();

        write(dir, "script.py", "raise RuntimeError('boom')\n");

        PythonRunnerTask task = newTask(dir);
        task.setScript(new File(dir, "script.py"));

        GradleException ex = assertThrows(GradleException.class, task::runPython);
        assertTrue(ex.getMessage().contains("boom"));
    }

    @Test
    void should_reuse_existing_venv() throws Exception {
        File dir = Files.createTempDirectory("py-cache").toFile();

        write(dir, "script.py", "print('reuse test')\n");

        PythonRunnerTask task = newTask(dir);
        task.setScript(new File(dir, "script.py"));

        task.runPython();

        File venv = new File(dir, ".venv");
        assertTrue(venv.exists());
        long lastModified = venv.lastModified();

        task.runPython();

        assertEquals(lastModified, venv.lastModified());
    }

    @Test
    void should_skip_install_if_requirements_unchanged() throws Exception {
        File dir = Files.createTempDirectory("py-req-cache").toFile();

        File req = new File(dir, "requirements.txt");

        write(dir, "requirements.txt", "# intentionally empty\n");
        write(dir, "script.py", "print('ok')\n");

        PythonRunnerTask task = newTask(dir);
        task.setScript(new File(dir, "script.py"));
        task.setRequirements(req);

        task.runPython();

        File marker = new File(dir, ".venv/.requirements.hash");
        assertTrue(marker.exists());

        String firstHash = Files.readString(marker.toPath());

        task.runPython();

        String secondHash = Files.readString(marker.toPath());
        assertEquals(firstHash, secondHash);
    }

    private PythonRunnerTask newTask(File dir) {
        Project project = ProjectBuilder.builder().withProjectDir(dir).build();
        PythonRunnerTask task = project.getTasks().create("pythonRunner", PythonRunnerTask.class);
        task.setWorkDir(dir);
        task.setPythonExecutable(pythonExecutable);
        return task;
    }

    private void write(File dir, String name, String content) throws Exception {
        Files.writeString(new File(dir, name).toPath(), content);
    }
}
