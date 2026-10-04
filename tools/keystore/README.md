# Signing keys

`tools/keystore/tune-beta.keystore` is the legacy shared beta key used by earlier releases. The current `pre-release-apks` workflow no longer uses it: it generates a fresh ephemeral RSA keystore on the GitHub Actions runner for each beta build, so the new APK has a completely different signing certificate.

- Legacy alias: `tune-beta` / passwords: `android` (public legacy key, not a secret)
- Fresh beta keys are generated in the cloud and discarded with the runner.
- For an official Play Store release, use a properly secured long-lived release key.