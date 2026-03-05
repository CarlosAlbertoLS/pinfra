package wgg.sice_pinfra

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.example.firebasedatamodule.AppConfigurator
import com.google.gson.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mx.com.mit.mobile.mitmobilelibrary.model.*
import org.checkerframework.checker.units.qual.s
import wgg.sice_pinfra.InitApplication.Companion.prefs
import wgg.sice_pinfra.mit.ReadingMIT
import wgg.sice_pinfra.databinding.ActivityMainBinding
import wgg.sice_pinfra.mit.ComMIT
import wgg.sice_pinfra.mit.DeviceMIT
import wgg.sice_pinfra.mit.LoginMIT
import wgg.sice_pinfra.screens.ErrorCustomDialogFragment
import wgg.sice_pinfra.screens.InitializingFragment
import wgg.sice_pinfra.screens.LoadingFragment
import wgg.sice_pinfra.screens.SuccessDialogFragment
import wgg.sice_pinfra.system.CheckNetworkTask
import wgg.sice_pinfra.system.PasswordActivity
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.coroutines.Continuation
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

private const val locationAndPhonePermissions = 200

open class MainActivity : AppCompatActivity(), LoginMIT.LoginListener, DeviceMIT.DeviceListener, ReadingMIT.ReadingListener, ComMIT.ComListener {
    private val loginMIT: LoginMIT by lazy { LoginMIT(this, this) }
    private val deviceMIT: DeviceMIT by lazy { DeviceMIT(this, this) }
    private val readingMIT: ReadingMIT by lazy { ReadingMIT(this, this) }
    private val comManager: ComMIT by lazy { ComMIT(this, this) }
    private var dialogConnectionError   : Dialog? = null
    private var timer: CountDownTimer? = null

    private lateinit var binding        : ActivityMainBinding
    private lateinit var appConfigurator: AppConfigurator

    private var typeReading             : String = ""
    private var msgResponse             : String = ""
    private var message                 : String = ""
    private var amount                  : String = ""
    private var reference               : String = ""
    private var networkAttempts         : Int = 0
    private var errorConnection         : Dialog? = null
    private var communicationAttempts   : Int = 0
    private var refInt                  : String = ""
    private var returningFromSettings = false
    private var isProccess = false
    private var lastSentMessage: String = ""

    private var waitingAckFromPayment = false
    private var ackRetryCount = 0
    private var outOfService = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingRunnables = mutableListOf<Runnable>()

    private var paymentJob: Job? = null
    private var responseContinuation: Continuation<Unit>? = null
    private fun postDelayed(delayMs: Long, action: () -> Unit) {
        val runnable = object : Runnable {
            override fun run() {
                action()
                pendingRunnables.remove(this)
            }
        }
        pendingRunnables.add(runnable)
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun enterOutOfServiceMode() {
        waitingAckFromPayment = false
        isProccess = false
        ackRetryCount = 0
        outOfService = true
        writeSerial(JSON_OUT_OF_SERVICE)
        openDialogErrorConnection(MSG_PAYMENT_UNAVAILABLE, MSG_SORRY_INCONVENIENCE)
    }

    private fun buildAckResponse(isConnected: Boolean): String {
        val errorCode = if (isConnected) "0" else "-1"
        return JsonObject().apply {
            addProperty("Respuesta", "ACK")
            addProperty("ErrorCode", errorCode)
        }.toString()
    }
    val androidVersion: String = Build.VERSION.RELEASE
    companion object {
        val jsonPatternCom: Pattern = Pattern.compile("""\{.*?"Tipo":\s*"[^"]+".*?"Datos":\s*\{.*?\}.*?\}""")
        val jsonPatternStatus: Pattern = Pattern.compile("""\{"Respuesta":\s*"[^"]*"\}""")
        val jsonPatternCancel: Pattern = Pattern.compile("""\{"Msg":\s*"[^"]*"\}""")
        val jsonPattern: Pattern = Pattern.compile("""\{"Msg":\s*"[^"]*","Monto":\s*"[^"]*","RefInt":\s*"[^"]*"\}""")
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "MX"))

        // Mensajes de error
        private const val MSG_PAYMENT_UNAVAILABLE = "Pago bancario no disponible"
        private const val MSG_SORRY_INCONVENIENCE = "Lamentamos los inconvenientes"
        private const val MSG_OUT_OF_SERVICE = "Fuera de servicio"
        private const val MSG_TRANSACTION_CANCELLED = "Se canceló la transacción"
        private const val MSG_UNKNOWN_ERROR = "Error desconocido"

        // JSON responses
        private const val JSON_NACK = "{\"Respuesta\": \"NACK\"}"
        private val JSON_OUT_OF_SERVICE = "{\"Msg\":\"$MSG_OUT_OF_SERVICE\"}"

        // Configuración
        private const val MAX_ACK_RETRIES = 3
        private const val MAX_NETWORK_ATTEMPTS = 3
        private const val MAX_COMMUNICATION_ATTEMPTS = 3

        // Tags de diálogos
        private const val DIALOG_TAG_LOADING = "loadingDialog"
        private const val DIALOG_TAG_ERROR = "errorCustomDialog"
        private const val DIALOG_TAG_SUCCESS = "successDialog"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runOnUiThread {
            val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val hasRestarted = sharedPreferences.getBoolean("RESTARTED_ONCE_KEY", false)
            appConfigurator = AppConfigurator(this)
            if (appConfigurator.verificarConfiguracion()) {
                setContentView(R.layout.activity_main)
                binding = ActivityMainBinding.inflate(layoutInflater)
                setContentView(binding.root)
                if (!hasRestarted) {
                    init {
                        sharedPreferences.edit().putBoolean("RESTARTED_ONCE_KEY", true).apply()
                        restartApp()
                    }
                } else {
                    init()
                }
            } else {
                val intent = Intent(this, Class.forName("com.example.firebasedatamodule.StartActivity"))
                startActivity(intent)
                finish()
            }
        }
    }
    private fun restartApp() {
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finishAffinity()
        Runtime.getRuntime().exit(0)
    }
    override fun onResume() {
        super.onResume()
        if (returningFromSettings) {
            updateUI()
            comManager.connect()
            readingMIT.connectReading()

//            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
//                if (throwable is com.vierge.a920library.eemv.exception.EmvException) {
//                    // Manejo de la excepción EmvException
//                    Log.e("MyApplication", "Error no capturado: ${throwable.message}")
//                    // Aquí podrías mostrar un diálogo o reiniciar la actividad
//                } else {
//                    // Manejo de cualquier otra excepción
//                    Log.e("MyApplication", "Error inesperado no capturado: ${throwable.message}")
//
//                }
//            }
            returningFromSettings = false

        }
    }
    private fun updateUI() {
        val paymentImageResource = if (prefs.getAmex()) {
            R.drawable.marks_am
        } else {
            R.drawable.marks_sin_am
        }
        binding.payment.setImageResource(paymentImageResource)

        val imageView9Resource = if (prefs.getEmpresa()) {
            R.drawable.sponsor_wl
        } else {
            R.drawable.sponsor_gt
        }
        binding.imageView4.setImageResource(imageView9Resource)

        val animationView: LottieAnimationView = binding.imgHome
        val animationRes = if (prefs.getEmpresa()) {
            R.raw.pay_wl
        } else {
            R.raw.pay_gt
        }
        animationView.setAnimation(animationRes)
        animationView.playAnimation()
    }

    private fun init(onInitComplete: (() -> Unit)? = null) {
        showInitializingDialog("login", "loadingDialog")
        comManager.connect()
        Log.d("MAIN", androidVersion)
        val logoImageView: ImageView = findViewById(R.id.logo)
        appConfigurator.cargarLogo(logoImageView)
        val paymentImageResource = if (prefs.getAmex()) {
            R.drawable.marks_am
        } else {
            R.drawable.marks_sin_am
        }
        binding.payment.setImageResource(paymentImageResource)

        val imageView9Resource = if (prefs.getEmpresa()) {
            R.drawable.sponsor_wl
        } else {
            R.drawable.sponsor_gt
        }
        binding.imageView4.setImageResource(imageView9Resource)

        val animationView: LottieAnimationView = binding.imgHome
        val animationRes = if (prefs.getEmpresa()) {
            R.raw.pay_wl
        } else {
            R.raw.pay_gt
        }
        animationView.setAnimation(animationRes)
        animationView.playAnimation()

        val firstButton     = binding.firstStep
        val secondButton    = binding.secondStep
        val settingButton   = binding.settings
        firstButton.setOnClickListener { secondButton.isEnabled = true }
        secondButton.setOnClickListener { settingButton.isEnabled = true }
        settingButton.setOnClickListener {
            settingButton.isEnabled = false
            secondButton.isEnabled = false
            startActivity(Intent(this, PasswordActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }

//        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
//            if (throwable is com.vierge.a920library.eemv.exception.EmvException) {
//                // Manejo de la excepción EmvException
//                Log.e("MyApplication", "Error no capturado: ${throwable.message}")
//                // Aquí podrías mostrar un diálogo o reiniciar la actividad
//            } else {
//                // Manejo de cualquier otra excepción
//                Log.e("MyApplication", "Error inesperado no capturado: ${throwable.message}")
//
//            }
//        }
        binding.settings.setOnClickListener {
            openSettings()
        }
        permissions()
        onInitComplete?.invoke()
    }

    private fun permissions() {
        val permissionsToCheck = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
        val permissionsNeeded = mutableListOf<String>()
        for (permission in permissionsToCheck) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission)
            }
        }
        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), locationAndPhonePermissions)
        } else {
            loginMIT.setCredentials()
        }
    }

    private fun openSettings() {
        val intent = Intent(this, PasswordActivity::class.java)
        startActivity(intent)
        returningFromSettings = true
    }

    override fun onDestroy() {
        // Limpiar handlers pendientes para evitar memory leaks
        pendingRunnables.forEach { mainHandler.removeCallbacks(it) }
        pendingRunnables.clear()
        super.onDestroy()
        comManager.disconnect()
    }
    /**************************************************** [ W G G ] **/
    private fun isValidMessageFormat(message: String): Boolean {
        return when {
            jsonPattern.matcher(message).matches() -> true
            jsonPatternStatus.matcher(message).matches() -> true
            jsonPatternCancel.matcher(message).matches() -> true
            jsonPatternCom.matcher(message).matches() -> true
            else -> false
        }
    }

    private suspend fun paymentProcess(msg: JsonObject) {
        return suspendCancellableCoroutine { continuation ->

            responseContinuation = continuation

            continuation.invokeOnCancellation {
                responseContinuation = null
                Log.d("APPLOG", "paymentProcess cancelado")
            }

            communicationAttempts = 0
            val isConnected = CheckNetworkTask(this).execute()
            if (isConnected) {

                message = ""
                networkAttempts = 0
                amount = msg.get("Monto").asString
                refInt = msg.get("RefInt").asString
                reference = "${prefs.getReference()}-$refInt"
                Log.e("doRetail", "1. doRetail")
                //isProccess = true
                readingMIT.doRetail(amount, reference)
            } else {

                networkAttempts++
                postDelayed(1500) {
                    writeSerial(JSON_NACK)
                }
                responseContinuation = null
                if (networkAttempts >= MAX_NETWORK_ATTEMPTS) {
                    closeDialogErrorConnection()
                    openDialogErrorConnection(MSG_PAYMENT_UNAVAILABLE, MSG_SORRY_INCONVENIENCE)
                } else {
                    openDialogErrorConnection("Conectando", "Esperando comunicación")

                }
            }
        }
    }

    private fun handleControl() {

        dialogConnectionError?.dismiss()

        val isConnected = CheckNetworkTask(this).execute()
        communicationAttempts = 0

        writeSerial(buildAckResponse(isConnected))

    }

    private suspend fun handlePaymentRequest(jsonObject: JsonObject) {

        val isConnected = CheckNetworkTask(this).execute()
        writeSerial(buildAckResponse(isConnected))
        if (isConnected) {
            isProccess = true
            paymentProcess(jsonObject)
        }else{
            showConnectionErrorAlert()
        }
    }

    private fun handleUnknownMessage() {
        communicationAttempts++
        msgResponse = ""

        if (communicationAttempts >= MAX_COMMUNICATION_ATTEMPTS) {
            Log.e("SICE[handleUnknownMessage]", "Se han recibido 3 JSON inválidos. Enviando 'Fuera de servicio'")
            writeSerial(JSON_OUT_OF_SERVICE)
            closeDialogErrorConnection()
            openDialogErrorConnection(MSG_PAYMENT_UNAVAILABLE, MSG_SORRY_INCONVENIENCE)
        } else {
            Log.e("SICE[handleUnknownMessage]", "Mensaje inválido recibido. Enviando NACK")
            writeSerial(JSON_NACK)
        }
    }

    private fun handleCancelRequest() {
        if(isProccess){
            readingMIT.cancelReadingCard()
            communicationAttempts = 0

        }else{

        }
    }

    private fun writeSerial(message: String) {
        try {
            lastSentMessage = message
            Log.e("SICE[writeSerial] ***** msg:", message)
            comManager.write(message.toByteArray(StandardCharsets.US_ASCII))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onConnectionResponse(reader: MITReader?, error: MITError?) {
        Log.e("Elementos", "3.")
    }

    /**************************************************** [ M I T ] - ReadingMIT **/
    override fun readingResult(result: String) {
        Log.e("readingResult", "2. $result")
        val errorMessage = when (result) {
            "Favor de verificar su conexión a Internet" -> "Favor de verificar su conexión a Internet"
            "Time Out" -> "Se ha excedido el tiempo límite"
            "Card Removed" -> "Se retiró la tarjeta antes de tiempo"
            "Error en el servicio, favor de intentar nuevamente." -> "Se ha producido un error en el servicio. Por favor, inténtelo nuevamente"
            "Sin conexión, favor de validar el estado de esta transacción en tu reporte" -> "Se ha producido un error en el servicio. Por favor, inténtelo nuevamente"
            else -> "Se ha producido un error."
        }
        message = buildTransactionResultJson(
            msg = "-1",
            descripcionError = errorMessage
        )
        postDelayed(2000) {
            showErrorCustomDialog(errorMessage)
        }
        writeSerial(message)
        waitingAckFromPayment = true
        ackRetryCount = 0

        isProccess = false
    }

    override fun cancelReadingCard() {
        Log.e("cancelReadingCard", "3. cancelReadingCard")
        message = buildTransactionResultJson(
            msg = "-1",
            descripcionError = MSG_TRANSACTION_CANCELLED
        )
        writeSerial(message)
        waitingAckFromPayment = true
        ackRetryCount = 0
        isProccess = false
        paymentJob?.cancel()
        showErrorCustomDialog(MSG_TRANSACTION_CANCELLED)
    }

    private fun buildTransactionResultJson(
        msg: String,
        descripcionError: String = "",
        numTarjeta: String = "",
        nombre: String = "",
        fechaAuth: String? = null,
        numOper: String = "",
        numAuth: String = "",
        ref: String = "",
        tipoTarjeta: String = "",
        marcaTarjeta: String = ""
    ): String {
        val jsonObject = JsonObject().apply {
            addProperty("Msg", msg)
            addProperty("DescripcionError", descripcionError)
            addProperty("NumTarjeta", numTarjeta)
            addProperty("Nombre", nombre)
            addProperty("FechaAuth", fechaAuth ?: sdf.format(Date()))
            addProperty("NumOper", numOper)
            addProperty("NumAuth", numAuth)
            addProperty("Ref", ref)
            addProperty("TipoTarjeta", tipoTarjeta)
            addProperty("MarcaTarjeta", marcaTarjeta)
        }
        return jsonObject.toString()
    }

    override fun confirmTransaction(bankCard: MITCard) {
        Log.e("confirmTransaction", "4. confirmTransaction")
        typeReading = bankCard.reading.toString()
        Log.e("confirmTransaction", "Success")
        readingMIT.submitPayment( "C")
        showLoadingDialog(typeReading)
    }

    override fun displayTransactionResult(transaction: MITTransaction) {
        try {
            Log.e("displayTransactionResult", "5. displayTransactionResult")
            Log.e("Transaction", transaction.toString())

            val isSuccess = transaction.errorCode == null
            val fechaAuth = "${transaction.date ?: ""} ${transaction.time ?: ""}".trim()

            message = if (isSuccess) {
                val tipoTarjeta = transaction.ccType?.substringBeforeLast("/") ?: ""
                val marcaTarjeta = transaction.ccType?.substringAfterLast("/") ?: ""
                buildTransactionResultJson(
                    msg = "0",
                    numTarjeta = transaction.ccNumber ?: "",
                    nombre = transaction.ccName ?: "",
                    fechaAuth = fechaAuth,
                    numOper = transaction.folio ?: "",
                    numAuth = transaction.auth ?: "",
                    ref = reference,
                    tipoTarjeta = tipoTarjeta,
                    marcaTarjeta = marcaTarjeta
                )
            } else {
                buildTransactionResultJson(
                    msg = "-1",
                    descripcionError = transaction.errorDescription ?: "",
                    numTarjeta = transaction.ccNumber ?: "",
                    nombre = transaction.ccName ?: "",
                    fechaAuth = fechaAuth,
                    numOper = transaction.folio ?: "",
                    numAuth = transaction.auth ?: "",
                    ref = reference
                )
            }
            writeSerial(message)
            waitingAckFromPayment = true
            ackRetryCount = 0
            if (isSuccess) {
                responseContinuation?.resume(Unit)
                responseContinuation = null
                isProccess = false
                successDialog(transaction.auth ?: "")
            } else {
                responseContinuation?.resume(Unit)
                responseContinuation = null
                isProccess = false
                showErrorCustomDialog(transaction.errorDescription ?: MSG_UNKNOWN_ERROR)
            }
        } catch (e: Exception) {
            Log.e("displayTransactionResult", "Error Exception: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun openDialogErrorConnection(title: String, subtitle: String) {
        errorConnection = Dialog(this@MainActivity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
            setContentView(R.layout.error_connection_tvm)
            findViewById<TextView>(R.id.connection_text_title)?.text = title
            findViewById<TextView>(R.id.connection_text_subtitle)?.text = subtitle
        }
        errorConnection?.show()
    }

    private fun closeDialogErrorConnection() {
        runOnUiThread {
            errorConnection?.takeIf { it.isShowing }?.dismiss()
        }
    }
    /**************************************************** [ V I E R G E ] - ComListener **/
    private fun showLoadingDialog(type: String) {
        val dialogFragment = LoadingFragment.newInstance(type)
        Handler(Looper.getMainLooper()).post {
            supportFragmentManager.beginTransaction()
                .add(dialogFragment, DIALOG_TAG_LOADING)
                .commitAllowingStateLoss()
            Log.e("showLoadingDialog", "Dialogo de carga mostrado")
        }
    }

    private fun closeLoadingDialog() {
        val dialogFragment = supportFragmentManager.findFragmentByTag(DIALOG_TAG_LOADING) as? DialogFragment
        if (dialogFragment != null) {
            dialogFragment.dismissAllowingStateLoss()
            Log.e("closeLoadingDialog", "Dialogo de carga cerrado")
        } else {
            Log.e("closeLoadingDialog", "No se encontró el diálogo de carga")
        }
    }

    private fun showErrorCustomDialog(errorMessage: String) {
        closeLoadingDialog()
        playSound(R.raw.deny_sound_v2)
        try {
            val errorDialogFragment = ErrorCustomDialogFragment.newInstance(errorMessage)
            supportFragmentManager.beginTransaction()
                .add(errorDialogFragment, "errorCustomDialog")
                .commitAllowingStateLoss()
            Handler(Looper.getMainLooper()).postDelayed({
                closeErrorCustomDialog()
            }, 3000)
        } catch (e: Exception) {
            Log.e("showErrorCustomDialog", "Error mostrando el dialogo personalizado", e)
        }

        isProccess = false
    }

    private fun successDialog(autorizationMessage: String) {
        closeLoadingDialog()
        playSound(R.raw.approved_sound_v1)
        val successDialogFragment = SuccessDialogFragment.newInstance(autorizationMessage)
        supportFragmentManager.beginTransaction()
            .add(successDialogFragment, "successDialog")
            .commitAllowingStateLoss()
        Handler(Looper.getMainLooper()).postDelayed({
            closeSuccessDialog()
        }, 3000)
    }

    private fun closeSuccessDialog() {
        val dialogFragment = supportFragmentManager.findFragmentByTag("successDialog") as? DialogFragment
        dialogFragment?.dismissAllowingStateLoss()
    }

    private fun closeErrorCustomDialog() {
        val dialogFragment = supportFragmentManager.findFragmentByTag("errorCustomDialog") as? DialogFragment
        dialogFragment?.dismissAllowingStateLoss()
    }

    private fun showInitializingDialog(type: String, tag: String) {
        val dialogFragment = InitializingFragment.newInstance(type)
        Handler(Looper.getMainLooper()).post {
            supportFragmentManager.beginTransaction()
                .add(dialogFragment, tag)
                .commitAllowingStateLoss()
            Log.e("showInitializingDialog", "Dialogo de inicializacion mostrado con tag: $tag")
        }
    }

    private fun closeInitializingDialog(tag: String) {
        val dialogFragment = supportFragmentManager.findFragmentByTag(tag) as? DialogFragment
        if (dialogFragment != null) {
            dialogFragment.dismissAllowingStateLoss()
            Log.e("closeInitializingDialog", "Dialogo de inicializacion cerrado")
        } else {
            Log.e("closeInitializingDialog", "No se encontró el diálogo de inicializacion")
        }
    }

    override fun read(msg: ByteArray?) {
        try {
            val chunk = msg?.let { String(it, StandardCharsets.UTF_8) } ?: return
            msgResponse += chunk
            Log.e("SICE[Terminal Message]", "Mensaje recibido: $msgResponse")
            // Process all complete JSON objects in the buffer
            while (true) {
                val startIndex = msgResponse.indexOf('{')
                if (startIndex == -1) return
                // Find matching closing brace by counting nested braces
                var endIndex = -1
                var depth = 0
                for (i in startIndex until msgResponse.length) {
                    when (msgResponse[i]) {
                        '{' -> depth++
                        '}' -> {
                            depth--
                            if (depth == 0) {
                                endIndex = i
                                break
                            }
                        }
                    }
                }
                if (endIndex == -1) {
                    // incomplete JSON, wait for more data
                    return
                }
                val jsonString = msgResponse.substring(startIndex, endIndex + 1)
                Log.e("[RX]", jsonString)
                // Remove processed segment from buffer
                msgResponse = if (endIndex + 1 < msgResponse.length) {
                    msgResponse.substring(endIndex + 1)
                } else {
                    ""
                }
                // If waiting for ACK from a payment, handle ACK logic first
                if (waitingAckFromPayment) {
                    try {
                        val json = JsonParser.parseString(jsonString).asJsonObject
                        if (json.has("Respuesta") && json.get("Respuesta").asString == "ACK") {
                            Log.e("SICE[ACK]", "ACK recibido, desbloqueando")
                            waitingAckFromPayment = false
                            isProccess = false
                            ackRetryCount = 0
                            closeDialogErrorConnection()
                        } else {
                            ackRetryCount++
                            Log.e("SICE[BLOQUEO]", "Mensaje ignorado esperando ACK ($ackRetryCount)")
                            writeSerial(lastSentMessage)
                        }

                        if (ackRetryCount >= MAX_ACK_RETRIES) {
                            Log.e("SICE[BLOQUEO]", "3 intentos sin ACK, Fuera de servicio")
                            enterOutOfServiceMode()
                        }
                    } catch (e: Exception) {
                        ackRetryCount++
                        Log.e("SICE[BLOQUEO]", "JSON inválido esperando ACK ($ackRetryCount)")
                        writeSerial(lastSentMessage)
                        if (ackRetryCount >= MAX_ACK_RETRIES) {
                            Log.e("SICE[BLOQUEO]", "3 intentos sin ACK por JSON inválido, Fuera de servicio")
                            enterOutOfServiceMode()
                        }
                    }
                    continue
                }
                // Si está en proceso de cobro pero aún no espera ACK, solo permitir mensaje de cancelación
                if (isProccess) {
                    try {
                        val jsonObject = JsonParser.parseString(jsonString).asJsonObject
                        if (jsonObject.has("Msg") && jsonObject.get("Msg").asString == "cancel") {

                            Log.e("SICE[CANCEL]", "Mensaje de cancelación recibido durante proceso de cobro")
                            Log.d("APPLOG", "Recibí un cancel")

                            val cancelMessage = buildJsonObject {
                                put("Respuesta", "ACK")
                                put("descripcionError", "Cancel Received")

                            }.toString()
                            writeSerial(cancelMessage)

                            paymentJob?.cancel()
                            handleCancelRequest()

                        } else {
                            Log.e("SICE[IGNORADO]", "Mensaje ignorado, proceso de cobro en curso")
                        }
                    } catch (e: Exception) {
                        Log.e("SICE[IGNORADO]", "Mensaje ignorado (JSON inválido), proceso de cobro en curso")
                    }
                    continue
                }
                // Normal processing flow
                try {
                    val jsonObject = JsonParser.parseString(jsonString).asJsonObject
                    if (!isValidMessageFormat(jsonString)) {
                        Log.e("SICE[NACK]", "JSON inválido recibido")
                        writeSerial(JSON_NACK)
                        handleUnknownMessage()
                        continue
                    }
                    if (outOfService) {
                        Log.e("SICE[RECOVERY]", "JSON válido recibido, saliendo de Fuera de servicio")
                        outOfService = false
                        communicationAttempts = 0
                        closeDialogErrorConnection()
                    }
                    when {
                        jsonObject.has("Msg") -> {
                            when (jsonObject.get("Msg").asString) {
                                "status" -> {
                                    handleControl()
                                }
                                "payment" -> {
                                    paymentJob?.cancel()

                                    paymentJob = lifecycleScope.launch {
                                        try {
                                            Log.d("APPLOG", "Iniciando Pago...")

                                            handlePaymentRequest(jsonObject)

                                            Log.d("APPLOG", "Pago finalizado con éxito")
                                        } catch (e: CancellationException) {

                                            Log.e("APPLOG", "El proceso de pago fue abortado externamente")
                                        }
                                    }

                                }
                                "cancel" -> {
                                    Log.d("APPLOG", "Recibí un cancel")

                                    val calendar = Calendar.getInstance().time
                                    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    val fechaFormateada = formatter.format(calendar)

                                    val cancelMessage = buildTransactionResultJson(
                                        msg = "-1",
                                        descripcionError = "Cancel Received",
                                        numTarjeta = "",
                                        nombre = "",
                                        fechaAuth = fechaFormateada,
                                        numOper = "",
                                        numAuth = "",
                                        ref = reference
                                    )
                                    writeSerial(cancelMessage)

                                    paymentJob?.cancel()
                                    handleCancelRequest()

                                }
                                else -> handleUnknownMessage()
                            }
                        }
                        jsonObject.has("Respuesta") -> {
                            when (jsonObject.get("Respuesta").asString) {
                                "ACK" -> {
                                    communicationAttempts = 0
                                    closeDialogErrorConnection()
                                }
                                "NACK" -> {
                                    writeSerial(lastSentMessage)
                                }
                                else -> handleUnknownMessage()
                            }
                        }
                        else -> handleUnknownMessage()
                    }
                } catch (e: Exception) {
                    Log.e("SICE[ParseException]", e.toString())
                    writeSerial(JSON_NACK)
                    handleUnknownMessage()
                }
            }
        } catch (e: Exception) {
            Log.e("SICE[read Exception]", e.toString())
            msgResponse = ""
            writeSerial(JSON_NACK)
        }
    }

    fun showConnectionErrorAlert() {

        timer?.cancel()

        if (dialogConnectionError == null) {

            dialogConnectionError = Dialog(this).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(false)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setContentView(R.layout.dialog_connection_error)
            }
        }

        val countDownText = dialogConnectionError?.findViewById<TextView>(R.id.registered_ticket_conter)

        timer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countDownText?.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                if (dialogConnectionError?.isShowing == true) {
                    dialogConnectionError?.dismiss()
                }
            }
        }

        if (dialogConnectionError?.isShowing == false) {
            dialogConnectionError?.show()
        }

        timer?.start()
    }

    override fun deviceResult(result: String) {
        Log.e("[deviceResult]", result)
    }

    override fun onConnectionResponse(result: Boolean) {
        if(result) {
            readingMIT.connectReading()
            deviceMIT.initTerminal()
        }
    }

    override fun onInitTerminalError(result: String) {
        Log.e("[onInitTerminalError]", result)
        if (result.contains("EC_0002")) {
            Log.e("[onInitTerminalError]", "Se detectó error de conexión, reintentando login...")

            closeInitializingDialog("loadingDialog")
            showConnectionErrorAlert()

            Handler(Looper.getMainLooper()).postDelayed({
                loginMIT.setCredentials()
            }, 5000)
        }
    }

    override fun onInitTerminalSuccess() {
        Log.e("[onInitTerminalSuccess]", "")
        deviceMIT.hideButtons(false)
        closeInitializingDialog("loadingDialog")
    }

    override fun loginResult(result: String) {
        Log.e("[loginResult]", result)
    }

    override fun onLoginError(result: String) {
        Log.e("[onLoginError]", result)
        if(result.contains("No Network", true)){
            Log.e("LogMIT", "Se perdio la conexion durante el login")
            Handler(Looper.getMainLooper()).postDelayed({ loginMIT.setCredentials()},10000)
        }
    }

    override fun onLoginSuccess(login: MITLogin?) {
        Log.e("[onLoginSuccess]", login.toString())
        deviceMIT.connectDevice()
    }

    private fun playSound(resourceId: Int) {
        if (!prefs.isSoundEnabled()) return
        val mediaPlayer = MediaPlayer.create(this, resourceId)
        mediaPlayer?.setOnCompletionListener { it.release() }
        mediaPlayer?.start()
    }
}