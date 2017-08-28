/*
 * <!--
 *   ~ Copyright (c) 2017. ThanksMister LLC
 *   ~
 *   ~ Licensed under the Apache License, Version 2.0 (the "License");
 *   ~ you may not use this file except in compliance with the License. 
 *   ~ You may obtain a copy of the License at
 *   ~
 *   ~ http://www.apache.org/licenses/LICENSE-2.0
 *   ~
 *   ~ Unless required by applicable law or agreed to in writing, software distributed 
 *   ~ under the License is distributed on an "AS IS" BASIS, 
 *   ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *   ~ See the License for the specific language governing permissions and 
 *   ~ limitations under the License.
 *   -->
 */

package com.thanksmister.iot.mqtt.alarmpanel.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.thanksmister.iot.mqtt.alarmpanel.BaseFragment;
import com.thanksmister.iot.mqtt.alarmpanel.R;
import com.thanksmister.iot.mqtt.alarmpanel.network.DarkSkyRequest;
import com.thanksmister.iot.mqtt.alarmpanel.network.model.Daily;
import com.thanksmister.iot.mqtt.alarmpanel.ui.modules.ScreenSaverModule;
import com.thanksmister.iot.mqtt.alarmpanel.ui.modules.WeatherModule;
import com.thanksmister.iot.mqtt.alarmpanel.utils.WeatherUtils;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

import static android.os.Looper.getMainLooper;
import static java.lang.Math.round;

public class InformationFragment extends BaseFragment {

    public static final long DATE_INTERVAL = 3600000; // 1 hour

    @Bind(R.id.temperatureText)
    TextView temperatureText;

    @Bind(R.id.outlookText)
    TextView outlookText;
    
    @Bind(R.id.conditionImage)
    ImageView conditionImage;

    @Bind(R.id.dateText)
    TextView dateText;
    
    @Bind(R.id.weatherLayout)
    View weatherLayout;
    
    @OnClick(R.id.weatherLayout)
    void weatherLayoutClicked() {
        if(extendedDaily != null) {
            showExtendedForecastDialog(extendedDaily);
        }
    }
    
    private WeatherModule weatherModule;
    private Daily extendedDaily;
    
    public InformationFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     */
    public static InformationFragment newInstance() {
        return new InformationFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        // start the clock
        final Handler someHandler = new Handler(getMainLooper());
        someHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String currentDateString = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(new Date());
                dateText.setText(currentDateString);
                someHandler.postDelayed(this, DATE_INTERVAL);
            }
        }, 10);

        // start the weather module
        if(getConfiguration().showWeatherModule() && getConfiguration().getDarkSkyKey() != null
                && getConfiguration().getLatitude() != null && getConfiguration().getLongitude() != null) {
            connectWeatherModule();
        } else {
            weatherLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_information, container, false);
        ButterKnife.bind(this, fragmentView);
        return fragmentView;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // this picks up changes made in the settings and connects weather if needed
        if(getConfiguration().showWeatherModule() && getConfiguration().getDarkSkyKey() != null
                && getConfiguration().getLatitude() != null && getConfiguration().getLongitude() != null) {
            connectWeatherModule();
        } else {
            disconnectWeatherModule();
            weatherLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        disconnectWeatherModule();
    }
    
    private void disconnectWeatherModule() {
        if(weatherModule != null) {
            weatherModule.cancelDarkSkyHourlyForecast();
        }
    }
    
    private void connectWeatherModule() {
        if(weatherModule == null) {
            weatherModule = new WeatherModule();
        }

        final String apiKey = getConfiguration().getDarkSkyKey();
        final String units = getConfiguration().getWeatherUnits();
        Timber.d("units: " + units);
        final String lat = getConfiguration().getLatitude();
        final String lon = getConfiguration().getLongitude();
        weatherModule.getDarkSkyHourlyForecast(apiKey, units, lat, lon, new WeatherModule.ForecastListener() {
            @Override
            public void onWeatherToday(String icon, double temperature, String summary) {
                weatherLayout.setVisibility(View.VISIBLE);
                outlookText.setText(summary);
                String displayUnits = (units.equals( DarkSkyRequest.UNITS_US)? getString(R.string.text_f): getString(R.string.text_c));
                temperatureText.setText(getString(R.string.text_temperature, String.valueOf(round(temperature)), displayUnits));
                conditionImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), WeatherUtils.getIconForWeatherCondition(icon), getActivity().getTheme()));
            }

            @Override
            public void onExtendedDaily(Daily daily) {
                extendedDaily = daily;
            }

            @Override
            public void onShouldTakeUmbrella(boolean takeUmbrella) {
                if(takeUmbrella) {
                    conditionImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_rain_umbrella, getActivity().getTheme()));
                }
            }
        });
    }
}