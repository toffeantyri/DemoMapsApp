package ru.toffeantyri.demomapsapp

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import ru.toffeantyri.demomapsapp.databinding.ActivityMainBinding
import ru.toffeantyri.demomapsapp.utils.checkPermissionSingle

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding.mapview) {
            mapWindow.map.move(
                CameraPosition(
                    Point(56.8389261, 60.6057025), 10f, 0f, 0f
                ), Animation(Animation.Type.SMOOTH, 5f), null
            )
        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapview.onStart()
        val mapKit = MapKitFactory.getInstance()
        initMapView(mapKit)
        checkPermissionSingle(Manifest.permission.ACCESS_FINE_LOCATION) {
            checkPermissionSingle(Manifest.permission.ACCESS_COARSE_LOCATION) {
                val userLocation = mapKit.createUserLocationLayer(binding.mapview.mapWindow)
                userLocation.isVisible = true
            }
        }
    }

    override fun onStop() {
        binding.mapview.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }


    private fun initMapView(mapKit: MapKit) {
        val trafficLayer = mapKit.createTrafficLayer(binding.mapview.mapWindow)
        binding.ovalButton.setOnClickListener {
            trafficLayer.isTrafficVisible = !trafficLayer.isTrafficVisible
        }


    }

}
