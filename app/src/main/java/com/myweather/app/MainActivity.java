package com.myweather.app;

import android.app.Activity;
import android.content.Context;
import android.location.*;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import android.content.Intent;



public class MainActivity extends Activity {

    Button getLocationButton;
    private LocationManager LocMan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getLocationButton =  (Button) findViewById(R.id.GetLocation);
        getLocationButton.setOnClickListener(LocationListener);
    }

    View.OnClickListener LocationListener = new View.OnClickListener(){
        @Override
        public void onClick(View v){


                LocMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (LocMan.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                    Toast.makeText(getApplicationContext(), "GPS module is currently working", Toast.LENGTH_SHORT)
                            .show();
                    startActivity(new Intent("com.myweather.Location"));
                }
                else {
                    Toast.makeText(getApplicationContext(), "Please enable GPS！", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, 0);
                }

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
