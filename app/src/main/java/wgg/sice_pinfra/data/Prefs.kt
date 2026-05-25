package wgg.sice_pinfra.data

import android.content.Context

class Prefs(val context: Context) {
    val DEVICE = "PAX"
    var REFERENCE = "SICE_pinfra"
    var TMOUT = "tmout"
    val PARKING = "ParkingID"
    private val AMEX = "amex"
    private val EMPRESA = "Empresa"
    private val SOUND_ENABLED = "sound_enabled"
    val storage = context.getSharedPreferences(DEVICE, 0)

    fun setReference(result: String) {
        storage.edit().putString(REFERENCE, result).apply()
    }
    fun setTmout(result: Int) {
        storage.edit().putInt(TMOUT, result).apply()
    }
    fun setParking(port: Int) {
        storage.edit().putInt(PARKING, port).apply()
    }
    /** METHOD GET INFO **/
    fun getReference(): String? {
        return storage.getString(REFERENCE, "")
    }
    fun getTmout(): Int {
        return storage.getInt(TMOUT, 60)
    }
    fun getParking():Int? {
        return storage.getInt(PARKING, 100)
    }

    /** MÉTODOS  PARA SWITCH DE AMEX **/
    fun setAmex(isEnabled: Boolean) {
        storage.edit().putBoolean(AMEX, isEnabled).apply()
    }
    fun getAmex(): Boolean {
        return storage.getBoolean(AMEX, true)
    }

    /** MÉTODOS  PARA SWITCH DE EMPRESA **/
    fun setEmpresa(isEnabled: Boolean) {
        storage.edit().putBoolean(EMPRESA, isEnabled).apply()
    }
    fun getEmpresa(): Boolean {
        return storage.getBoolean(EMPRESA, false)
    }

    /** MÉTODOS  PARA SWITCH DE SONIDOS **/
    fun setSoundEnabled(isEnabled: Boolean) {
        storage.edit().putBoolean(SOUND_ENABLED, isEnabled).apply()
    }
    fun isSoundEnabled(): Boolean {
        return storage.getBoolean(SOUND_ENABLED, true)
    }

}