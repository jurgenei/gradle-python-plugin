# Gradle Python Plugin

![Conformance](https://img.shields.io/badge/Conformance-Check--All%20Passing-brightgreen)

[![Test](https://github.com/jurgenei/gradle-python-plugin/actions/workflows/test.yml/badge.svg)](https://github.com/jurgenei/gradle-python-plugin/actions/workflows/test.yml)
[![Coverage CI](https://github.com/jurgenei/gradle-python-plugin/actions/workflows/coverage.yml/badge.svg)](https://github.com/jurgenei/gradle-python-plugin/actions/workflows/coverage.yml)
[![Coverage](https://codecov.io/gh/jurgenei/gradle-python-plugin/branch/main/graph/badge.svg)](https://codecov.io/gh/jurgenei/gradle-python-plugin)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Run Python scripts from Gradle with isolated virtual environments and optional dependency installation.

## Highlights

- Creates and reuses a local `.venv` in your task working directory
- Installs `requirements.txt` only when file content changes (SHA-256 hash cache)
- Runs scripts with arguments and surfaces stdout/stderr in Gradle logs
- Provides a single task name: `PythonRunnerTask`

## Plugin Coordinates

- Plugin ID: `name.jurgenei.gradle.python`
- Java package: `name.jurgenei.gradle.python`
- Registered task: `PythonRunnerTask`

## Requirements

- JDK 17+ (tested with recent Gradle versions)
- Python 3 available on the build machine
- Gradle (or the provided wrapper)

## Quick Start

```groovy
plugins {
    id 'name.jurgenei.gradle.python'
}

PythonRunnerTask {
    script = file("scripts/run.py")
    workDir = projectDir
    requirements = file("requirements.txt") // optional
    args = ["foo", "bar"]                  // optional
    pythonExecutable = "/usr/bin/python3"   // optional
}
```

Then run:

```bash
./gradlew PythonRunnerTask
```

## Task Properties

| Property | Type | Required | Description |
| --- | --- | --- | --- |
| `script` | `File` | Yes | Python script to execute. |
| `workDir` | `File` | No | Working directory and location for `.venv` (defaults to script parent, then project dir). |
| `requirements` | `File` | No | `requirements.txt` file to install; re-installs only when content hash changes. |
| `args` | `List<String>` | No | Positional arguments passed to the script. |
| `pythonExecutable` | `String` | No | Python interpreter used to create the virtual environment. |

## How Caching Works

When `requirements` is set, the plugin computes a SHA-256 hash of the file and stores it at `.venv/.requirements.hash`. If the hash is unchanged, dependency installation is skipped.

## Development

Run tests locally:

```bash
./gradlew test
```

## CI

GitHub Actions is configured to run tests on every push (any branch) and pull request.

## Contributing

1. Create a branch from `main`.
2. Run `./gradlew test` before opening a PR.
3. Submit a PR with a short change summary.

## License

This project is licensed under the terms of the `LICENSE` file.
