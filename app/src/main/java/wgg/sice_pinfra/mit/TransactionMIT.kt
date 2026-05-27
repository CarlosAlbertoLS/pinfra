package wgg.sice_pinfra.mit

import android.content.Context
import android.util.Log
import mx.com.mit.mobile.mitmobilelibrary.manager.functions.MITTransactionCallback
import mx.com.mit.mobile.mitmobilelibrary.manager.functions.MITTransactionManager
import mx.com.mit.mobile.mitmobilelibrary.manager.transaction.reading.MITReadingCallback
import mx.com.mit.mobile.mitmobilelibrary.model.MITCard
import mx.com.mit.mobile.mitmobilelibrary.model.MITError
import mx.com.mit.mobile.mitmobilelibrary.model.MITReader
import mx.com.mit.mobile.mitmobilelibrary.model.MITReport
import mx.com.mit.mobile.mitmobilelibrary.model.MITTransaction

class TransactionMIT(
    private val context: Context,
    private val transactionListener: TransactionListener
): MITTransactionCallback {
    private val transactionManager: MITTransactionManager by lazy {
        MITTransactionManager(context, this)
    }

    fun getTransactionByReference(reference: String){
        transactionManager.getTransactionsByReference(reference)
    }

    override fun onRefundTransaction(
        transaction: MITTransaction?,
        error: MITError?
    ) {
    }

    override fun onReturnTransactions(report: MITReport?, error: MITError?) {
        Log.d("APPLOG", "Lo recibido desde mit: $report")
        transactionListener.onReturnTransactions(report, error)
    }

    override fun onSaveSignatureResponse(error: MITError?) {
    }

    override fun onURLVoucherResponse(
        urlTicket: String?,
        error: MITError?
    ) {
    }

    override fun onVoucherByEmailResponse(error: MITError?) {
    }

    override fun onVoucherBySmsResponse(error: MITError?) {
    }

    interface TransactionListener {
        fun onReturnTransactions(report: MITReport?, error: MITError?)
    }

}