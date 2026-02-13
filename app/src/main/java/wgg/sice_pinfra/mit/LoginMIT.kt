package wgg.sice_pinfra.mit

import android.content.Context
import mx.com.mit.mobile.mitmobilelibrary.manager.login.MITLoginCallback
import mx.com.mit.mobile.mitmobilelibrary.manager.login.MITLoginManager
import mx.com.mit.mobile.mitmobilelibrary.model.MITEnvironment
import mx.com.mit.mobile.mitmobilelibrary.model.MITError
import mx.com.mit.mobile.mitmobilelibrary.model.MITLogin

import com.example.firebasedatamodule.SecureDataManager
import mx.com.mit.mobile.mitmobilelibrary.model.MITReader


class LoginMIT(
    private val context: Context,
    private val listener: LoginListener
): MITLoginCallback {
    private val loginManager: MITLoginManager by lazy {
//        if (prefs.getParking() == 99) {
//            MITLoginManager(context, this, MITEnvironment.QA)
//        } else {
//            MITLoginManager(context, this, MITEnvironment.PROD)
//        }
        MITLoginManager(context, this, MITEnvironment.QA, MITReader.Model.IM30)
    }

    private lateinit var secureDataManager: SecureDataManager
    private var businessId: String? = null
    private var apiKey: String? = null
    private var encryptKey: String? = null

    fun setCredentials() {

        secureDataManager = SecureDataManager(context)

        val comercio = secureDataManager.getComercio() ?: "default"
        if (comercio != null) {
            val datosRecuperados = secureDataManager.getData(comercio)

            if (datosRecuperados != null) {
                businessId = datosRecuperados["BusinessID"]
                apiKey = datosRecuperados["ApiKey"]
                encryptKey = datosRecuperados["EncryptionKey"]
            } else {
                listener.loginResult("No se encontraron datos para el comercio")
            }
        } else {
            listener.loginResult("No se encontró un comercio.")
        }

        if (businessId != null && apiKey != null && encryptKey != null) {
            loginManager.doLogin(businessId!!, apiKey!!, encryptKey!!)
        } else {
            listener.loginResult("Credenciales no disponibles.")
        }
    }

    override fun onLoginResponse(login: MITLogin?, error: MITError?) {
        if(login != null) {
            listener.loginResult("########################### - onLoginResponse[Success]: $login")
            listener.onLoginSuccess(login)
        } else {
            listener.loginResult("########################### - onLoginResponse[error]: $error")
            listener.onLoginError(error.toString())
        }
    }

    override fun onSessionResponse(login: MITLogin?) {
        listener.loginResult("########################### - onSessionResponse[Success]: $login")
    }
    interface LoginListener {
        fun loginResult(result: String)
        fun onLoginError(result: String)
        fun onLoginSuccess(login: MITLogin?)
    }
}