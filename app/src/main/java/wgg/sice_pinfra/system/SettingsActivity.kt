package wgg.sice_pinfra.system

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import wgg.sice_pinfra.InitApplication.Companion.prefs
import wgg.sice_pinfra.mit.DeviceMIT
import wgg.sice_pinfra.R
import wgg.sice_pinfra.databinding.SettingsActivityBinding

class SettingsActivity : AppCompatActivity(), DeviceMIT.DeviceListener {
    private lateinit var binding    : SettingsActivityBinding
    private val deviceMIT: DeviceMIT by lazy { DeviceMIT(this, this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        binding = SettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        deviceMIT.connectDevice()
        binding.back.setOnClickListener{ finish() }
        binding.butttons.setOnCheckedChangeListener{ _, isChecked ->
            if(isChecked)
                deviceMIT.hideButtons(true)
            else
                deviceMIT.hideButtons(false)
        }
        /****** R E F E R E N C E ******/
        binding.editTextReferenceBank.setText(prefs.getReference().toString())
        binding.editTextReferenceBank.setOnKeyListener { _, keyCode, event ->
            when {
                ((keyCode == KeyEvent.KEYCODE_ENTER) && (event.action == KeyEvent.ACTION_DOWN)) -> {
                    // Muestra un Toast indicando que la conexión ha fallado
                    Toast.makeText(this, "Se guardo correctamente", Toast.LENGTH_LONG).show()
                    val referenceBank = binding.editTextReferenceBank.text.toString()
                    prefs.setReference(referenceBank)
                    return@setOnKeyListener true
                }
                else -> false
            }
        }
        /****** T I M E O U T ******/
        binding.editTextTmout.setText(prefs.getTmout().toString())
        binding.editTextTmout.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.let {
                    if (it.isEmpty()) {
                        binding.editTextTmout.error = null
                    } else {
                        try {
                            val num = it.toLong()
                            if (num < 15 || num > 60) {
                                binding.editTextTmout.error = "El número debe estar entre 15 y 60 seg"
                            } else {
                                binding.editTextTmout.error = null
                            }
                        } catch (e: NumberFormatException) {
                            binding.editTextTmout.error = "Por favor, ingrese un número válido"
                        }
                    }
                }
            }

        })
        binding.editTextTmout.setOnKeyListener { _, keyCode, event ->
            when {
                ((keyCode == KeyEvent.KEYCODE_ENTER) && (event.action == KeyEvent.ACTION_DOWN)) -> {
                    val tmout = binding.editTextTmout.text.toString().toInt()
                    if (tmout < 15 || tmout > 60) {
                        // Muestra un Toast indicando que la conexión ha fallado
                        Toast.makeText(this, "No se pudo guardar el valor", Toast.LENGTH_LONG).show()
                    } else {
                        // Muestra un Toast indicando que la conexión ha fallado
                        Toast.makeText(this, "Se guardo correctamente", Toast.LENGTH_LONG).show()
                        prefs.setTmout(tmout)
                    }
                    return@setOnKeyListener true
                }
                else -> false
            }
        }
        /****** A M E X ******/
        binding.butttons3.isChecked = prefs.getAmex()
        binding.butttons3.setOnCheckedChangeListener { _, isChecked ->
            prefs.setAmex(isChecked)
            val message = if (isChecked) "AMEX activado" else "AMEX desactivado"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        /****** W O R K L E V E L ******/
        binding.butttons4.isChecked = prefs.getEmpresa()
        binding.butttons4.setOnCheckedChangeListener { _, isChecked ->
            prefs.setEmpresa(isChecked)
            val message = if (isChecked) "Worklevel Activado" else "WorkLevel desactivado"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        /****** S O N I D O S ******/
        binding.butttons5.isChecked = prefs.isSoundEnabled()
        binding.butttons5.setOnCheckedChangeListener { _, isChecked ->
            prefs.setSoundEnabled(isChecked)
            val message = if (isChecked) "Sonidos activados" else "Sonidos desactivados"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }


    }
    override fun onDestroy() {
        super.onDestroy()
    }
    /**************************************************** [ M I T ] - DeviceMIT **/
    override fun deviceResult(result: String) {
    }
    override fun onConnectionResponse(result: Boolean) {
    }
    override fun onInitTerminalError(error: String) {

    }
    override fun onInitTerminalSuccess() {
    }
}