package xyz.ksharma.krail.core.remoteconfig

import dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfigValue
import dev.gitlive.firebase.remoteconfig.ValueSource

class RemoteConfigValue(private val firebaseRemoteConfigValue: FirebaseRemoteConfigValue) {

    /**
     * Gets the value as a [Boolean].
     *
     * @return [Boolean] representation of this parameter value.
     */
    fun asBoolean(): Boolean = firebaseRemoteConfigValue.asBoolean()

    /**
     * Gets the value as a [ByteArray].
     *
     * @return [ByteArray] representation of this parameter value.
     */
    fun asByteArray(): ByteArray = firebaseRemoteConfigValue.asByteArray()

    /**
     * Gets the value as a [Double].
     *
     * @return [Double] representation of this parameter value.
     */
    fun asDouble(): Double = firebaseRemoteConfigValue.asDouble()

    /**
     * Gets the value as a [Long].
     *
     * @return [Long] representation of this parameter value.
     */
    fun asLong(): Long = firebaseRemoteConfigValue.asLong()

    /**
     * Gets the value as a [String].
     *
     * @return [String] representation of this parameter value.
     */
    fun asString(): String = firebaseRemoteConfigValue.asString()

    /**
     * Checks if the value was fetched from a remote source.
     *
     * @return [Boolean] `true` if the value was fetched from a remote source, `false` otherwise.
     */
    fun isRemoteSource(): Boolean = firebaseRemoteConfigValue.getSource() == ValueSource.Remote
}
