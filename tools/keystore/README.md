# Shared beta signing key

tune-beta.keystore signs all GitHub beta/official APKs so updates install cleanly over each other.

- Alias: tune-beta / passwords: android (public beta key, NOT a secret)
- Generated once on GitHub Actions, committed here so every build uses it.
- If Tune ever ships on the Play Store, replace with a properly secured release key.
