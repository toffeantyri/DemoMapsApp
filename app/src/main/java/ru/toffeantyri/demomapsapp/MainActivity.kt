package ru.toffeantyri.demomapsapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import com.google.android.gms.location.LocationServices
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.PolylineBuilder
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
import ru.toffeantyri.demomapsapp.list_adapter.AddressListAdapter
import ru.toffeantyri.demomapsapp.list_adapter.ListItemClickInterface
import ru.toffeantyri.demomapsapp.model.PointAddressData

class MainActivity : AppCompatActivity(), Session.SearchListener, UserLocationObjectListener,
    CameraListener, DrivingSession.DrivingRouteListener, ListItemClickInterface {

    private var inputMethodManager: InputMethodManager? = null
    private lateinit var binding: ActivityMainBinding

    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(
            this
        )
    }

    private val mapKit: MapKit by lazy { MapKitFactory.getInstance() }
    private val userLocationKit: UserLocationLayer by lazy {
        mapKit.createUserLocationLayer(binding.mapview.mapWindow).apply {
            setObjectListener(this@MainActivity)
        }
    }

    private var startPoint = Point(56.856417, 60.636695)
    private var endPoint = Point(56.878817, 60.610532)
    private var centerScreenLocation = getScreenCenter(startPoint, endPoint)


    private var mapObjects: MapObjectCollection? = null

    private val drivingRouter: DrivingRouter by lazy {
        DirectionsFactory.getInstance().createDrivingRouter()
    }

    private var drivingSession: DrivingSession? = null

    private lateinit var searchManager: SearchManager
    private lateinit var searchSession: Session

    private val addressList: List<PointAddressData> by lazy {
        getAddressListPlease()
    }

    private val listAdapter by lazy {
        AddressListAdapter(this, R.layout.address_item, addressList, this)
    }

    private fun getAddressListPlease(): List<PointAddressData> {
        return listOf(
            PointAddressData(56.856417, 60.636695, "Home"),
            PointAddressData(56.878817, 60.610532, "Work"),
            PointAddressData(56.829281, 60.603592, "Green witch")
        )
    }


    private fun getScreenCenter(first: Point, second: Point): Point {
        return Point(
            (first.latitude + second.latitude) / 2,
            (first.longitude + second.longitude) / 2,
        )
    }


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
        inputMethodManager = this.getSystemService()
        initLocationCoarsePermission()
        initListView()
        initMapViewBehaviour()
    }

    private fun initListView() {
        with(binding) {
            listView.adapter = listAdapter
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initMapViewBehaviour() {
        userLocationKit.isVisible = false
        SearchFactory.initialize(this)
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)

        with(binding) {
            mapObjects = mapview.mapWindow.map.mapObjects.addCollection()

            mapview.mapWindow.map.addCameraListener(this@MainActivity)

            searchEditText.setOnTouchListener { v, event ->
                listView.visibility = View.VISIBLE
                false
            }


            searchEditText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    submitQuery(searchEditText.text.toString())
                    listView.visibility = View.GONE
                }

                false
            }


            val trafficLayer = mapKit.createTrafficLayer(mapview.mapWindow)
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
        //mapObj.clear()
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
        binding.listView.visibility = View.GONE

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

    //DrivenSessionListener
    override fun onDrivingRoutes(p0: MutableList<DrivingRoute>) {
        for (route in p0) {
            val polyline = PolylineBuilder().apply {
                append(route.geometry)
            }.build()

            mapObjects?.addPolyline(polyline)
        }
    }

    override fun onDrivingRoutesError(p0: Error) {
        val errorMessage = when (p0) {
            else -> getString(R.string.Unknown_error)
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }

    private fun submitRequest(startPoint: Point, endPoint: Point) {
        val drivingOptions = DrivingOptions()
        val vehicleOptions = VehicleOptions()
        val requestPoints = ArrayList<RequestPoint>()

        requestPoints.add(
            RequestPoint(
                startPoint,
                RequestPointType.WAYPOINT,
                null,
                null
            ),
        )

        requestPoints.add(
            RequestPoint(
                endPoint,
                RequestPointType.WAYPOINT,
                null,
                null
            ),
        )

        drivingSession =
            drivingRouter.requestRoutes(requestPoints, drivingOptions, vehicleOptions, this)


    }

    @SuppressLint("MissingPermission")
    override fun itemClick(pos: Int) {
        binding.listView.visibility = View.GONE
        inputMethodManager?.hideSoftInputFromWindow(binding.root.windowToken, 0)
        mapObjects?.clear()

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                Toast.makeText(this, "Начальное местоположение неизвестно", Toast.LENGTH_SHORT)
                    .show()
                return@addOnSuccessListener
            }
            startPoint = Point(location.latitude, location.longitude)
            endPoint = Point(addressList[pos].lat, addressList[pos].lon)
            centerScreenLocation = getScreenCenter(startPoint, endPoint)
            userLocationKit.isVisible = true
            with(binding.mapview) {
                mapWindow.map.move(
                    CameraPosition(
                        centerScreenLocation, 12f, 0f, 0f
                    ), Animation(Animation.Type.SMOOTH, 3f), null
                )
            }
            submitRequest(startPoint, endPoint)
        }
    }

}
