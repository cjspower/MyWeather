package com.myweather.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.location.LocationManager;
import android.os.*;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class Location extends Activity {

    private static final int ADDR_INFO=1;
    private static final int ADDR_FAIL=2;
    private static final int GET_STATUS=3;
    Button goBackButton;
    Button startButton;
    private boolean status = false;
    private long preTime;
    public static final String TAG = "LocationActivity";
    private android.location.Location location;
    private LocationManager locationManager;

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        Log.i(TAG, "onCreate(Bundle savedInstanceState)");
        goBackButton = (Button) findViewById(R.id.GoBack);
        startButton = (Button) findViewById(R.id.Start);
        textView = (TextView) findViewById(R.id.ViewLocation);
        goBackButton.setOnClickListener(BackListener);
        startButton.setOnClickListener(StartListener);

    }

    android.os.Handler handler = new android.os.Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ADDR_INFO: {
                    textView.append(((Hashtable<String, String>)msg.obj).get("city")+", "+
                                        ((Hashtable<String, String>)msg.obj).get("state")+", "+
                                        ((Hashtable<String, String>)msg.obj).get("country")+"\n");
                    break;
                }
                case ADDR_FAIL:{
                    textView.append((String)msg.obj+"\n");
                    break;
                }
                case GET_STATUS:{
                    textView.append((String)msg.obj+"\n");
                    break;
                }
            }
        }


    };

    View.OnClickListener StartListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!status) {
                status = true;
                startLocationService();
                startButton.setText("Stop");
            } else {
                status = false;
                stopLocationService();
                startButton.setText("Start");
            }
        }
    };

    View.OnClickListener BackListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    public void startLocationService() {
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        String provider = locationManager.getBestProvider(criteria, true);
        android.location.Location location = locationManager.getLastKnownLocation(provider);
        preTime = System.currentTimeMillis();
        updateWithNewLocation(location);
        locationManager.requestLocationUpdates(provider, 5000, 0,
                locationListener);
    }

    public void stopLocationService() {
        if(locationManager != null){
            locationManager.removeUpdates(locationListener);
        }
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(android.location.Location location) {
            Log.i(TAG, "onLocationChanged(Location location)");
            updateWithNewLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            updateWithNewLocation(null);
        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private void updateWithNewLocation(android.location.Location location) {
        Log.i(TAG, "updateWithNewLocation(Location location)");
        String latLongString;

        if (location != null) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();

            long subTime = (System.currentTimeMillis() - preTime) / 1000;
            float v = (subTime == 0 || this.location == null) ? 0 : (this.location
                    .distanceTo(location) / subTime);
            latLongString = "Latitude:" + lat + " Longitude:" + "\n";
            this.location = location;
            preTime = System.currentTimeMillis();
            textView.setText(latLongString);

            GetMyLocationAddress getMyLocationAddress = new GetMyLocationAddress(lat,lng);
            Thread t = new Thread(getMyLocationAddress);
            t.start();

        }

    }


    public class GetMyLocationAddress implements Runnable{

        private double lat;
        private double lng;

        GetMyLocationAddress(double lat, double lng){
            this.lat = lat;
            this.lng = lng;
        }

        @Override
        public void run() {
            String url = "http://api.wunderground.com/auto/wui/geo/GeoLookupXML/index.xml?query="+lat+","+lng;
            HttpGet uri = new HttpGet(url);
            DefaultHttpClient client = new DefaultHttpClient();
            try {
                HttpResponse response = client.execute(uri);
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode()!=200){
                    Message s = new Message();
                    s.what = ADDR_FAIL;
                    s.obj = "HTTP bad status";
                    handler.sendMessage(s);
                }else{
                    Message s = new Message();
                    s.what = GET_STATUS;
                    s.obj = "HTTP good status";
                    handler.sendMessage(s);
                    try {
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document document = builder.parse(response.getEntity().getContent());
                        String country = document.getElementsByTagName("country").item(0).getFirstChild().getNodeValue();
                        String state = document.getElementsByTagName("state").item(0).getFirstChild().getNodeValue();
                        String city = document.getElementsByTagName("city").item(0).getFirstChild().getNodeValue();
                        Hashtable<String, String> stringHashtable = new Hashtable<String, String>();
                        stringHashtable.put("country",country);
                        stringHashtable.put("state",state);
                        stringHashtable.put("city",city);
                        Message j = new Message();
                        j.what = ADDR_INFO;
                        j.obj = stringHashtable;
                        handler.sendMessage(j);




                    }catch (ParserConfigurationException e){
                        e.printStackTrace();
                        s.what = ADDR_FAIL;
                        s.obj = "PraserException";
                        handler.sendMessage(s);
                    }catch (SAXException e){
                        e.printStackTrace();
                        s.what = ADDR_FAIL;
                        s.obj = "SAXException";
                        handler.sendMessage(s);
                    }



                }

            }catch (IOException e){
                e.printStackTrace();
                Message s = new Message();
                s.what = ADDR_FAIL;
                s.obj = "IOException";
                handler.sendMessage(s);
            }


        }
    }

/*    private String getMyLocationAddress(double lat, double lng){
        Geocoder geocoder=new Geocoder(this, Locale.US);
        try{

            List<Address> addresses = geocoder.getFromLocation(lat,lng,1);
            if(addresses != null){

                Address fetchedAddress=addresses.get(0);
                StringBuilder strAddress = new StringBuilder();

                for(int i=0; i<fetchedAddress.getMaxAddressLineIndex(); i++) {
                    strAddress.append(fetchedAddress.getAddressLine(i)).append("\n");
                }

                return strAddress.toString();

            }
            else{
                return "Can't find location";
            }

        }catch (IOException e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Fail to get address",Toast.LENGTH_LONG).show();
        }
        return "IOErr";
    }*/

}
