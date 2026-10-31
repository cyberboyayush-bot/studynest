package me.aysh.studynest.ui

/** Screen content that is fetched from the server. */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Failed(val message: String) : Loadable<Nothing>
    data class Ready<T>(val value: T) : Loadable<T>

    val valueOrNull: T? get() = (this as? Ready<T>)?.value
}
