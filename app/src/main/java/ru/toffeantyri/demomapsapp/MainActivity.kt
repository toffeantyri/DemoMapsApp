package ru.toffeantyri.demomapsapp

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.RotationType
import com.yandex.mapkit.map.VisibleRegion
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError
import ru.toffeantyri.demomapsapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), Session.SearchListener, UserLocationObjectListener,
    CameraListener {

    private lateinit var binding: ActivityMainBinding

    private val mapKit: MapKit by lazy { MapKitFactory.getInstance() }
    val userLocationKit: UserLocationLayer by lazy {
        mapKit.createUserLocationLayer(binding.mapview.mapWindow).apply {
            setObjectListener(this@MainActivity)
        }
    }

    private lateinit var searchManager: SearchManager
    private lateinit var searchSession: Session

    private fun submitQuery(query: String) {
        if (query.isBlank()) {
            val mapObj: MapObjectCollection = binding.mapview.mapWindow.map.mapObjects
            mapObj.clear()
            return
        }

        searchSession = searchManager.submit(
            query,
            VisibleRegionUtils.toPolygon(
                VisibleRegion(
                    binding.mapview.mapWindow.map.visibleRegion.topLeft,
                    binding.mapview.mapWindow.map.visibleRegion.topRight,
                    binding.mapview.mapWindow.map.visibleRegion.bottomLeft,
                    binding.mapview.mapWindow.map.visibleRegion.bottomRight,
                )
            ),
            SearchOptions(),
            this
        )
    }


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
        initMapViewBehaviour()
    }

    private fun initMapViewBehaviour() {
        val trafficLayer = mapKit.createTrafficLayer(binding.mapview.mapWindow)
        userLocationKit.isVisible = true
        SearchFactory.initialize(this)
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
        with(binding) {
            mapview.mapWindow.map.addCameraListener(this@MainActivity)
            searchEditText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    submitQuery(searchEditText.text.toString())
                }
                false
            }

            setTrafficButtonColor(trafficLayer.isTrafficVisible)
            ovalButton.setOnClickListener {
                trafficLayer.isTrafficVisible = !trafficLayer.isTrafficVisible
                setTrafficButtonColor(trafficLayer.isTrafficVisible)
            }
        }
    }

    private fun setTrafficButtonColor(visible: Boolean) {
        with(binding) {
            ovalButton.backgroundTintList = if (visible)
                ColorStateList.valueOf(getColor(R.color.green))
            else ColorStateList.valueOf(getColor(R.color.gray))
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

    // Search Listener
    override fun onSearchResponse(response: Response) {
        val mapObj: MapObjectCollection = binding.mapview.mapWindow.map.mapObjects
        mapObj.clear()
        for (searchResult in response.collection.children) {
            searchResult.obj?.geometry?.get(0)?.point?.let { resultLocation ->
                if (response != null) {
                    mapObj.addPlacemark(
                        resultLocation,
                        ImageProvider.fromResource(this, R.drawable.search_result)
                    )
                }
            }
        }
    }

    override fun onSearchError(e: Error) {
        val errorMessage = when (e) {
            is RemoteError -> getString(R.string.remote_error)
            is NetworkError -> getString(R.string.network_error)
            else -> getString(R.string.Unknown_error)
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT)
    }

    // Camera Listener
    override fun onCameraPositionChanged(
        map: Map,
        camPos: CameraPosition,
        cameraUpdateReason: CameraUpdateReason,
        finished: Boolean
    ) {
        val requestText = binding.searchEditText.text.toString()
        if (finished && requestText.isNotBlank()) {
            submitQuery(requestText)
        }
    }

    // user object listener
    override fun onObjectAdded(userLocationView: UserLocationView) {
        userLocationKit.setAnchor(
            PointF(
                (binding.mapview.width * 0.5).toFloat(), (binding.mapview.height * 0.5).toFloat()
            ),
            PointF(
                (binding.mapview.width * 0.5).toFloat(), (binding.mapview.height * 0.83).toFloat()
            )
        )


        userLocationView.arrow.setIcon(
            ImageProvider.fromResource(this, R.drawable.user_location_ic),
            IconStyle().setRotationType(RotationType.ROTATE)
        )

        val picIcon = userLocationView.pin.useCompositeIcon()

        picIcon.setIcon(
            "icon",
            ImageProvider.fromResource(this, R.drawable.user_location_ic),
            IconStyle().setAnchor(PointF(0f, 0f))
                .setRotationType(RotationType.ROTATE)
                .setZIndex(0f)
                .setScale(1f)
        )

//        picIcon.setIcon(
//            "pin", ImageProvider.fromResource(this, R.drawable.dot_selected),
//            IconStyle()
//                .setAnchor(PointF(0.5f, 05f))
//                .setRotationType(RotationType.ROTATE)
//                .setZIndex(1f)
//                .setScale(0.5f)
//        )

        userLocationView.accuracyCircle.fillColor = Color.BLUE and -0x66000001
    }

    override fun onObjectRemoved(userLocationView: UserLocationView) {

    }

    override fun onObjectUpdated(userLocationView: UserLocationView, event: ObjectEvent) {

    }

}
