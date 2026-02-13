package wgg.sice_pinfra.mit

import android.content.Context
import android.util.Log
import mx.com.mit.mobile.mitmobilelibrary.manager.com.rs232.MITComCallback
import mx.com.mit.mobile.mitmobilelibrary.manager.com.rs232.MITComManager
import mx.com.mit.mobile.mitmobilelibrary.model.MITError
import mx.com.mit.mobile.mitmobilelibrary.model.MITPort
import mx.com.mit.mobile.mitmobilelibrary.model.MITReader

class ComMIT(
    context: Context,
    private val listener: ComListener
) : MITComCallback {

    private val comManager: MITComManager by lazy {
        MITComManager(context, MITReader.Model.IM30, this)
    }
    fun connect() {
        comManager.setComParams(MITPort.COM2, 9600, 8, 'n', 1)
        comManager.connectComPort()
    }
    fun disconnect() {
        comManager.disconnectComPort()
    }
    fun write(msg: ByteArray) {
        comManager.writeMessage(msg)
    }
    override fun onCOMError(error: MITError) {
        Log.e("SICE[onCOMError:]", error.toString())
    }
    override fun onConnected(msg: String) {
        Log.e("SICE[onConnected:]", msg)
    }
    override fun onDisconnected() {
        Log.e("SICE[onDisconnected:]", "msg")
    }
    override fun onReadMessage(msg: ByteArray?) {
        listener.read(msg)
    }
    override fun onSetParam() {
        Log.e("SICE[onSetParam:]", "msg")
    }
    interface ComListener {
        fun read(msg: ByteArray?)
    }
}