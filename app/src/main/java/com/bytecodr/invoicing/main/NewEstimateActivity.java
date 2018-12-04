package com.bytecodr.invoicing.main;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
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
import com.bytecodr.invoicing.helper.helper_number;
import com.bytecodr.invoicing.helper.helper_string;
import com.bytecodr.invoicing.model.Client;
import com.bytecodr.invoicing.model.Estimate;
import com.bytecodr.invoicing.model.Invoice;
import com.bytecodr.invoicing.model.Item;
import com.bytecodr.invoicing.network.MySingleton;
import com.bytecodr.invoicing.network.Network;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.rey.material.widget.Spinner;
import com.rey.material.widget.Switch;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NewEstimateActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener
{
    public static final String TAG = "NewEstimateActivity";
    private MaterialDialog progressDialog;
    private JSONObject api_parameter;

    private Estimate currentEstimate;
    private Client currentClient;

    private ArrayList<Client> array_list_clients;
    private String[] array_clients;
    Spinner spinner_client;

    private EditText edit_estimate_number;
    private EditText edit_tax_rate;
    private EditText edit_client_notes;
    private EditText edit_estimate_date;
    private EditText edit_estimate_due_date;

    private TextView text_subtotal_amount;
    private TextView text_tax_amount;
    private TextView text_total_amount;

    private DatePickerDialog datePicker;

    private Switch switch_payment_received;

    private ArrayList<Item> array_list_items;
    private EstimateItemAdapter adapter_item;
    private ListView list_items;

    static final int ITEM_PICKER_TAG = 1;

    private String currency;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /** INVOICE SETUP **/
    private String logoImage;
    private BaseFont bfBold;
    private BaseFont bf;
    private int pageNumber = 0;
    private double Subtotal;
    private double Tax;

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have read or write permission
        int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_estimate);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        SharedPreferences settings = getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE);

        //Means user is not logged in
        if (settings == null || settings.getInt("logged_in", 0) == 0 || settings.getString("api_key", "").equals(""))
        {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= 23) verifyStoragePermissions(this);

        currency = settings.getString(SettingActivity.CURRENCY_SYMBOL_KEY, "$");

        progressDialog = new MaterialDialog.Builder(this)
                .title(R.string.progress_dialog)
                .content(R.string.please_wait)
                .cancelable(false)
                .progress(true, 0).build();

        edit_estimate_number = (EditText)findViewById(R.id.edit_estimate_number);
        edit_tax_rate = (EditText)findViewById(R.id.edit_tax_rate);
        edit_client_notes = (EditText)findViewById(R.id.edit_client_notes);
        edit_estimate_date = (EditText)findViewById(R.id.edit_estimate_date);
        edit_estimate_due_date = (EditText)findViewById(R.id.edit_estimate_due_date);

        text_subtotal_amount = (TextView)findViewById(R.id.text_subtotal_amount);
        text_tax_amount = (TextView)findViewById(R.id.text_tax_amount);
        text_total_amount = (TextView)findViewById(R.id.text_total_amount);

        text_subtotal_amount.setText(currency + helper_number.round(0));
        text_tax_amount.setText(currency + helper_number.round(0));
        text_total_amount.setText(currency + helper_number.round(0));

        switch_payment_received = (Switch)findViewById(R.id.switch_payment_received);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd. MMM yyyy");
        Calendar calendar = Calendar.getInstance();

        datePicker = DatePickerDialog.newInstance(
                NewEstimateActivity.this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePicker.vibrate(false);

        currentEstimate = (Estimate)getIntent().getSerializableExtra("data");
        currentClient = new Client();

        if (currentEstimate != null && currentEstimate.Id > 0)
        {
            edit_estimate_number.setTag(currentEstimate.Id);
            edit_estimate_number.setText(currentEstimate.getEstimateNumberFormatted());
            edit_tax_rate.setText(String.format("%.2f", currentEstimate.TaxRate));
            edit_client_notes.setText(currentEstimate.ClientNote);

            if (currentEstimate.EstimateDate != 0) edit_estimate_date.setText(dateFormat.format(currentEstimate.getEstimateDate()));
            if (currentEstimate.EstimateDueDate != 0) edit_estimate_due_date.setText(dateFormat.format(currentEstimate.getEstimateDueDate()));

            toolbar.setTitle(currentEstimate.getEstimateName());
        }
        else
        {
            edit_estimate_date.setText(dateFormat.format(calendar.getTime()));
            toolbar.setTitle(getResources().getString(R.string.title_activity_new_estimate));
        }

        array_list_clients = new ArrayList<Client>();
        array_list_items = new ArrayList<Item>();

        list_items = (ListView) findViewById(R.id.list_item);
        adapter_item = new EstimateItemAdapter(NewEstimateActivity.this, array_list_items);
        list_items.setAdapter(adapter_item);
        list_items.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

               Item item  = array_list_items.get(position);
               Intent intent = new Intent(NewEstimateActivity.this, ItemPickerActivity.class);
               intent.putExtra("data", item);
               intent.putExtra("position", position);
               startActivityForResult(intent, ITEM_PICKER_TAG);
            }

        });

        spinner_client = (Spinner) findViewById(R.id.spinner_client);

        edit_estimate_date.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String date = edit_estimate_date.getText().toString();
                datePicker.show(getFragmentManager(), "estimate_date");
            }
        });

        edit_estimate_due_date.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String date = edit_estimate_due_date.getText().toString();
                datePicker.show(getFragmentManager(), "estimate_due_date");
            }
        });

        edit_tax_rate.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {   }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)  {    }

            @Override
            public void afterTextChanged(Editable s)
            {
                calculate_total();
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.add_item_button);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NewEstimateActivity.this, ItemPickerActivity.class);
                startActivityForResult(intent, ITEM_PICKER_TAG);
            }
        });

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        api_parameter = new JSONObject();

        try
        {
            api_parameter.put("user_id", settings.getInt("id", 0));
            api_parameter.put("include_logo", 1);

            if (currentEstimate != null && currentEstimate.Id > 0)
            {
                api_parameter.put("include_estimate_lines", currentEstimate.Id);
            }
            else
            {
                api_parameter.put("include_estimate_number", 1);
            }
        }
        catch(JSONException ex) {}

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        RunGetClientService();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        MySingleton.getInstance(this).getRequestQueue().cancelAll(TAG);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_estimate, menu);

        if (currentEstimate != null)
        {
            MenuItem item = menu.findItem(R.id.action_delete);
            if (item != null)
            {
                item.setVisible(true);
            }

            MenuItem item_convert_invoice = menu.findItem(R.id.action_convert_to_invoice);
            if (item_convert_invoice != null)
            {
                item_convert_invoice.setVisible(true);
            }

            item = menu.findItem(R.id.action_download);
            if (item != null)
            {
                item.setVisible(true);
            }

            item = menu.findItem(R.id.action_email);
            if (item != null)
            {
                item.setVisible(true);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save)
        {
            if (isFormValid())
            {
                SharedPreferences settings = getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE);

                try
                {
                    if (isEstimateFormValid())
                    {
                        api_parameter = new JSONObject();
                        api_parameter.put("user_id", settings.getInt("id", 0));

                        Object itemId = edit_estimate_number.getTag();

                        if (!itemId.equals("0"))
                        {
                            api_parameter.put("id", itemId.toString());
                        }

                        api_parameter.put("estimate_number", edit_estimate_number.getText().toString().trim());
                        api_parameter.put("tax_rate", edit_tax_rate.getText().toString().trim());
                        api_parameter.put("client_id", array_list_clients.get(spinner_client.getSelectedItemPosition()).Id);
                        api_parameter.put("notes", edit_client_notes.getText().toString().trim());

                        String estimateDateString = edit_estimate_date.getText().toString().trim();
                        String estimateDueDateString = edit_estimate_due_date.getText().toString().trim();

                        SimpleDateFormat sdf = new SimpleDateFormat("dd. MMM yyyy");
                        Date estimateDate = null;
                        Date estimateDueDate = null;

                        try
                        {
                            estimateDate = sdf.parse(estimateDateString);
                            estimateDueDate = sdf.parse(estimateDueDateString);

                        }
                        catch (ParseException e)  {  }

                        api_parameter.put("estimate_date", (estimateDate != null ? (estimateDate.getTime() / 1000) : null));
                        api_parameter.put("due_date", (estimateDueDate != null ? (estimateDueDate.getTime() / 1000) : null));

                        Gson json = new Gson();

                        api_parameter.put("items", json.toJson(array_list_items).toString());

                        RunCreateEstimateService();
                    }
                }
                catch (JSONException ex)
                {
                    Toast.makeText(NewEstimateActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                }

                return true;
            }
            else
            {
                return false;
            }
        }
        if (id == R.id.action_convert_to_invoice)
        {
            SharedPreferences settings = getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE);
            try
            {
                api_parameter = new JSONObject();
                api_parameter.put("user_id", settings.getInt("id", 0));
                Object itemId = edit_estimate_number.getTag();

                if (!itemId.equals("0"))
                {
                    api_parameter.put("id", itemId.toString());
                }

                RunUpdateEstimateAsInvoicedService();
            }
            catch(JSONException ex) {}

        }
        else if (id == R.id.action_delete)
        {
            new MaterialDialog.Builder(this)
                    .title(R.string.delete)
                    .content(R.string.delete_item)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .cancelable(false)
                    .negativeColorRes(R.color.colorAccent)
                    .positiveColorRes(R.color.colorAccent)
                    .callback(new MaterialDialog.ButtonCallback()
                    {
                        @Override
                        public void onPositive(MaterialDialog dialog)
                        {
                            //Delete
                            SharedPreferences settings = getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE);

                            Object id = edit_estimate_number.getTag();

                            if (!id.equals("0"))
                            {
                                try
                                {
                                    api_parameter = new JSONObject();
                                    api_parameter.put("id", id.toString());
                                    api_parameter.put("user_id", settings.getInt("id", 0));

                                    RunDeleteEstimateService();
                                }
                                catch (JSONException ex)
                                {
                                    Toast.makeText(NewEstimateActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog)
                        {
                            //Cancel
                            dialog.dismiss();

                            if (dialog != null && dialog.isShowing())
                            {
                                // If the response is JSONObject instead of expected JSONArray
                                dialog.dismiss();
                            }
                        }
                    })
                    .show();
        }
        else if (id == R.id.action_download)
        {
            String path = downloadPDF();

            if (path.length() == 0) {
                Toast.makeText(NewEstimateActivity.this, getResources().getString(R.string.pdf_not_created), Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(path)), "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            }
        }
        else if (id == R.id.action_email)
        {
            String path = downloadPDF();

            SharedPreferences settings = getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE);

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{currentClient.Email});

            emailIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(getResources().getString(R.string.estimate_email_subject), currentEstimate.getEstimateNumberFormatted(), settings.getString("firstname", ""), settings.getString("lastname", "")));
            emailIntent.putExtra(Intent.EXTRA_TEXT, String.format(getResources().getString(R.string.estimate_email_text), currentClient.Name,  currentEstimate.getEstimateName(), settings.getString("firstname", ""), settings.getString("lastname", "")));

            emailIntent.setType("text/plain");
            Uri uri = Uri.fromFile(new File(path));
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(emailIntent, "Send Email"));
        }
        else if (id == android.R.id.home) //Handles the back button, to make sure clients fragment is preselected
        {
            Intent intent = new Intent(NewEstimateActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("tab", "estimates");
            startActivity(intent);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean isEstimateFormValid()
    {
        boolean isValid = true;

        if (edit_estimate_number.getText().toString().trim().length() == 0){
            edit_estimate_number.setError(getResources().getString(R.string.estimate_number_required));
            isValid = false;
        }
        else
            edit_estimate_number.setError(null);

        if (spinner_client.getSelectedItemPosition() == -1 || array_list_clients.size() == 0)
        {
            Toast.makeText(NewEstimateActivity.this, getResources().getString(R.string.client_required), Toast.LENGTH_LONG).show();
            isValid = false;
        }

        if (array_list_items.size() == 0)
        {
            Toast.makeText(NewEstimateActivity.this, getResources().getString(R.string.items_required), Toast.LENGTH_LONG).show();
            isValid = false;
        }

        return isValid;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // Check which request we're responding to
        if (requestCode == ITEM_PICKER_TAG) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Item item = (Item)data.getSerializableExtra("data");

                Integer position = data.getIntExtra("position", -1);

                if (position >-1)
                {
                    Item existingItem = array_list_items.get(position);
                    existingItem.Name = item.Name;
                    existingItem.Description = item.Description;
                    existingItem.Rate = item.Rate;
                    existingItem.Quantity = item.Quantity;
                }
                else
                {
                    array_list_items.add(item);
                }

                adapter_item.notifyDataSetChanged();

                calculate_total();

                setListViewHeightBasedOnChildren(list_items);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth)
    {
        if (view.getTag().equals("estimate_date"))
            edit_estimate_date.setText(dayOfMonth + ". " + helper_number.getMonthName(monthOfYear) + " " + year);
        else
            edit_estimate_due_date.setText(dayOfMonth + ". " + helper_number.getMonthName(monthOfYear) + " " + year);
    }

    public void RunGetClientService()
    {
        progressDialog.show();

        JsonObjectRequest postRequest = new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "clients/get", api_parameter, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        try
                        {
                            JSONObject result = ((JSONObject)response.get("data"));
                            JSONArray clients = (JSONArray)result.get("clients");
                            JSONArray estimate_lines = (JSONArray)result.get("estimate_lines");

                            Integer estimate_number = helper_string.optInt(result,"estimate_number");

                            logoImage = helper_string.optString(result, "logo");

                            if (estimate_number > 0)
                            {
                                edit_estimate_number.setText(String.format("%04d", estimate_number));
                            }

                            array_list_clients.clear();
                            array_clients = new String[ clients.length()];

                            Integer selected_client_index = 0;

                            if (clients.length() > 0) {
                                for (int i = 0; i < clients.length(); i++) {
                                    JSONObject obj = clients.getJSONObject(i);

                                    Client client = new Client();

                                    client.Id = obj.optInt("id");
                                    client.UserId = obj.optInt("user_id");
                                    client.Name = helper_string.optString(obj, "name");
                                    client.Email =  helper_string.optString(obj, "email");
                                    client.Address1 = helper_string.optString(obj, "address1");
                                    client.Address2 = helper_string.optString(obj, "address2");
                                    client.City = helper_string.optString(obj, "city");
                                    client.State = helper_string.optString(obj, "state");
                                    client.Postcode = helper_string.optString(obj, "postcode");
                                    client.Country = helper_string.optString(obj, "country");

                                    array_list_clients.add(client);
                                    array_clients[i] = client.Name;

                                    if (currentEstimate != null && currentEstimate.Id > 0 && currentEstimate.ClientId == client.Id) {
                                        selected_client_index = i;
                                        currentClient = client;
                                    }

                                    /*if (obj.optInt("estimate_number") > 0)
                                        estimate_number = obj.optInt("estimate_number");*/
                                }

                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(NewEstimateActivity.this, R.layout.custom_simple_spinner_item, array_clients);
                                spinner_client.setAdapter(adapter);
                                spinner_client.setSelection(selected_client_index);
                            }

                            if (estimate_lines.length() > 0)
                            {
                                for (int i = 0; i < estimate_lines.length(); i++)
                                {
                                    JSONObject obj = estimate_lines.getJSONObject(i);

                                    Item item = new Item();
                                    item.Id = obj.optInt("id");
                                    item.Quantity = obj.optInt("quantity");
                                    item.Name = helper_string.optString(obj, "name");
                                    item.Rate = obj.optDouble("rate");
                                    item.Description = helper_string.optString(obj, "description");

                                    array_list_items.add(item);
                                }

                                calculate_total();
                                setListViewHeightBasedOnChildren(list_items);
                            }
                        }
                        catch(Exception ex)
                        {
                                Toast.makeText(NewEstimateActivity.this, R.string.error_try_again_support, Toast.LENGTH_LONG).show();
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
                                Toast.makeText(NewEstimateActivity.this, json.has("message") ? json.getString("message") : json.getString("error"), Toast.LENGTH_LONG).show();
                            }
                            catch (JSONException e)
                            {
                                Toast.makeText(NewEstimateActivity.this, R.string.error_try_again_support, Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                        {
                            Toast.makeText(NewEstimateActivity.this, error != null && error.getMessage() != null ? error.getMessage() : error.toString(), Toast.LENGTH_LONG).show();
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
        RequestQueue queue = MySingleton.getInstance(NewEstimateActivity.this).getRequestQueue();

        //Used to mark the request, so we can cancel it on our onStop method
        postRequest.setTag(TAG);

        MySingleton.getInstance(NewEstimateActivity.this).addToRequestQueue(postRequest);
    }

    public boolean isFormValid()
    {
        boolean isValid = true;

        if (edit_estimate_number.getText().toString().trim().length() == 0) {
            edit_estimate_number.setError(getResources().getString(R.string.estimate_number_required));
            isValid = false;
        } else
            edit_estimate_number.setError(null);

        if (spinner_client.getSelectedItemPosition() == -1) {
            Toast.makeText(NewEstimateActivity.this, getResources().getString(R.string.client_required), Toast.LENGTH_LONG).show();
            return false;
        }

        return isValid;
    }

    public void calculate_total()
    {
        double subtotal = 0;

        for(int i =0; i < array_list_items.size(); i++)
        {
            Item item = array_list_items.get(i);

            subtotal = subtotal + (item.Rate * item.Quantity);
        }

        double tax_rate = 0;

        try
        {
            if (edit_tax_rate.getText().toString().length() > 0) {
                tax_rate = Double.parseDouble(edit_tax_rate.getText().toString());
            }
        }
        catch(Exception ex){   }

        double tax = (tax_rate * subtotal) / 100;

        text_subtotal_amount.setText(currency + helper_number.round(subtotal));
        text_tax_amount.setText(currency + helper_number.round(tax));
        text_total_amount.setText(currency + helper_number.round(subtotal + tax));
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0) {
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    public class EstimateItemAdapter extends ArrayAdapter<Item>
    {
        private final Context context;
        private final ArrayList<Item> values;
        private String currency;

        public EstimateItemAdapter(Context context, ArrayList<Item> values) {
            super(context, R.layout.layout_estimate_item_row, values);

            this.context = context;
            this.values = values;

            SharedPreferences settings = context.getSharedPreferences(LoginActivity.SESSION_USER, context.MODE_PRIVATE);
            currency = settings.getString(SettingActivity.CURRENCY_SYMBOL_KEY, "$");
        }

        public Item getItem(int position)
        {
            return values.get(position);
        }

        /**
         * Here we go and get our rowlayout.xml file and set the textview text.
         * This happens for every row in your listview.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView = inflater.inflate(R.layout.layout_estimate_item_row, parent, false);

            Item item = values.get(position);

            TextView text_name = (TextView) rowView.findViewById(R.id.text_name);
            TextView text_rate = (TextView) rowView.findViewById(R.id.text_rate);
            TextView text_quantity = (TextView) rowView.findViewById(R.id.text_quantity);
            ImageView image_remove_item = (ImageView) rowView.findViewById(R.id.image_remove_item);
            image_remove_item.setTag(position);

            text_name.setText(item.Name);
            text_rate.setText(currency + String.format( "%.2f", item.Quantity * item.Rate));
            text_quantity.setText(item.Quantity + " x " + String.format( "%.2f", item.Rate));

            image_remove_item.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    array_list_items.remove(Integer.parseInt(v.getTag().toString()));
                    adapter_item.notifyDataSetChanged();
                    calculate_total();
                    setListViewHeightBasedOnChildren(list_items);
                }
            });

            return rowView;
        }
    }

    public String downloadPDF()
    {
        return createPDF(currentEstimate.getEstimateName() + ".pdf");
    }

    private Integer address1_start = 750;
    private Integer address2_start = 660;
    private Integer address_height = 12;

    private Integer line_heading_start = 560;
    private Integer line_item_start = 535;
    private Integer line_item_height = 28;

    private BaseColor color_light_grey = new BaseColor(227,227,227);
    private BaseColor color_invoice_header_background = new BaseColor(244,67,54);

    private String createPDF (String pdfFilename){

        pageNumber = 0;
        Subtotal = 0;
        Tax = 0;
        Document doc = new Document();
        PdfWriter docWriter = null;
        initializeFonts();

        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + pdfFilename;

        try {
            docWriter = PdfWriter.getInstance(doc, new FileOutputStream(path));
            doc.addAuthor("Bytecodr");
            doc.addCreationDate();
            doc.addProducer();
            doc.addTitle(currentEstimate.getEstimateName());
            doc.setPageSize(PageSize.LETTER);

            doc.open();
            PdfContentByte cb = docWriter.getDirectContent();

            boolean beginPage = true;
            int y = 0;

            for(int i=0; i < array_list_items.size(); i++ ){
                if(beginPage){
                    beginPage = false;
                    generateLayout(doc, cb);
                    generateHeader(doc, cb);
                    y = line_item_start;
                }
                generateDetail(doc, cb, i, y);
                y = y - line_item_height;
                if(y < 50){
                    printPageNumber(cb);
                    doc.newPage();
                    beginPage = true;
                }
            }

            Tax = (currentEstimate.TaxRate * Subtotal) / 100;

            //This is the last item (notes). If it's over page, put it all on the next page.
            if ((y - 115) < 50)
            {
                printPageNumber(cb);
                doc.newPage();
                y = address1_start;
            }

            createText(cb, 460, y - 10, getResources().getString(R.string.subtotal), 9, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 568, y - 10, helper_number.round(Subtotal), 9, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);

            createText(cb, 460, y - 35, getResources().getString(R.string.tax) + "(" + currentEstimate.TaxRate+ "%)", 9, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 568, y - 35, helper_number.round(Tax), 9, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);

            LineSeparator lineSeparator = new LineSeparator();

            lineSeparator.setLineColor(BaseColor.BLACK);
            lineSeparator.drawLine(cb, 400, 575, y - 50);

            createText(cb, 460, y - 70, getResources().getString(R.string.balance_due), 9, bfBold, BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 568, y - 70, currency + helper_number.round(Tax + Subtotal), 9, bfBold, BaseColor.BLACK, Element.ALIGN_RIGHT);

            if (currentEstimate.ClientNote.length() > 0) {
                createText(cb, 30, y - 130, getResources().getString(R.string.notes), 10, bf, BaseColor.GRAY, Element.ALIGN_LEFT);
                createText(cb, 30, y - 145, currentEstimate.ClientNote, 9, bf, BaseColor.BLACK, Element.ALIGN_LEFT);
            }

            printPageNumber(cb);

        }
        catch (DocumentException ex)
        {
            ex.printStackTrace();
            return "";
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return "";
        }
        finally
        {
            if (doc != null)
            {
                doc.close();
            }
            if (docWriter != null)
            {
                docWriter.close();
            }
        }

        return path;
    }

    private void generateLayout(Document doc, PdfContentByte cb)  {

        try {
            Rectangle rec = new Rectangle(30,line_heading_start + 15,580,line_heading_start - 8);
            rec.setBackgroundColor(color_invoice_header_background);
            cb.rectangle(rec);

            // Estimate Detail box Text Headings
            createText(cb, 40, line_heading_start, "#", 9, bf, BaseColor.WHITE, Element.ALIGN_LEFT);
            createText(cb, 70, line_heading_start, getResources().getString(R.string.item), 9, bf, BaseColor.WHITE, Element.ALIGN_LEFT);
            createText(cb, 400, line_heading_start, getResources().getString(R.string.rate), 9, bf, BaseColor.WHITE, Element.ALIGN_RIGHT);
            createText(cb, 460, line_heading_start, getResources().getString(R.string.quantity), 9, bf, BaseColor.WHITE, Element.ALIGN_RIGHT);
            createText(cb, 568, line_heading_start, getResources().getString(R.string.amount), 9, bf, BaseColor.WHITE, Element.ALIGN_RIGHT);
        }
        catch (Exception ex){
            ex.printStackTrace();
        }

    }

    private void generateHeader(Document doc, PdfContentByte cb)  {

        try {

            SharedPreferences settings = getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE);

            Integer address_startX = 30;

            String address = settings.getString(SettingActivity.ADDRESS_SYMBOL_KEY, "");

            String[] myAddress = address.length() > 0 ? address.split("\\|\\#", -1) : new String [0]; //new String[] {settings.getString("address1", ""), settings.getString("address2", ""),settings.getString("city", ""),settings.getString("state", ""),settings.getString("postcode", ""),settings.getString("country", "")};

            createText(cb, address_startX, address1_start, settings.getString("firstname", "") + " " + settings.getString("lastname", ""), 8, bf, BaseColor.BLACK, Element.ALIGN_LEFT);

            Integer index = 1;
            for(Integer i = 0; i < myAddress.length; i++)
            {
                String value = myAddress[i].trim();

                if (value.length() > 0) {
                    if (i == 3 && myAddress[4].trim().length() > 0)
                    {
                        value += " " + myAddress[4].trim();
                        i++;
                    }

                    createText(cb, address_startX, address1_start - (index) * address_height, value, 8, bf, BaseColor.BLACK, Element.ALIGN_LEFT);
                    index++;
                }
            }

            String[] clientAddress = new String[] {currentClient.Address1, currentClient.Address2,currentClient.City,currentClient.State,currentClient.Postcode,currentClient.Country};

            createText(cb, address_startX, address2_start, currentClient.Name, 8, bf, BaseColor.BLACK, Element.ALIGN_LEFT);

            Integer clientIndex = 1;
            for(Integer i = 0; i < clientAddress.length; i++)
            {
                String value = clientAddress[i];

                if (value != null && value.length() > 0) {
                    if (i == 3 && clientAddress[4].length() > 0)
                    {
                        value += " " + clientAddress[4];
                        i++;
                    }

                    createText(cb, address_startX, address2_start - clientIndex * address_height, value, 8, bf, BaseColor.BLACK, Element.ALIGN_LEFT);
                    clientIndex++;
                }
            }

            clientIndex = clientIndex - 1;

            createText(cb, 350, address1_start - 10, getResources().getString(R.string.estimate_capitalized), 20, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);

            if (!logoImage.isEmpty())
            {
                Image image = Image.getInstance(Base64.decode(logoImage, Base64.DEFAULT));
                float width = image.getScaledWidth();
                float height = image.getScaledHeight();
                image.setAlignment(Element.ALIGN_RIGHT);
                image.setAbsolutePosition(410 + (180 - width), address1_start - height + 10);
                cb.addImage(image);
            }

            createText(cb, 460, address2_start, getResources().getString(R.string.estimate) + " #:", 10, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 580, address2_start, currentEstimate.getEstimateNumberFormatted() + "", 10, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");

            createText(cb, 460, address2_start - (address_height * 1) - 5, getResources().getString(R.string.estimate_date), 10, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 580, address2_start - (address_height * 1) - 5, dateFormat.format(helper_number.unixToDate(currentEstimate.EstimateDate)), 10, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);

            if (currentEstimate.EstimateDueDate > 0)
            {
                createText(cb, 460, address2_start - (address_height * 2) - 10, getResources().getString(R.string.estimate_due_date), 10, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
                createText(cb, 580, address2_start - (address_height * 2) - 10, dateFormat.format(helper_number.unixToDate(currentEstimate.EstimateDueDate)), 10, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
            }

        }

        catch (Exception ex){
            ex.printStackTrace();
        }

    }

    private void generateDetail(Document doc, PdfContentByte cb, int index, int y)  {
        Item item = array_list_items.get(index);

        if (item == null) return;

        try {
            createText(cb, 40, y, String.valueOf(index + 1), 8, bf, BaseColor.BLACK, Element.ALIGN_LEFT);
            createText(cb, 70, y, item.Name, 8, bf, BaseColor.BLACK, Element.ALIGN_LEFT);
            createText(cb, 400, y, helper_number.round(item.Rate), 8, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);

            createText(cb, 460, y, helper_number.round(item.Quantity), 8, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);
            createText(cb, 568, y, helper_number.round(item.getTotal()), 8, bf, BaseColor.BLACK, Element.ALIGN_RIGHT);

            Subtotal += item.getTotal();

            LineSeparator lineSeparator = new LineSeparator();

            lineSeparator.setLineColor(color_light_grey);
            lineSeparator.drawLine(cb, 30, 580, y - 10);
        }

        catch (Exception ex){
            ex.printStackTrace();
        }

    }

    private void createHeadings(PdfContentByte cb, float x, float y, String text)
    {
        cb.beginText();
        cb.setFontAndSize(bf, 8);
        cb.setTextMatrix(x,y);
        cb.showText(text.trim());
        cb.endText();
    }

    private void createText(PdfContentByte cb, float x, float y, String text, Integer size, BaseFont font, BaseColor color, Integer alignment)
    {
        Font fnt = new Font(font, size);
        fnt.setColor(color);

        Phrase phrase = new Phrase(text, fnt);
        ColumnText.showTextAligned(cb, alignment, phrase, x, y, 0);

    }

    private void  printPageNumber(PdfContentByte cb)
    {
        cb.beginText();
        cb.setFontAndSize(bfBold, 8);
        cb.showTextAligned(PdfContentByte.ALIGN_RIGHT, getResources().getString(R.string.page_no) + (pageNumber+1), 570 , 25, 0);
        cb.endText();

        pageNumber++;
    }

    private void createContent(PdfContentByte cb, float x, float y, String text, int align)
    {
        cb.beginText();
        cb.setFontAndSize(bf, 8);
        cb.showTextAligned(align, text.trim(), x, y, 0);
        cb.endText();
    }

    private void initializeFonts()
    {
        try {
            bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);

        } catch (DocumentException ex) {
            //ex.printStackTrace();
        } catch (IOException ex) {
            //ex.printStackTrace();
        }
    }

    public void RunCreateEstimateService()
    {
        progressDialog.show();

        JsonObjectRequest postRequest = new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "estimates/create", api_parameter, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            // If the response is JSONObject instead of expected JSONArray
                            progressDialog.dismiss();
                        }

                        Intent intent = new Intent(NewEstimateActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("tab", "estimates");
                        startActivity(intent);
                        finish();
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
                                Toast.makeText(NewEstimateActivity.this, json.has("message") ? json.getString("message") : json.getString("error"), Toast.LENGTH_LONG).show();
                            }
                            catch (JSONException e)
                            {
                                Toast.makeText(NewEstimateActivity.this, R.string.error_try_again_support, Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                        {
                            Toast.makeText(NewEstimateActivity.this, error != null && error.getMessage() != null ? error.getMessage() : error.toString(), Toast.LENGTH_LONG).show();
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
        RequestQueue queue = MySingleton.getInstance(this.getApplicationContext()).getRequestQueue();

        //Used to mark the request, so we can cancel it on our onStop method
        postRequest.setTag(TAG);

        MySingleton.getInstance(this).addToRequestQueue(postRequest);
    }

    public void RunUpdateEstimateAsInvoicedService()
    {
        progressDialog.show();

        JsonObjectRequest postRequest = new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "estimates/invoiced", api_parameter, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            // If the response is JSONObject instead of expected JSONArray
                            progressDialog.dismiss();
                        }

                        Invoice invoice = new Invoice();
                        invoice.TaxRate = Double.parseDouble(edit_tax_rate.getText().toString().trim());
                        invoice.ClientId = array_list_clients.get(spinner_client.getSelectedItemPosition()).Id;
                        invoice.ClientNote = edit_client_notes.getText().toString().trim();

                        Intent intent = new Intent(NewEstimateActivity.this, NewInvoiceActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("data", invoice);
                        intent.putExtra("items", array_list_items);

                        startActivity(intent);
                        finish();
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
                                Toast.makeText(NewEstimateActivity.this, json.has("message") ? json.getString("message") : json.getString("error"), Toast.LENGTH_LONG).show();
                            }
                            catch (JSONException e)
                            {
                                Toast.makeText(NewEstimateActivity.this, R.string.error_try_again_support, Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                        {
                            Toast.makeText(NewEstimateActivity.this, error != null && error.getMessage() != null ? error.getMessage() : error.toString(), Toast.LENGTH_LONG).show();
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
        RequestQueue queue = MySingleton.getInstance(this.getApplicationContext()).getRequestQueue();

        //Used to mark the request, so we can cancel it on our onStop method
        postRequest.setTag(TAG);

        MySingleton.getInstance(this).addToRequestQueue(postRequest);
    }

    public void RunDeleteEstimateService()
    {
        progressDialog.show();

        JsonObjectRequest postRequest = new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "estimates/delete", api_parameter, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            // If the response is JSONObject instead of expected JSONArray
                            progressDialog.dismiss();
                        }

                        Intent intent = new Intent(NewEstimateActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("tab", "estimates");
                        startActivity(intent);
                        finish();
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
                                Toast.makeText(NewEstimateActivity.this, json.has("message") ? json.getString("message") : json.getString("error"), Toast.LENGTH_LONG).show();
                            }
                            catch (JSONException e)
                            {
                                Toast.makeText(NewEstimateActivity.this, R.string.error_try_again_support, Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                        {
                            Toast.makeText(NewEstimateActivity.this, error != null && error.getMessage() != null ? error.getMessage() : error.toString(), Toast.LENGTH_LONG).show();
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
        RequestQueue queue = MySingleton.getInstance(this.getApplicationContext()).getRequestQueue();

        //Used to mark the request, so we can cancel it on our onStop method
        postRequest.setTag(TAG);

        MySingleton.getInstance(this).addToRequestQueue(postRequest);
    }
}
