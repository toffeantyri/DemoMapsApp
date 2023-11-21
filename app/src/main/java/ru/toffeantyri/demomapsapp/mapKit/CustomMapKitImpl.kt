package ru.toffeantyri.demomapsapp.mapKit

import android.content.Context
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.DrivingSession.DrivingRouteListener
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapWindow
import com.yandex.mapkit.map.VisibleRegion
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.traffic.TrafficLayer
import com.yandex.mapkit.user_location.UserLocationLayer

class CustomMapKitImpl(
    private val context: Context,
    private val mapWindow: MapWindow,
    private val searchSessionListener: Session.SearchListener,
    private val drivingRouteListener: DrivingRouteListener
) {

    init {
        SearchFactory.initialize(context)
    }

    private var mapObjects: MapObjectCollection = mapWindow.map.mapObjects

    private val mapKit = MapKitFactory.getInstance()

    private var searchManager: SearchManager? = null
    private var searchSession: Session? = null

    private val drivingRouter: DrivingRouter by lazy {
        DirectionsFactory.getInstance().createDrivingRouter()
    }
    private var drivingSession: DrivingSession? = null


    private val userLocationLayer: UserLocationLayer by lazy {
        mapKit.createUserLocationLayer(mapWindow)
    }

    private val trafficLayer: TrafficLayer by lazy {
        mapKit.createTrafficLayer(mapWindow)
    }

    fun setUserLocation(visibility: Boolean): UserLocationLayer {
        return userLocationLayer.apply { isVisible = visibility }
    }


    fun setTrafficLayer(visibility: Boolean): TrafficLayer {
        return trafficLayer.apply {
            isTrafficVisible = visibility
        }
    }

    fun initSearchManager(searchType: SearchManagerType) {
        searchManager = SearchFactory.getInstance().createSearchManager(searchType)
    }


    fun submitQuery(query: String, searchOptions: SearchOptions = SearchOptions()) {
        searchSession = searchManager?.submit(
            query,
            VisibleRegionUtils.toPolygon(
                VisibleRegion(
                    mapWindow.map.visibleRegion.topLeft,
                    mapWindow.map.visibleRegion.topRight,
                    mapWindow.map.visibleRegion.bottomLeft,
                    mapWindow.map.visibleRegion.bottomRight,
                )
            ),
            searchOptions,
            searchSessionListener
        )
    }

    fun clearMapObjects() {
        mapObjects.clear()
    }

    fun getMapObjects(): MapObjectCollection {
        return mapObjects
    }

    fun submitRequest(startPoint: Point, endPoint: Point) {
        val drivingOptions = DrivingOptions()
        val vehicleOptions = VehicleOptions()
        val requestPoints = ArrayList<RequestPoint>()

        requestPoints.add(
            RequestPoint(startPoint, RequestPointType.WAYPOINT, null, null),
        )

        requestPoints.add(
            RequestPoint(endPoint, RequestPointType.WAYPOINT, null, null),
        )

        drivingSession =
            drivingRouter.requestRoutes(
                requestPoints,
                drivingOptions,
                vehicleOptions,
                drivingRouteListener
            )
    }

}