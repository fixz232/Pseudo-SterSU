# Store UI redesign

## Scope

The Manager store now uses one shared visual system for themes, interface styles,
plugins, customization, the local theme library, profiles, creator workflows,
rankings, font management, and import/confirmation dialogs. Kernel, ksud, signing,
catalog URLs, package verification, and repository publishing are not changed by
this UI work.

## Information architecture

- Themes: search, status filters, categories, previews, favorites, and rankings.
- Styles: search, categories, download settings, version-aware apply/update actions,
  and package details with save/share/remove controls.
- Plugins: search, all/installed/updates filters, compatibility feedback, details,
  usage instructions, install/update, and confirmed removal.
- Customize: grouped appearance, assets, and atmosphere controls.
- My: saved themes, search, import/export, backup/transfer, and profile access.

Primary tabs remain at the bottom on phones and use a navigation rail on wider
layouts. Existing enum ordinals are preserved. Details return to their catalog;
style catalog and detail scrolling are separate. Missing catalog entries show a
recoverable state instead of an unexplained partial detail page.

## Visual rules

- Shared StoreScaffold, neutral light/dark surfaces, app typography, and a readable
  version of the current accent color.
- Surface corners of 12-16 dp, 16 dp content spacing, and 20 dp page gutters.
- Search and actual content before secondary explanations or statistics.
- One clear primary action per catalog item. Infrequent actions live in details.
- Technical metadata and network options use expandable sections.
- Adaptive theme/style columns and maximum content widths for larger screens.
- Existing download validation and destructive-action confirmations are retained.

## Automated verification

Run the Manager task with JDK 21 and the existing bundled ksud binaries:

```powershell
cd E:\KernelSU-main\manager
$env:JAVA_HOME='D:\java21'
$env:GRADLE_JAVA_HOME='D:\java21'
.\gradlew.bat :app:testDebugUnitTest --no-daemon
cd ..
git diff --check
```

Added regression coverage checks navigation order and saved ordinals, light/dark
text contrast with custom accents, and style-update state (including same-version
content changes and stale catalogs that must not downgrade a newer installation).

## Device validation checklist

Compilation and unit tests do not replace visual or touch validation. Check on a
phone and tablet before release:

- Light/dark mode and the app's supported interface styles.
- Narrow portrait, landscape, large system fonts, cutouts, and keyboard insets.
- Tab switching, detail back navigation, restored list position, and recreation.
- Loading, offline cached data, empty catalogs, no search results, and retry.
- Theme download/apply/rollback, favorites, and local-library import/export.
- Style install/update/apply, save/share, and confirmed removal.
- Plugin compatibility, paired-plugin requirements, update, and removal.
- Profile editing, creator drafts/submission, and font import/select/remove.

No APK, device installation, Git commit, or remote publication is part of this
redesign task.
