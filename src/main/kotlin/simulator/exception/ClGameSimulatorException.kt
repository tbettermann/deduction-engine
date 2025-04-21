package de.tb.simulator.exception

class ClGameSimulatorException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    constructor(cause: Throwable) : this(null, cause)
}