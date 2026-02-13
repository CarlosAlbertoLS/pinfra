package wgg.sice_pinfra.screens

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import wgg.sice_pinfra.InitApplication.Companion.prefs
import wgg.sice_pinfra.R

class SuccessDialogFragment(private val autorizationMessage: String) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
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
        isCancelable = false
        val string = "Autozacion: $autorizationMessage"
        val view = inflater.inflate(R.layout.dialog_success, container, false)
        view.findViewById<TextView>(R.id.txt_autorizacion)?.text = string

        val imageView2 = view.findViewById<ImageView>(R.id.imageView2)
        val imageResource = if (prefs.getEmpresa()) {
            R.drawable.sponsor_wl
        } else {
            R.drawable.sponsor_gt
        }
        imageView2.setImageResource(imageResource)

        return view
    }

    companion object {
        fun newInstance(errorMessage: String): SuccessDialogFragment {
            return SuccessDialogFragment(errorMessage)
        }
    }
}