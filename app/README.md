Firebase config file

This project expects a Firebase `google-services.json` file in the `app/` directory for local builds.

Steps for developers:

- Obtain `google-services.json` from the Firebase console for the project.
- Place it at `app/google-services.json` (this file is ignored by Git).
- Do NOT commit `google-services.json`.

If you accidentally committed secrets, rotate the exposed API keys in the Firebase console immediately.
