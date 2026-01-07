package com.example.turnoshospi.util

/**
 * Wrapper para estados de recursos con soporte para cache.
 *
 * @param T tipo de datos
 * @property data los datos (puede ser null en Loading o Error)
 * @property message mensaje de error (solo en Error)
 * @property fromCache indica si los datos vienen del cache local
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null,
    val fromCache: Boolean = false
) {
    /**
     * Estado de carga inicial.
     */
    class Loading<T>(data: T? = null) : Resource<T>(data)

    /**
     * Estado exitoso con datos.
     *
     * @param data los datos obtenidos
     * @param fromCache true si vienen del cache local, false si vienen de Firebase
     */
    class Success<T>(data: T, fromCache: Boolean = false) : Resource<T>(data, fromCache = fromCache)

    /**
     * Estado de error.
     *
     * @param message mensaje descriptivo del error
     * @param data datos parciales (puede ser cache previo)
     */
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)

    /**
     * Verifica si es estado Loading.
     */
    val isLoading: Boolean get() = this is Loading

    /**
     * Verifica si es estado Success.
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Verifica si es estado Error.
     */
    val isError: Boolean get() = this is Error

    /**
     * Obtiene los datos o un valor default.
     */
    fun dataOrDefault(default: T): T = data ?: default

    companion object {
        /**
         * Crea un Resource.Success desde datos.
         */
        fun <T> success(data: T, fromCache: Boolean = false): Resource<T> =
            Success(data, fromCache)

        /**
         * Crea un Resource.Error desde una excepcion.
         */
        fun <T> error(exception: Throwable, data: T? = null): Resource<T> =
            Error(exception.message ?: "Error desconocido", data)

        /**
         * Crea un Resource.Error desde un mensaje.
         */
        fun <T> error(message: String, data: T? = null): Resource<T> =
            Error(message, data)

        /**
         * Crea un Resource.Loading.
         */
        fun <T> loading(data: T? = null): Resource<T> = Loading(data)
    }
}
