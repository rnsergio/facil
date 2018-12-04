package com.bytecodr.invoicing.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bytecodr.invoicing.R;
import com.bytecodr.invoicing.helper.helper_string;
import com.bytecodr.invoicing.model.Invoice;
import com.bytecodr.invoicing.network.MySingleton;
import com.bytecodr.invoicing.network.Network;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HomeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment
{
    private MaterialDialog progressDialog;
    private JSONObject api_parameter;

    private TextView text_paid;
    private TextView text_unpaid;

    private String currency;

    private BarChart chart;

    private OnFragmentInteractionListener mListener;

    public HomeFragment()
    {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2)
    {
        HomeFragment fragment = new HomeFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        progressDialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.progress_dialog)
                .content(R.string.please_wait)
                .cancelable(false)
                .progress(true, 0).build();

        SharedPreferences settings = getActivity().getSharedPreferences(LoginActivity.SESSION_USER, getActivity().MODE_PRIVATE);
        currency = settings.getString(SettingActivity.CURRENCY_SYMBOL_KEY, "$");

        api_parameter = new JSONObject();

        try
        {
            api_parameter.put("user_id", settings.getInt("id", 0));
        }
        catch(JSONException ex) {}

        RunGetInvoiceService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        text_paid = (TextView) view.findViewById(R.id.text_paid);
        text_unpaid    = (TextView) view.findViewById(R.id.text_unpaid);

        chart = (BarChart) view.findViewById(R.id.chart);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.setDescription("");

        AdView mAdView = (AdView) view.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri)
    {
        if (mListener != null)
        {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener)
        {
            mListener = (OnFragmentInteractionListener) context;
        } else
        {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        //Checks to make sure fragment is still attached to activity
        if (isAdded())
        {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_activity_home);
            NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener
    {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void RunGetInvoiceService()
    {
        progressDialog.show();

        JsonObjectRequest postRequest = new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "invoices/get", api_parameter, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        try
                        {
                            Calendar calendar = Calendar.getInstance(); // this takes current date
                            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
                            calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
                            calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
                            calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));

                            //Getting first of the last 4 months
                            long monthStartDate = calendar.getTimeInMillis() / 1000;

                            calendar.add(Calendar.MONTH, -1);
                            long prevMonth1StartDate = calendar.getTimeInMillis() / 1000;

                            calendar.add(Calendar.MONTH, -1);
                            long prevMonth2StartDate = calendar.getTimeInMillis() / 1000;

                            calendar.add(Calendar.MONTH, -1);
                            long prevMonth3StartDate = calendar.getTimeInMillis() / 1000;

                            calendar.add(Calendar.MONTH, -1);
                            long prevMonth4StartDate = calendar.getTimeInMillis() / 1000;

                            calendar.add(Calendar.MONTH, 4);
                            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                            calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY));
                            calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE));
                            calendar.set(Calendar.SECOND, calendar.getActualMaximum(Calendar.SECOND));

                            long monthEndDate = calendar.getTimeInMillis() / 1000;

                            calendar.add(Calendar.MONTH, -1);
                            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                            long prevMonth1EndDate = calendar.getTimeInMillis() / 1000;
                            String prevMonth1Name = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault() );

                            calendar.add(Calendar.MONTH, -1);
                            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                            long prevMonth2EndDate = calendar.getTimeInMillis() / 1000;
                            String prevMonth2Name = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault() );

                            calendar.add(Calendar.MONTH, -1);
                            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                            long prevMonth3EndDate = calendar.getTimeInMillis() / 1000;
                            String prevMonth3Name = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault() );

                            calendar.add(Calendar.MONTH, -1);
                            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                            long prevMonth4EndDate = calendar.getTimeInMillis() / 1000;
                            String prevMonth4Name = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault() );

                            double paid_total = 0;
                            double unpaid_total = 0;
                            double prevMonth1_total = 0;
                            double prevMonth2_total = 0;
                            double prevMonth3_total = 0;
                            double prevMonth4_total = 0;

                            JSONObject result = ((JSONObject)response.get("data"));
                            JSONArray invoices = (JSONArray)result.get("invoices");

                            for (int i = 0; i < invoices.length(); i++) {
                                JSONObject obj = invoices.getJSONObject(i);

                                Invoice invoice = new Invoice();

                                invoice.Id = obj.optInt("id");
                                invoice.UserId = obj.optInt("user_id");

                                invoice.InvoiceNumber = obj.getInt("invoice_number");
                                invoice.ClientName = helper_string.optString(obj, "client_name");
                                invoice.ClientId = obj.getInt("client_id");
                                invoice.ClientNote = helper_string.optString(obj, "notes");
                                invoice.InvoiceDate = obj.optInt("invoice_date", 0);
                                invoice.InvoiceDueDate = obj.optInt("due_date", 0);
                                invoice.TaxRate = obj.getDouble("tax_rate");
                                invoice.TotalMoney = obj.getDouble("total");
                                invoice.IsPaid = (obj.getInt("is_paid") == 1 ? true : false);

                                invoice.Created = obj.optInt("created_on", 0);
                                invoice.Updated = obj.optInt("updated_on", 0);

                                if (invoice.InvoiceDate >= monthStartDate && invoice.InvoiceDate <= monthEndDate)
                                {
                                    if (invoice.IsPaid)
                                        paid_total += invoice.TotalMoney;
                                    else
                                        unpaid_total += invoice.TotalMoney;
                                }

                                //Only invoices marked as PAID will be added to the chart
                                if (invoice.IsPaid)
                                {
                                    if (invoice.InvoiceDate >= prevMonth1StartDate && invoice.InvoiceDate <= prevMonth1EndDate)
                                        prevMonth1_total += invoice.TotalMoney;

                                    if (invoice.InvoiceDate >= prevMonth2StartDate && invoice.InvoiceDate <= prevMonth2EndDate)
                                        prevMonth2_total += invoice.TotalMoney;

                                    if (invoice.InvoiceDate >= prevMonth3StartDate && invoice.InvoiceDate <= prevMonth3EndDate)
                                        prevMonth3_total += invoice.TotalMoney;

                                    if (invoice.InvoiceDate >= prevMonth4StartDate && invoice.InvoiceDate <= prevMonth4EndDate)
                                        prevMonth4_total += invoice.TotalMoney;
                                }
                            }

                            text_paid.setText(currency + String.format("%.2f", paid_total));
                            text_unpaid.setText(currency + String.format("%.2f", unpaid_total));


                            ArrayList<BarEntry> valsComp1 = new ArrayList<BarEntry>();
                            ArrayList<BarEntry> valsComp2 = new ArrayList<BarEntry>();
                            ArrayList<BarEntry> valsComp3 = new ArrayList<BarEntry>();
                            ArrayList<BarEntry> valsComp4 = new ArrayList<BarEntry>();

                            valsComp1.add(new BarEntry((float)prevMonth4_total, 0));
                            valsComp2.add(new BarEntry((float)prevMonth3_total, 1));
                            valsComp3.add(new BarEntry((float)prevMonth2_total, 2));
                            valsComp4.add(new BarEntry((float)prevMonth1_total, 3));

                            BarDataSet setComp1 = new BarDataSet(valsComp4, "");
                            setComp1.setAxisDependency(YAxis.AxisDependency.LEFT);
                            setComp1.setColors(new int[] { R.color.colorPrimaryLight, R.color.colorPrimaryLight, R.color.colorPrimaryLight, R.color.colorPrimaryLight }, getActivity());

                            BarDataSet setComp2 = new BarDataSet(valsComp3, "");
                            setComp2.setAxisDependency(YAxis.AxisDependency.LEFT);
                            setComp2.setColors(new int[] { R.color.colorPrimaryLight, R.color.colorPrimaryLight, R.color.colorPrimaryLight, R.color.colorPrimaryLight }, getActivity());

                            BarDataSet setComp3 = new BarDataSet(valsComp2, "");
                            setComp3.setAxisDependency(YAxis.AxisDependency.LEFT);
                            setComp3.setColors(new int[] { R.color.colorPrimaryLight, R.color.colorPrimaryLight, R.color.colorPrimaryLight, R.color.colorPrimaryLight }, getActivity());

                            BarDataSet setComp4 = new BarDataSet(valsComp1, "");
                            setComp4.setAxisDependency(YAxis.AxisDependency.LEFT);
                            setComp4.setColors(new int[] { R.color.colorPrimaryLight, R.color.colorPrimaryLight, R.color.colorPrimaryLight, R.color.colorPrimaryLight }, getActivity());

                            ArrayList<IBarDataSet> dataSets = new ArrayList<IBarDataSet>();
                            dataSets.add(setComp4);
                            dataSets.add(setComp3);
                            dataSets.add(setComp2);
                            dataSets.add(setComp1);

                            ArrayList<String> xVals = new ArrayList<String>();
                            xVals.add(prevMonth4Name); xVals.add(prevMonth3Name); xVals.add(prevMonth2Name); xVals.add(prevMonth1Name);

                            BarData data = new BarData(xVals, dataSets);
                            chart.setData(data);
                            chart.invalidate();
                        }
                        catch(Exception ex)
                        {
                            if (isAdded()) {
                                Toast.makeText(getContext(), R.string.error_try_again_support, Toast.LENGTH_LONG).show();
                            }
                        }

                        if (progressDialog != null && progressDialog.isShowing()) {
                            // If the response is JSONObject instead of expected JSONArray
                            progressDialog.dismiss();
                        }
                    }
                }, new Response.ErrorListener()
                {

                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        // TODO Auto-generated method stub
                        if (progressDialog != null && progressDialog.isShowing()) {
                            // If the response is JSONObject instead of expected JSONArray
                            progressDialog.dismiss();
                        }

                        NetworkResponse response = error.networkResponse;
                        if (response != null && response.data != null)
                        {
                            try
                            {
                                JSONObject json = new JSONObject(new String(response.data));
                                Toast.makeText(getContext(), json.has("message") ? json.getString("message") : json.getString("error"), Toast.LENGTH_LONG).show();
                            }
                            catch (JSONException e)
                            {
                                Toast.makeText(getContext(), R.string.error_try_again_support, Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                        {
                            Toast.makeText(getContext(), error != null && error.getMessage() != null ? error.getMessage() : error.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                })
        {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError
            {
                Map<String, String> params = new HashMap<String, String>();
                params.put("X-API-KEY", MainActivity.api_key);
                return params;
            }
        };

        // Get a RequestQueue
        RequestQueue queue = MySingleton.getInstance(getContext()).getRequestQueue();

        //Used to mark the request, so we can cancel it on our onStop method
        postRequest.setTag(MainActivity.TAG);

        MySingleton.getInstance(getContext()).addToRequestQueue(postRequest);
    }
}
