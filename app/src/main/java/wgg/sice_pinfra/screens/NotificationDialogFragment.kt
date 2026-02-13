package wgg.sice_pinfra.screens

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment

class NotificationDialogFragment(private val layoutId: Int, private val autoDismissTime: Long = 0L) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Crear el diálogo sin título y no cancelable
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Configurar que no se puede cancelar
        isCancelable = false

        // Manejar el tiempo de auto-cierre si se especifica
        if (autoDismissTime > 0) {
            Handler(Looper.getMainLooper()).postDelayed({
                dismiss()
            }, autoDismissTime)
        }

        // Inflar el layout
        return inflater.inflate(layoutId, container, false)
    }

    companion object {
        fun newInstance(layoutId: Int, autoDismissTime: Long = 0L): NotificationDialogFragment {
            return NotificationDialogFragment(layoutId, autoDismissTime)
        }
    }
}
