package jareddefour.example.comp3606a2

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5RetainHandling


import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck
import jareddefour.example.comp3606a2.Publisher.Publisher
import jareddefour.example.comp3606a2.PublisherList.PublisherListAdapter
import org.json.JSONObject
import java.nio.charset.StandardCharsets


class MainActivity : AppCompatActivity(), OnMapReadyCallback  {

    private lateinit var mMap: GoogleMap
//    private var client: Mqtt5BlockingClient? = null
    private var client: Mqtt5Client? = null
    private var subAck: Mqtt5SubAck? = null
    private var publisherListAdapter: PublisherListAdapter? = null
    private val pointsList = mutableListOf<PointsMap>()
    private val publisherList = mutableListOf<Publisher>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val publisherListingView : RecyclerView =findViewById(R.id.publisher_list)

        publisherListAdapter = PublisherListAdapter(this,this,publisherList)
        publisherListingView.adapter =publisherListAdapter
        publisherListingView.layoutManager = LinearLayoutManager(this)

        client= Mqtt5Client.builder().serverHost("broker.sundaebytestt.com").identifier("8160359802").buildAsync()

        try {
            (client as Mqtt5AsyncClient).connect()
            Toast.makeText(this,"Successfully connected to Broker", Toast.LENGTH_SHORT).show()

        } catch (e:Exception){
            Toast.makeText(this,"An error occurred when connecting to broker", Toast.LENGTH_SHORT).show()
        }



        subAck = (client as Mqtt5AsyncClient).subscribeWith()
                .topicFilter("assignment/location")
                .qos(MqttQos.AT_LEAST_ONCE)
                .noLocal(true)                                      // we do not want to receive our own message
                .retainHandling(Mqtt5RetainHandling.DO_NOT_SEND)    // do not send retained messages
                .retainAsPublished(true)                            // keep the retained flag as it was published
                .callback { publish -> storeLocation(publish) }
            .send().join()


        Log.e("TEST", "subscribed $subAck");


    }


    fun addPublisher(publisherID: String) {
        var inList = false
        for (p in publisherList) {
            if (p.id == publisherID) {
                return
            }
        }
        publisherListAdapter?.addItemToEnd(Publisher(publisherID))
        Log.e("SUCCESS", "THE PUBLISHER SHOULD BE REFLECTED IN THE ATTENDEES")
    }

    private fun storeLocation(publish: Mqtt5Publish?) {
        if (publish != null ) {
            val string:String = String(publish.payloadAsBytes, StandardCharsets. UTF_8)
            Log.e("Tag","received message: $string ")
            val jsonString = JSONObject(string)
            if(!hasKeys(jsonString.keys()))
                return
            val timestamp = jsonString["timestamp"].toString()
            val publisherID = jsonString["id"].toString()
            val longitude = jsonString["longitude"].toString()
            val latitude = jsonString["latitude"].toString()
            val latLong = LatLng(latitude.toDouble(),longitude.toDouble())


            val pMap = PointsMap(publisherID,timestamp.toLong(),latLong)
            runOnUiThread{
                pointsList.add(pMap)
                drawPolyline()
                addPublisher(publisherID)
            }

        }

    }

    private fun hasKeys(keys: Iterator<String>): Boolean {
        for(k in keys){
            if(!isKey(k.toString()))
                return false
        }
        return true
    }

    private fun isKey(key: String): Boolean {
        return !(key != "id" && key != "timestamp" && key != "longitude" && key!="latitude")
    }



    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(10.436808, -61.423523), 9.0f))

//        mMap.setOnMapClickListener { latLng ->
//            addMarkerAtLocation(latLng)
//            drawPolyline()
//        }
    }
//
//    private fun addMarkerAtLocation(latLng: LatLng) {
//        val newCustomPoint = PointsMap("816035980",123, latLng)
//
//        pointsList.add(newCustomPoint)
//
//        mMap.addMarker(
//            MarkerOptions()
//                .position(latLng)
//                .title("Marker ${newCustomPoint.point}")
//        )
//
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
//    }

    private fun drawPolyline() {

    runOnUiThread{
        val latLngPoints = pointsList.map { it.point }
        mMap.clear()


        val polylineOptions = PolylineOptions()
            .addAll(latLngPoints)
            .color(Color.RED)
            .width(5f)
            .geodesic(true)

        mMap.addPolyline(polylineOptions)

    }
//        val bounds = LatLngBounds.builder()
//        latLngPoints.forEach { bounds.include(it) }
//        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))


    }

    override fun onDestroy() {
        client?.toBlocking()?.disconnect()
        super.onDestroy()
    }

    fun updateHeader(publisherID: String) {
        val summaryTitle: TextView = findViewById(R.id.summaryTitle)
        summaryTitle.text = "Summary of - $publisherID"
    }

    fun openSummary(publisherID: String) {
        updateHeader(publisherID)
        toggleViews()

    }

    fun toggleViews(){
        val landingPageView: ConstraintLayout = findViewById(R.id.landingView)
        if(landingPageView.visibility == View.VISIBLE) landingPageView.visibility= View.GONE else landingPageView.visibility= View.VISIBLE

        val publisherListView: ConstraintLayout = findViewById(R.id.publisher_list_view)
        if(publisherListView.visibility == View.VISIBLE) publisherListView.visibility= View.GONE else publisherListView.visibility=View.VISIBLE

        val summaryPageView: ConstraintLayout = findViewById(R.id.summaryView)
        if(summaryPageView.visibility == View.GONE) summaryPageView.visibility=View.VISIBLE else summaryPageView.visibility=View.GONE

        val summaryDataView: ConstraintLayout = findViewById(R.id.summaryDataView)
        if(summaryDataView.visibility == View.GONE) summaryDataView.visibility=View.VISIBLE else summaryDataView.visibility=View.GONE

    }



    override fun onBackPressed() {
        super.onBackPressed()
    }




}