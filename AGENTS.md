<!-- Added: 2026-10-31 -->
## StudyNest architecture conventions
- Android: manual DI via AppContainer (no Hilt). Screens get ViewModels with appViewModel { c -> ... }. Repositories throw only AppException (typed Kind); StudyRepository.revision bumps after every successful write and screens reload with combine(revision, reloads).collectLatest.
- One-off UI events use Channel + receiveAsFlow; app-wide snackbars go through LocalAppMessenger (root-scoped, survives screen close).
- Every Compose Dialog/ModalBottomSheet must expose test tags (Modifier.exposeTestTags()); scripts/demo.py finds nodes by test tag resource-id.
- FocusTimer is timestamp-based (elapsedRealtime + wall clock) and persisted in DataStore; never count ticks.
- Kotlin warnings are errors (allWarningsAsErrors). Release builds use R8: endpoints whose body is ignored return Response<Unit>.
- Server: every route scoped by userId; strict Zod schemas with defaults only on create; dates are the student's local YYYY-MM-DD sent by the client; weeks start Monday.
