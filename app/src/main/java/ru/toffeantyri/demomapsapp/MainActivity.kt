package ru.toffeantyri.demomapsapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import ru.toffeantyri.demomapsapp.databinding.ActivityMainBinding

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
        initLocationCoarsePermission()
        initMapView()
    }

    private fun initMapView() {
        val mapKit = MapKitFactory.getInstance()
        val trafficLayer = mapKit.createTrafficLayer(binding.mapview.mapWindow)
        val userLocation = mapKit.createUserLocationLayer(binding.mapview.mapWindow)
        userLocation.isVisible = true
        binding.ovalButton.setOnClickListener {
            trafficLayer.isTrafficVisible = !trafficLayer.isTrafficVisible
        }
    }

    private val locationCoarsePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            initLocationFinePermission()
        } else {
            Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    private fun initLocationCoarsePermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initLocationFinePermission()
        } else {
            locationCoarsePermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    private val locationFinePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    private fun initLocationFinePermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show()
        } else {
            locationFinePermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapview.onStart()
    }

    override fun onStop() {
        binding.mapview.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

}
