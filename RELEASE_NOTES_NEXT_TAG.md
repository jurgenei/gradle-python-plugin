
# Release Notes (Next Tag)

Date: 2026-05-29
Scope: `gradle-python-plugin` release notes sync for `0.1.1`

## Highlights

- Version line remains on `0.1.1` for the release branch.
- Repository metadata and quality/security configuration remain aligned.

## Notes

- This release-note update is a repository-level sync entry for the release branch.
- No additional release-note-only behavioral deltas are introduced in this file.

## Quality Automation

- Added baseline `qodana.yaml` for JVM community linting on JDK 21.
- Added `.github/workflows/qodana_code_quality.yml` with `main`/`release/**` trigger parity.
- Qodana workflow uses read-only permissions and publishes scan results without auto-fixes.

