package me.aysh.studynest.data.focus

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.aysh.studynest.data.model.SubjectColor
import me.aysh.studynest.data.model.SubjectIcon
import me.aysh.studynest.data.model.SubjectRef
import java.io.IOException
import java.time.Instant

private val Context.focusDataStore by preferencesDataStore(
    name = "focus",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/** Keeps the in-progress focus run in DataStore so it survives the process being killed. */
class DataStoreFocusRunStore(context: Context, private val json: Json) : FocusRunStore {

    private val dataStore = context.applicationContext.focusDataStore

    override suspend fun load(): FocusRun? =
        read(RUN_KEY)?.let { raw -> runCatching { json.decodeFromString<StoredRun>(raw).toRun() }.getOrNull() }

    override suspend fun save(run: FocusRun?) = write(RUN_KEY, run?.let { json.encodeToString(StoredRun.from(it)) })

    override suspend fun loadPending(): PendingFocusSave? =
        read(PENDING_KEY)?.let { raw -> runCatching { json.decodeFromString<StoredPending>(raw).toPending() }.getOrNull() }

    override suspend fun savePending(pending: PendingFocusSave?) =
        write(PENDING_KEY, pending?.let { json.encodeToString(StoredPending.from(it)) })

    private suspend fun read(key: Preferences.Key<String>): String? =
        try {
            dataStore.data.first()[key]
        } catch (_: IOException) {
            null
        }

    private suspend fun write(key: Preferences.Key<String>, value: String?) {
        dataStore.edit { prefs -> if (value == null) prefs.remove(key) else prefs[key] = value }
    }

    @Serializable
    private data class StoredPending(
        val id: String,
        val subject: StoredSubject? = null,
        val startedAtMillis: Long,
        val seconds: Int,
        val completed: Boolean,
    ) {
        fun toPending() = PendingFocusSave(id, subject?.toRef(), Instant.ofEpochMilli(startedAtMillis), seconds, completed)

        companion object {
            fun from(p: PendingFocusSave) = StoredPending(
                id = p.id,
                subject = p.subject?.let(StoredSubject::from),
                startedAtMillis = p.startedAt.toEpochMilli(),
                seconds = p.seconds,
                completed = p.completed,
            )
        }
    }

    @Serializable
    private data class StoredSubject(val id: String, val name: String, val color: String, val icon: String) {
        fun toRef() = SubjectRef(id, name, SubjectColor.fromKey(color), SubjectIcon.fromKey(icon))

        companion object {
            fun from(ref: SubjectRef) = StoredSubject(ref.id, ref.name, ref.color.key, ref.icon.key)
        }
    }

    @Serializable
    private data class StoredRun(
        val subjectId: String? = null,
        val subjectName: String? = null,
        val subjectColor: String? = null,
        val subjectIcon: String? = null,
        val plannedMillis: Long,
        val startedAtMillis: Long,
        val accumulatedMillis: Long,
        val resumedAtElapsed: Long? = null,
        val resumedAtWallMillis: Long? = null,
    ) {
        fun toRun() = FocusRun(
            subject = if (subjectId != null && subjectName != null) {
                SubjectRef(
                    subjectId,
                    subjectName,
                    SubjectColor.fromKey(subjectColor.orEmpty()),
                    SubjectIcon.fromKey(subjectIcon.orEmpty()),
                )
            } else {
                null
            },
            plannedMillis = plannedMillis,
            startedAt = Instant.ofEpochMilli(startedAtMillis),
            accumulatedMillis = accumulatedMillis,
            resumedAtElapsed = resumedAtElapsed,
            resumedAtWallMillis = resumedAtWallMillis,
        )

        companion object {
            fun from(run: FocusRun) = StoredRun(
                subjectId = run.subject?.id,
                subjectName = run.subject?.name,
                subjectColor = run.subject?.color?.key,
                subjectIcon = run.subject?.icon?.key,
                plannedMillis = run.plannedMillis,
                startedAtMillis = run.startedAt.toEpochMilli(),
                accumulatedMillis = run.accumulatedMillis,
                resumedAtElapsed = run.resumedAtElapsed,
                resumedAtWallMillis = run.resumedAtWallMillis,
            )
        }
    }

    private companion object {
        val RUN_KEY = stringPreferencesKey("run")
        val PENDING_KEY = stringPreferencesKey("pending")
    }
}
