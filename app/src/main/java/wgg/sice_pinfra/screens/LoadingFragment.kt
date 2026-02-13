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
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import wgg.sice_pinfra.InitApplication.Companion.prefs
import wgg.sice_pinfra.R

class LoadingFragment(private val type: String) : DialogFragment() {

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
        isCancelable = false
        val view = inflater.inflate(R.layout.dialog_loading, container, false)
        if(type == "CHIP") {
            view.findViewById<TextView>(R.id.textView7)?.text =  "No retire su tarjeta"
        }

        val imageView5 = view.findViewById<ImageView>(R.id.imageView5)
        val imageResource = if (prefs.getEmpresa()) {
            R.drawable.sponsor_wl
        } else {
            R.drawable.sponsor_gt
        }
        imageView5.setImageResource(imageResource)

        return view
    }

    companion object {
        fun newInstance(type: String): LoadingFragment {
            return LoadingFragment(type)
        }
    }
}