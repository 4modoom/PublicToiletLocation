package com.samodoom.publictoiletlocation

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.InfoWindow
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.util.FusedLocationSource
import org.json.JSONObject
import kotlin.math.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback {


    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // 지구의 반지름(km)

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    private val LOCATION_PERMISSION_REQUEST_CODE = 5000

    private val PERMISSIONS = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private lateinit var naverMap: NaverMap
    private lateinit var locationSource: FusedLocationSource
    private var locationManager: LocationManager? = null

    // onCreate에서 권한을 확인하며 위치 권한이 없을 경우 사용자에게 권한을 요청한다.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val json = assets.open("list.json").reader().readText()
        val data = JSONObject(json).getJSONArray("data")

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE)
        }

        initMapView()

        val button = findViewById<Button>(R.id.button)
        val markers = arrayOfNulls<Marker>(362)
        val infoWindow = InfoWindow()

        button.setOnClickListener {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                // 위치를 얻었을 때의 동작을 정의합니다.
                // location 객체에서 위도와 경도를 얻을 수 있습니다.
                val latitude = location?.latitude
                val longitude = location?.longitude
                val cameraUpdate = CameraUpdate.scrollTo(LatLng(latitude!!, longitude!!))
                    .animate(CameraAnimation.Easing)
                naverMap.moveCamera(cameraUpdate)
                val locationOverlay = naverMap.locationOverlay
                locationOverlay.isVisible = true
                locationOverlay.circleRadius = 100
                for (i in 0..361) {
                    markers[i]?.map = null
                    val toiletLatitude = data.getJSONObject(i).getString("WGS84위도").toDouble()
                    val toiletLongitude = data.getJSONObject(i).getString("WGS84경도").toDouble()
                    val toiletType = data.getJSONObject(i).getString("구분")
                    val toiletTime = data.getJSONObject(i).getString("개방시간")
                    val distance =
                        calculateDistance(latitude, longitude, toiletLatitude, toiletLongitude)
                    if (distance <= 0.6) { // 반경 600M
                        markers[i] = Marker()
                        markers[i]!!.position = LatLng(toiletLatitude, toiletLongitude)
                        markers[i]!!.map = naverMap
                        println("d: $distance")

                        val rootView = findViewById<View>(R.id.map_fragment) as ViewGroup
                        val adapter = ItemAdapter(this@MainActivity, rootView, toiletType, toiletTime)
                        infoWindow.adapter = adapter
                        infoWindow.zIndex = 10
                        infoWindow.alpha = 0.9f

                        val listener = Overlay.OnClickListener { overlay ->
                            val marker = overlay as Marker

                            if (marker.infoWindow == null) {
                                // 현재 마커에 정보 창이 열려있지 않을 경우 엶
                                infoWindow.open(marker)
                            } else {
                                // 이미 현재 마커에 정보 창이 열려있을 경우 닫음
                                infoWindow.close()
                            }

                            true
                        }
                        markers[i]!!.onClickListener = listener

                    }
                }
            }
        }
    }

    private fun initMapView() {
        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.map_fragment, it).commit()
            }

        // fragment의 getMapAsync() 메서드로 OnMapReadyCallback 콜백을 등록하면 비동기로 NaverMap 객체를 얻을 수 있다.
        mapFragment.getMapAsync(this)
        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
    }

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        // 현재 위치
        naverMap.locationSource = locationSource
        // 현재 위치 버튼 기능
        naverMap.uiSettings.isLocationButtonEnabled = true
        // 위치를 추적하면서 카메라도 따라 움직인다.
        naverMap.locationTrackingMode = LocationTrackingMode.Follow
    }

}