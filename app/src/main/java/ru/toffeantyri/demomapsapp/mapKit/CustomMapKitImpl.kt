package ru.toffeantyri.demomapsapp.mapKit

import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.map.MapWindow
import com.yandex.mapkit.traffic.TrafficLayer
import com.yandex.mapkit.user_location.UserLocationLayer

class CustomMapKitImpl(private val mapWindow: MapWindow) {

    private val mapKit = MapKitFactory.getInstance()

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

}