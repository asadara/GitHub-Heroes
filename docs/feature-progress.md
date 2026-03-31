# Feature Progress

## Repository Detail

Status: completed

- Added automatic repository preview image detection from the GitHub `homepage` field when the value is a direct image URL.
- Added support for raster formats: `.png`, `.jpg`, `.jpeg`, `.webp`, `.gif`, `.bmp`, `.avif`.
- Added SVG preview rendering support.
- Added in-app preview card on the repository detail screen.
- Added debug preview switching for visual validation in debug builds.

## README Rendering

Status: completed

- Replaced plain-text README preview with markdown rendering.
- Added support for remote images inside README content.
- Added support for relative GitHub README image paths.
- Added support for markdown features used in GitHub-style README content, including tables, task lists, HTML, and strikethrough.

## Build And Delivery

Status: completed

- Upgraded the Android/Gradle toolchain to a stable buildable state.
- Fixed deprecated parcelable access in the legacy detail activity.
- Built and installed the latest debug APK to the connected tablet.

## Social Actions Phase 2

Status: in progress

- Upgraded GitHub OAuth scope handling for public social actions only.
- Added scope readiness indicators in Settings and Profile when re-auth is required.
- Added follow or unfollow support on the live user profile screen.
- Added star and watch toggles on the repository detail screen.
- Added issue target selection plus public issue comment and issue reaction actions on the repository detail screen.
- Verified the connected tablet session carries social scopes: `notifications`, `public_repo`, `read:user`, `user:email`, and `user:follow`.

## Repository Assets

Status: completed

- Captured the Home screen from the connected tablet.
- Saved the screenshot asset in `docs/repo-home-preview.png`.
- Added `README.md` so the screenshot is visible on the GitHub repository page.

## Verification Notes

- Repository homepage image preview is confirmed working when the creator provides a direct image URL.
- Repository README image rendering is confirmed working when the creator provides images in README markdown.
