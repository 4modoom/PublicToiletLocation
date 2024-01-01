package com.samodoom.publictoiletlocation

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.naver.maps.map.overlay.InfoWindow
import com.naver.maps.map.overlay.InfoWindow.DefaultViewAdapter


class ItemAdapter(private val mContext: Context, private val mParent: ViewGroup, val toiletType: String, val toiletTime: String) :
    DefaultViewAdapter(mContext) {
    override fun getContentView(infoWindow: InfoWindow): View {
        val view =
            LayoutInflater.from(mContext).inflate(R.layout.item, mParent, false) as View
        val txtType = view.findViewById<View>(R.id.toiletType) as TextView
        val txtTime = view.findViewById<View>(R.id.toiletTime) as TextView
        txtType.text = toiletType
        txtTime.text = toiletTime
        return view
    }
}