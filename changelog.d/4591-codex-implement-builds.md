<!-- category: Changed -->
- **A Codex delegate can now build what it writes.** `codex-delegate.sh implement` used to run Codex
  under plain `workspace-write` — no network, nothing writable outside the worktree — so it could
  neither resolve Maven nor run `./gradlew`. It now runs under a Codex permission profile: outbound
  network, a dedicated Gradle home (`~/.gradle-codex`) that shares the real dependency cache
  read-only through `GRADLE_RO_DEP_CACHE`, and writable Android, Kotlin/Native, Maven, Xcode and
  SwiftPM caches. Because network plus read-anywhere is an exfiltration path, the profile also
  read-denies the secrets on the machine: other worktrees' `local.properties`,
  `~/.gradle/gradle.properties` (never handed over — it holds the publishing and signing keys),
  `~/.m2/settings.xml`, gh/npm/ssh/gpg configuration and the Claude and Codex credentials.
  Measured on codex-cli 0.163.0: `:sceneview-core:androidTest` ran 929 tests green inside the
  delegate, while reading the main checkout's `local.properties` answered "Operation not permitted".
