package com.aloktripathy.sunshine;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.aloktripathy.sunshine.core.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ForecastFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class ForecastFragment extends Fragment {
    public final String LOG_TAG = ForecastFragment.class.getSimpleName();
    public final int DEFAULT_DAYS = 14;
    private OnFragmentInteractionListener mListener;
    private String mLocation;
    private static ArrayAdapter<String> forecastAdapter;

    public ForecastFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecast_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refresh();
                return true;
            default:
                super.onOptionsItemSelected(item);
                return true;
        }
    }

    private String getLocation() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        return sp.getString(
                getString(R.string.preference_location_key),
                getString(R.string.preference_location_default)
                );
    }

    private void refresh() {
        mLocation = getLocation();
        new FetchWeatherTask().setDays(DEFAULT_DAYS).setQuery(mLocation).execute();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        String location = getLocation();
        if (!location.equals(mLocation)) {
            refresh();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_forecast, container, false);
        forecastAdapter = new ArrayAdapter<String>(
                getActivity(),                      // The current context (this activity).
                R.layout.list_item_forecast,        // Name of the layout id.
                R.id.list_item_forecast_textview);  // The id of the TextView to populate;                      // The data to populate.

        ListView listview_forecast = (ListView)rootView.findViewById(R.id.listview_forecast);

        // Let's make sure when an item is clicked on the list view we are taking the right action.
        listview_forecast.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MainActivity activity = (MainActivity)getActivity();
                activity.showDetailActivity(parent.getAdapter().getItem(position).toString());
            }
        });
        listview_forecast.setAdapter(forecastAdapter);
        refresh();
        return rootView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public static ArrayAdapter<String> getForecastAdapter() {
        return forecastAdapter;
    }
}

class FetchWeatherTask extends AsyncTask<Void, Void, String> {
    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
    private final String URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
    private final String APPID = "6b9c15d8b77d56224d37f636440b3d28";
    private final String MODE = "json";
    private final String UNITS = "metric";

    private String query;
    private int days;

    @Override
    protected String doInBackground(Void... params) {
        String url = buildUrl();
        return new Request().request(url);
    }

    @Override
    protected void onPostExecute(String jsonResult) {
        String[] weatherData;
        try {
             weatherData = this.getWeatherDataFromJson(jsonResult, getDays());
        } catch (JSONException e) {
            Log.d(LOG_TAG, "onPostExecute: Invalid JSON data" + jsonResult);
            return;
        }
        List<String> weekForecast = new ArrayList<>(Arrays.asList(weatherData));
        ArrayAdapter<String> adapter = ForecastFragment.getForecastAdapter();
        adapter.clear();
        adapter.addAll(weekForecast);
    }

    public String getQuery() {
        return query;
    }

    public FetchWeatherTask setQuery(String query) {
        this.query = query;
        return this;
    }

    public int getDays() {
        return days;
    }

    public FetchWeatherTask setDays(int days) {
        this.days = days;
        return this;
    }

    private String buildUrl() {
        Uri uri = Uri.parse(URL).buildUpon()
                .appendQueryParameter("appid", APPID)
                .appendQueryParameter("mode", MODE)
                .appendQueryParameter("units", UNITS)
                .appendQueryParameter("cnt", String.valueOf(getDays()))
                .appendQueryParameter("q", getQuery())
                .build();
        Log.d(LOG_TAG, "buildUrl: " + uri);
        return uri.toString();
    }

    /* The date/time conversion code is going to be moved outside the asynctask later,
 * so for convenience we're breaking it out into its own method now.
 */
    private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        // OWM returns daily forecasts based upon the local time of the city that is being
        // asked for, which means that we need to know the GMT offset to translate this data
        // properly.

        // Since this data is also sent in-order and the first day is always the
        // current day, we're going to take advantage of that to get a nice
        // normalized UTC date for all of our weather.

        Time dayTime = new Time();
        dayTime.setToNow();

        // we start at the day returned by local time. Otherwise this is a mess.
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        // now we work exclusively in UTC
        dayTime = new Time();

        String[] resultStrs = new String[numDays];
        for(int i = 0; i < weatherArray.length(); i++) {
            // For now, using the format "Day, description, hi/low"
            String day;
            String description;
            String highAndLow;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".
            long dateTime;
            // Cheating to convert this to UTC time, which is what we want anyhow
            dateTime = dayTime.setJulianDay(julianStartDay+i);
            day = getReadableDateString(dateTime);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            highAndLow = formatHighLows(high, low);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;
        }
        return resultStrs;
    }
}
