package com.example.SurfApp;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Created by Q on 05-Dec-15.
 */
public class AmbientService extends Service
{
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Bundle extras = intent.getExtras();

        final String location = (String) extras.get("location");
        final int min_wave = (int) extras.get("min_wave");
        final int max_wave = (int) extras.get("max_wave");

        new Thread()
        {
            public void run()
            {
                findInBackground(min_wave, max_wave, location);
            }
        }.start();

        return START_NOT_STICKY;
    }

    private void findInBackground(int min_wave, int max_wave, String location)
    {
        String package_name = getPackageName();
        boolean flag = false;
        String str = "";
        int[] heights;
        int heightsCount = 0;
        int avg_surf = 0;

        while(true)
        {
            Date now = new Date();
            String timeStamp = new SimpleDateFormat("ddMMyyyyHHmm").format(now);

            if(timeStamp.charAt(timeStamp.length()-1) % 2 != 0)
            {
                flag = false;
            }

            if(timeStamp.charAt(timeStamp.length()-1) % 2 == 0 && !flag)
            {
                flag = true;

                try
                {
                    Document document = Jsoup.connect("http://www.prh.noaa.gov/hnl/pages/SRF.php").get();
                    Elements e = document.select("p");

                    for(Element height : e)
                    {
                        if(height.toString().contains("Surf along " + location))
                        {
                            str = height.toString();
                            String[] arr = str.split("[ \t\n]+");
                            heights = new int[arr.length];

                            heightsCount = 0;
                            for(int j = 0; j < arr.length-1; j++ )
                            {
                                if(arr[j].matches("-?\\d+(\\.\\d+)?")){
                                    heights[heightsCount] = Integer.parseInt(arr[j]);
                                    heightsCount++;
                                }
                            }

                            avg_surf = (heights[0] + heights[1]) / 2;

                            Log.d("test", Integer.toString(heights[0]));
                            Log.d("test", Integer.toString(heights[1]));
                        }
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                if(avg_surf < min_wave)
                {
                    getPackageManager().setComponentEnabledSetting(
                            new ComponentName(package_name, package_name + ".AmbientInterface-red"),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

                    getPackageManager().setComponentEnabledSetting(
                            new ComponentName(package_name, package_name + ".AmbientInterface-yellow"),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

                    getPackageManager().setComponentEnabledSetting(
                            new ComponentName(package_name, package_name + ".AmbientInterface-green"),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
                else if(avg_surf > max_wave)
                {
                    getPackageManager().setComponentEnabledSetting(
                            new ComponentName(package_name, package_name + ".AmbientInterface-yellow"),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

                    getPackageManager().setComponentEnabledSetting(
                            new ComponentName(package_name, package_name + ".AmbientInterface-red"),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

                    getPackageManager().setComponentEnabledSetting(
                            new ComponentName(package_name, package_name + ".AmbientInterface-green"),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
                else if (avg_surf >= min_wave && avg_surf <= max_wave)
                {
                    getPackageManager().setComponentEnabledSetting(
                            new ComponentName(package_name, package_name + ".AmbientInterface-green"),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

                    getPackageManager().setComponentEnabledSetting(
                            new ComponentName(package_name, package_name + ".AmbientInterface-red"),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

                    getPackageManager().setComponentEnabledSetting(
                            new ComponentName(package_name, package_name + ".AmbientInterface-yellow"),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
            }
        }
    }
}
