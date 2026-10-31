package me.aysh.studynest.data.focus

import android.content.Context
import androidx.datastore.preferences.core.edit
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

private val Context.focusDataStore by preferencesDataStore(name = "focus")

/** Keeps the in-progress focus run in DataStore so it survives the process being killed. */
class DataStoreFocusRunStore(context: Context, private val json: Json) : FocusRunStore {

    private val dataStore = context.applicationContext.focusDataStore

    override suspend fun load(): FocusRun? {
        val raw = try {
            dataStore.data.first()[RUN_KEY]
        } catch (_: IOException) {
            null
        } ?: return null
        return runCatching { json.decodeFromString<StoredRun>(raw).toRun() }.getOrNull()
    }

    override suspend fun save(run: FocusRun?) {
        dataStore.edit { prefs ->
            if (run == null) prefs.remove(RUN_KEY) else prefs[RUN_KEY] = json.encodeToString(StoredRun.from(run))
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
    }
}
