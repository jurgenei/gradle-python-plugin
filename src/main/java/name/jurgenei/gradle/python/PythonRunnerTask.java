package name.jurgenei.gradle.python;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Gradle task that executes a Python script from a project directory.
 *
 * <p>Execution model:</p>
 * <ul>
 *   <li>Creates a virtual environment under {@code .venv} if it does not exist.</li>
 *   <li>Optionally installs dependencies from {@code requirements.txt}.</li>
 *   <li>Skips dependency installation when the requirements file hash is unchanged.</li>
 *   <li>Runs the configured script and fails the task on non-zero exit code.</li>
 * </ul>
 */
public class PythonRunnerTask extends DefaultTask {

    private File workDir;
    private File script;
    private File requirements;
    private List<String> args = new ArrayList<>();
    private String pythonExecutable = "/usr/bin/python3";

    /**
     * Task entry point that prepares the environment and executes the script.
     *
     * @throws Exception if environment setup or script execution fails
     */
    @TaskAction
    public void runPython() throws Exception {
        if (script == null || !script.exists()) {
            throw new GradleException("'script' must point to an existing python file.");
        }

        File effectiveWorkDir = resolveWorkDir();
        File venvDir = new File(effectiveWorkDir, ".venv");
        File marker = new File(venvDir, ".requirements.hash");

        setupVenv(effectiveWorkDir, venvDir);
        installRequirements(effectiveWorkDir, marker);

        Result result = runScript(effectiveWorkDir, venvDir);
        if (!result.stdout().isBlank()) {
            getLogger().lifecycle(result.stdout().trim());
        }
        if (!result.stderr().isBlank()) {
            getLogger().error(result.stderr().trim());
        }

        if (result.exitCode() != 0) {
            String details = result.stderr().isBlank() ? "" : ": " + result.stderr().trim();
            throw new GradleException("Python script failed with exit code " + result.exitCode() + details);
        }
    }

    private File resolveWorkDir() {
        if (workDir != null) {
            workDir.mkdirs();
            return workDir;
        }
        File parent = script.getParentFile();
        if (parent != null) {
            parent.mkdirs();
            return parent;
        }
        return getProject().getProjectDir();
    }

    private void setupVenv(File effectiveWorkDir, File venvDir) throws Exception {
        if (venvDir.exists()) {
            return;
        }
        runCommand(effectiveWorkDir, List.of(pythonExecutable, "-m", "venv", venvDir.getAbsolutePath()));
    }

    private void installRequirements(File effectiveWorkDir, File marker) throws Exception {
        if (requirements == null || !requirements.exists()) {
            return;
        }

        String hash = hash(requirements);
        if (marker.exists()) {
            String current = Files.readString(marker.toPath());
            if (Objects.equals(current, hash)) {
                return;
            }
        }

        runCommand(effectiveWorkDir, List.of(
                pythonBin(effectiveWorkDir), "-m", "pip", "install", "-r", requirements.getAbsolutePath()
        ));
        Files.writeString(marker.toPath(), hash);
    }

    private Result runScript(File effectiveWorkDir, File venvDir) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(new File(venvDir, "bin/python").getAbsolutePath());
        cmd.add(script.getAbsolutePath());
        cmd.addAll(args);

        getLogger().info("run: " + String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(effectiveWorkDir);

        Process process = pb.start();
        ProcessOutput output = readProcessOutput(process);
        int exitCode = process.waitFor();

        return new Result(exitCode, output.stdout(), output.stderr());
    }

    private void runCommand(File effectiveWorkDir, List<String> cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(effectiveWorkDir);

        Process process = pb.start();
        ProcessOutput output = readProcessOutput(process);
        int exitCode = process.waitFor();

        output.stdout().lines().forEach(line -> getLogger().info("[python] " + line));
        output.stderr().lines().forEach(line -> getLogger().error("[python] " + line));

        if (exitCode != 0) {
            throw new GradleException("Command failed: " + String.join(" ", cmd));
        }
    }

    private ProcessOutput readProcessOutput(Process process) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> stdoutFuture = executor.submit(streamReader(process.getInputStream()));
            Future<String> stderrFuture = executor.submit(streamReader(process.getErrorStream()));

            String stdout = stdoutFuture.get();
            String stderr = stderrFuture.get();
            return new ProcessOutput(stdout, stderr);
        } catch (ExecutionException e) {
            throw new GradleException("Failed to read process output", e.getCause());
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<String> streamReader(InputStream stream) {
        return () -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
                return output.toString();
            }
        };
    }

    private String pythonBin(File effectiveWorkDir) {
        return new File(new File(effectiveWorkDir, ".venv"), "bin/python").getAbsolutePath();
    }

    private String hash(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(Files.readAllBytes(file.toPath()));
        return Base64.getEncoder().encodeToString(digest);
    }

    /**
     * Returns the script file to execute.
     *
     * @return configured Python script
     */
    @InputFile
    public File getScript() {
        return script;
    }

    /**
     * Sets the script file to execute.
     *
     * @param script Python script path
     */
    public void setScript(File script) {
        this.script = script;
    }

    /**
     * Returns the requirements file used for dependency installation.
     *
     * @return requirements file or {@code null}
     */
    @Optional
    @InputFile
    public File getRequirements() {
        return requirements;
    }

    /**
     * Sets the requirements file for dependency installation.
     *
     * @param requirements requirements file path
     */
    public void setRequirements(File requirements) {
        this.requirements = requirements;
    }

    /**
     * Returns the working directory used for command execution and venv storage.
     *
     * @return working directory or {@code null} when default resolution is used
     */
    @Optional
    @Input
    public File getWorkDir() {
        return workDir;
    }

    /**
     * Sets the working directory used for command execution and venv storage.
     *
     * @param workDir task working directory
     */
    public void setWorkDir(File workDir) {
        this.workDir = workDir;
    }

    /**
     * Returns positional arguments passed to the script.
     *
     * @return script arguments
     */
    @Optional
    @Input
    public List<String> getArgs() {
        return args;
    }

    /**
     * Sets positional arguments passed to the script.
     *
     * @param args script arguments; {@code null} is treated as empty
     */
    public void setArgs(List<String> args) {
        this.args = args == null ? new ArrayList<>() : new ArrayList<>(args);
    }

    /**
     * Returns the Python executable used to create the virtual environment.
     *
     * @return Python executable path or command
     */
    @Input
    public String getPythonExecutable() {
        return pythonExecutable;
    }

    /**
     * Sets the Python executable used to create the virtual environment.
     *
     * @param pythonExecutable executable path or command
     */
    public void setPythonExecutable(String pythonExecutable) {
        this.pythonExecutable = pythonExecutable;
    }

    /**
     * Process result for a script execution.
     *
     * @param exitCode process exit code
     * @param stdout captured standard output
     * @param stderr captured standard error
     */
    public record Result(int exitCode, String stdout, String stderr) {
    }

    private record ProcessOutput(String stdout, String stderr) {
    }
}
