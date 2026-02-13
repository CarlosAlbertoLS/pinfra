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
import wgg.sice_pinfra.R
import wgg.sice_pinfra.InitApplication.Companion.prefs


class ErrorCustomDialogFragment(private val errorMessage: String) : DialogFragment() {

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
        val view = inflater.inflate(R.layout.dialog_error_custom, container, false)
        view.findViewById<TextView>(R.id.txt_error)?.text = errorMessage

        val imageView4 = view.findViewById<ImageView>(R.id.imageView4)
        val imageResource = if (prefs.getEmpresa()) {
            R.drawable.sponsor_wl
        } else {
            R.drawable.sponsor_gt
        }
        imageView4.setImageResource(imageResource)

        return view
    }

    companion object {
        fun newInstance(errorMessage: String): ErrorCustomDialogFragment {
            return ErrorCustomDialogFragment(errorMessage)
        }
    }
}