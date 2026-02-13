package wgg.sice_pinfra.mit

import android.content.Context
import android.util.Log
import mx.com.mit.mobile.mitmobilelibrary.manager.transaction.reading.MITReadingCallback
import mx.com.mit.mobile.mitmobilelibrary.manager.transaction.reading.MITReadingManager
import mx.com.mit.mobile.mitmobilelibrary.model.*
import wgg.sice_pinfra.InitApplication.Companion.prefs
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ReadingMIT(
    private val context: Context,
    private val listener: ReadingListener
) : MITReadingCallback {
    private val readingManager: MITReadingManager by lazy {
        MITReadingManager(context, MITReader.Model.IM30, this)
    }
    private var currency: MITCurrency = MITCurrency.MXN
    private var userTransaction: String = ""
    fun connectReading() {
        readingManager.connect()
    }
    fun doRetail(
        amount: String,
        reference: String
    ) {
        val timeout = prefs.getTmout()
        Log.i("timeout", timeout.toString())
        readingManager.setReadingSettings(timeout, true, true)
        readingManager.doRetail(amount, reference, this.currency, this.userTransaction)
    }

    fun cancelReadingCard()  {
        readingManager.cancelReadingCard()
    }
    fun submitPayment(merchant: String) {
        readingManager.submitPayment(merchant)
    }
    /**************************************************** [ M I T ] - MITDeviceCallback **/

    override fun onConnectionResponse(reader: MITReader?, error: MITError?) {
        listener.onConnectionResponse(reader, error)
    }

    override fun onMITError(error: MITError?) {
        error?.description?.let {
            listener.readingResult(it)
        }
    }

    override fun onCancelReadingCard() {
        listener.cancelReadingCard()
    }

    override fun onMerchantResponse(
        contado: ArrayList<MITMerchant>?,
        promotions: ArrayList<MITMerchant>?,
        error: MITError?
    ) {
        listener.readingResult("onObtainMerchantCallback")
    }

    override fun onPaymentAskDcc(localCurrency: MITDccOption, foreignCurrency: MITDccOption) {
        TODO("Not yet implemented")
    }

    override fun onCardInformationResponse(card: MITCard) {
        listener.confirmTransaction(card)
    }

    override fun onTransactionResponse(transaction: MITTransaction) {
        listener.displayTransactionResult(transaction)
    }

    override fun onTransactionProgress(progress: MITProgress) { /** Don't support IM30 **/ }

    override fun onRequestCVVAmex() { /** Don't support IM30 **/ }

    override fun onSelectApplication(cardApplications: ArrayList<MITCardApplication>) { /** Don't support IM30 **/ }

    interface ReadingListener {
        fun onConnectionResponse(reader: MITReader?, error: MITError?)
        fun readingResult(result: String)
        fun cancelReadingCard()
        fun confirmTransaction(bankCard: MITCard)
        fun displayTransactionResult(transaction: MITTransaction)
    }
}