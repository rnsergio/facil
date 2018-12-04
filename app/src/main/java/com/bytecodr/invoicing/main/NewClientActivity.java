package com.bytecodr.invoicing.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
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
import com.bytecodr.invoicing.model.Client;
import com.bytecodr.invoicing.network.MySingleton;
import com.bytecodr.invoicing.network.Network;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class NewClientActivity extends AppCompatActivity
{
    public static final String TAG = "NewClientActivity";
    private MaterialDialog progressDialog;
    private JSONObject api_parameter;

    private Client currentItem;

    private EditText edit_name;
    private EditText edit_email;
    private EditText edit_address1;
    private EditText edit_address2;
    private EditText edit_city;
    private EditText edit_state;
    private EditText edit_postcode;
    private EditText edit_country;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_client);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

        progressDialog = new MaterialDialog.Builder(this)
                .title(R.string.progress_dialog)
                .content(R.string.please_wait)
                .cancelable(false)
                .progress(true, 0).build();

        edit_name = (EditText)findViewById(R.id.edit_name);
        edit_email = (EditText)findViewById(R.id.edit_email);
        edit_address1 = (EditText)findViewById(R.id.edit_address1);
        edit_address2 = (EditText)findViewById(R.id.edit_address2);
        edit_city = (EditText)findViewById(R.id.edit_city);
        edit_state = (EditText)findViewById(R.id.edit_state);
        edit_postcode = (EditText)findViewById(R.id.edit_postcode);
        edit_country = (EditText)findViewById(R.id.edit_country);

        currentItem = (Client)getIntent().getSerializableExtra("data");

        if (currentItem != null && currentItem.Id > 0)
        {
            edit_name.setTag(currentItem.Id);
            edit_name.setText(currentItem.Name);
            edit_email.setText(currentItem.Email);
            edit_address1.setText(currentItem.Address1);
            edit_address2.setText(currentItem.Address2);
            edit_city.setText(currentItem.City);
            edit_state.setText(currentItem.State);
            edit_postcode.setText(currentItem.Postcode);
            edit_country.setText(currentItem.Country);

            toolbar.setTitle(currentItem.Name);
        }
        else
        {
            toolbar.setTitle(getResources().getString(R.string.title_activity_new_client));
        }

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
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
        getMenuInflater().inflate(R.menu.menu_new_client, menu);

        if (currentItem != null)
        {
            MenuItem item = menu.findItem(R.id.action_delete);
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
                    api_parameter = new JSONObject();
                    api_parameter.put("user_id", settings.getInt("id", 0));

                    Object itemId = edit_name.getTag();

                    if (!itemId.equals("0"))
                    {
                        api_parameter.put("id", itemId.toString());
                    }

                    api_parameter.put("name", edit_name.getText().toString().trim());
                    api_parameter.put("email", edit_email.getText().toString().trim());
                    api_parameter.put("address1", edit_address1.getText().toString().trim());
                    api_parameter.put("address2", edit_address2.getText().toString().trim());
                    api_parameter.put("city", edit_city.getText().toString().trim());
                    api_parameter.put("state", edit_state.getText().toString().trim());
                    api_parameter.put("postcode", edit_postcode.getText().toString().trim());
                    api_parameter.put("country", edit_country.getText().toString().trim());

                    RunCreateClientService();
                }
                catch (JSONException ex)
                {
                    Toast.makeText(NewClientActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                }

                return true;
            }
            else
            {
                return false;
            }
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

                            Object id = edit_name.getTag();

                            if (!id.equals("0"))
                            {
                                try
                                {
                                    api_parameter = new JSONObject();
                                    api_parameter.put("id", id.toString());
                                    api_parameter.put("user_id", settings.getInt("id", 0));

                                    RunDeleteClientService();
                                }
                                catch (JSONException ex)
                                {
                                    Toast.makeText(NewClientActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
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
        else if (id == android.R.id.home) //Handles the back button, to make sure clients fragment is preselected
        {
            Intent intent = new Intent(NewClientActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("tab", "clients");
            startActivity(intent);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean isFormValid()
    {
        boolean isValid = true;

        if (edit_name.getText().toString().trim().length() == 0){
            edit_name.setError("Name required");
            isValid = false;
        }
        else
        {
            edit_name.setError(null);
        }

        String email = edit_email.getText().toString().trim();

        if (email.length() > 0)
        {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            {
                edit_email.setError("Valid email required");
                isValid = false;
            }
            else
            {
                edit_email.setError(null);
            }
        }
        else
        {
            edit_email.setError(null);
        }

        return isValid;
    }

    public void RunCreateClientService()
    {
        progressDialog.show();

        JsonObjectRequest postRequest = new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "clients/create", api_parameter, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            // If the response is JSONObject instead of expected JSONArray
                            progressDialog.dismiss();
                        }

                        Intent intent = new Intent(NewClientActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("tab", "clients");
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
                                Toast.makeText(NewClientActivity.this, json.has("message") ? json.getString("message") : json.getString("error"), Toast.LENGTH_LONG).show();
                            }
                            catch (JSONException e)
                            {
                                Toast.makeText(NewClientActivity.this, R.string.error_try_again_support, Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                        {
                            Toast.makeText(NewClientActivity.this, error != null && error.getMessage() != null ? error.getMessage() : error.toString(), Toast.LENGTH_LONG).show();
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
        RequestQueue queue = MySingleton.getInstance(NewClientActivity.this).getRequestQueue();

        //Used to mark the request, so we can cancel it on our onStop method
        postRequest.setTag(TAG);

        MySingleton.getInstance(this).addToRequestQueue(postRequest);
    }

    public void RunDeleteClientService()
    {
        progressDialog.show();

        JsonObjectRequest postRequest = new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "clients/delete", api_parameter, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            // If the response is JSONObject instead of expected JSONArray
                            progressDialog.dismiss();
                        }

                        Intent intent = new Intent(NewClientActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("tab", "clients");
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
                                Toast.makeText(NewClientActivity.this, json.has("message") ? json.getString("message") : json.getString("error"), Toast.LENGTH_LONG).show();
                            }
                            catch (JSONException e)
                            {
                                Toast.makeText(NewClientActivity.this, R.string.error_try_again_support, Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                        {
                            Toast.makeText(NewClientActivity.this, error != null && error.getMessage() != null ? error.getMessage() : error.toString(), Toast.LENGTH_LONG).show();
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
