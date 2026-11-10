# Versioning Strategy

This project uses **static versioning via `gradle.properties`** as the single source of truth for both local development and CI/CD distribution pipelines.

---

## Single Source of Truth

Version codes and version names are declared strictly in `gradle.properties`:

```properties
# Application version properties
APP_VERSION_CODE=226
APP_VERSION_NAME=1.4.0
```

`app/build.gradle.kts` consumes these properties directly:
```kotlin
defaultConfig {
    versionCode = project.property("APP_VERSION_CODE").toString().toInt()
    versionName = project.property("APP_VERSION_NAME").toString()
}
```

---

## Release Workflow

When publishing a new release:
1. Update `APP_VERSION_CODE` and `APP_VERSION_NAME` in `gradle.properties`.
2. Commit the bump: `git commit -am "chore: bump version to X.Y.Z"`.
3. Tag the release: `git tag -a vX.Y.Z -m "Release vX.Y.Z"`.
4. Push: `git push origin master --tags`.

---

## Distribution Alignment

- **GitHub Actions (`deploy.yml`):** Reads `APP_VERSION_CODE` and `APP_VERSION_NAME` from `gradle.properties` during checkout. No CLI overrides are applied.
- **Fastlane (`fastlane/metadata/android/en-US/changelogs/`):** Release notes are supplied in `changelogs/<versionCode>.txt`.
