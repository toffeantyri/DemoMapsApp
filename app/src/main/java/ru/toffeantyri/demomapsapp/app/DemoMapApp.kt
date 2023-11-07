package ru.toffeantyri.demomapsapp.app

import android.app.Application
import com.yandex.mapkit.MapKitFactory
import ru.toffeantyri.demomapsapp.BuildConfig

class DemoMapApp : Application() {


    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
        MapKitFactory.initialize(this)
    }

}