package wgg.sice_pinfra

import android.app.Application
import wgg.sice_pinfra.data.Prefs

class InitApplication: Application() {
    companion object{
        lateinit var prefs: Prefs
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(applicationContext)
    }
}