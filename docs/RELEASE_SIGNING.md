# Release Signing With GitHub Actions

This project can build a signed release APK in GitHub Actions without committing the keystore. The workflow lives at `.github/workflows/android.yml`.

## 1. Keep Signing Files Private

Never commit these files:

- `*.jks`
- `*.keystore`
- `*_base64.txt`
- `keystore.properties`
- passwords, aliases, tokens, or secret notes

They are ignored by `.gitignore`.

## 2. Generate The Base64 Secret File

From the project root on Windows:

```powershell
.\scripts\export-keystore-base64.ps1 -KeystorePath .\smartIsland.jks
```

This creates:

```text
smartIsland_base64.txt
```

Open that file locally and copy the full single-line content into a GitHub repository secret named:

```text
ANDROID_KEYSTORE_BASE64
```

Do not commit `smartIsland_base64.txt`.

## 3. Add GitHub Repository Secrets

In GitHub, open:

```text
Repository -> Settings -> Secrets and variables -> Actions -> New repository secret
```

Add these secrets:

| Secret name | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Full content of `smartIsland_base64.txt` |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore/store password |

The workflow detects the key alias from `smartIsland.jks` automatically and uses `ANDROID_KEYSTORE_PASSWORD` as the key password.

## 4. Build A Release

Manual release build:

1. Go to the repository on GitHub.
2. Open `Actions`.
3. Select `Build Android APK`.
4. Click `Run workflow`.

Tag release:

```bash
git tag v3.2
git push origin v3.2
```

When a tag starting with `v` is pushed, the workflow builds signed artifacts and publishes them to a GitHub release. Pushes to `main` also publish/update a release using the app `versionName`; pushes to `dev` build a debug APK artifact only.

## What Is `RUNNER_TEMP`?

`RUNNER_TEMP` is a GitHub Actions temporary folder path that exists only during a workflow run. The workflow decodes your `ANDROID_KEYSTORE_BASE64` secret into:

```text
$RUNNER_TEMP/smartIsland.jks
```

That file is created inside GitHub's temporary runner environment, used for signing, and then discarded after the job finishes. It does not need to exist in your repository.

## 5. Outputs

The workflow uploads one signed release APK:

```text
SmartIsland-v3.0.apk
```

The version part comes from `versionName` in `app/build.gradle.kts`.

Artifacts are also available from the workflow run summary.

## Important Backup Note

Keep multiple secure backups of your keystore and passwords. If you lose the signing key, you may not be able to publish updates signed with the same identity.
