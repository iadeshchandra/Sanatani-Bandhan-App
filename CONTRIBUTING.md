# Contributing to Sanatani Bandhan CRM

Thank you for your interest in contributing to our SaaS platform! 

As an enterprise-grade application managing sensitive community data, donations, and spiritual events, we enforce strict coding and UI standards.

## Development Rules

1. **Architecture:** This app uses Native Android (Java) and Firebase Realtime Database. We operate an offline-first model, so ensure `keepSynced(true)` is respected for all core database references.
2. **UI/UX Standards:**
   - All horizontal buttons must use `weightSum="2"` for perfect 50/50 alignment.
   - Text fields displaying User IDs, Phone Numbers, or specific amounts must utilize `singleLine="true"` and `ellipsize="end"` to prevent wrapping.
   - Use Material Design `MaterialCardView` with `12dp` corner radiuses and `#FFFBF2` backgrounds.
3. **Spiritual Branding:** - Do not remove the daily Shlokas or the English/Bengali date tracking from the Dashboard.
   - Any modifications to the `PdfReportService.java` must retain the Sanatani headers (*"Ahimsa paramo dharma"*) and closing blessings.
4. **CI/CD Pipeline:** Do not commit code that breaks the GitHub Actions build. Always run `./gradlew assembleDebug` locally before pushing.

## Submitting Changes
Create a new branch for your feature, ensure all XML UI elements are perfectly aligned without breaking existing Java backend logic, and submit a Pull Request for review.
