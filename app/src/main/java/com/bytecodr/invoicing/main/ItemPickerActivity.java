package com.bytecodr.invoicing.main;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
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
import com.bytecodr.invoicing.helper.helper_string;
import com.bytecodr.invoicing.model.Item;
import com.bytecodr.invoicing.network.MySingleton;
import com.bytecodr.invoicing.network.Network;
import com.rey.material.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ItemPickerActivity extends AppCompatActivity
{
    public static final String TAG = "ItemPickerActivity";
    private MaterialDialog progressDialog;
    private JSONObject api_parameter;

    Item currentItem;
    Integer itemPosition;

    private ArrayList<Item> array_list_items;
    private String[] array_items;
    Spinner spinner_items;

    private EditText edit_name;
    private EditText edit_description;
    private EditText edit_rate;
    private EditText edit_quantity;

    private String currency;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_picker);

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

        currency = settings.getString(SettingActivity.CURRENCY_SYMBOL_KEY, "$");

        progressDialog = new MaterialDialog.Builder(this)
                .title(R.string.progress_dialog)
                .content(R.string.please_wait)
                .cancelable(false)
                .progress(true, 0).build();

        array_list_items = new ArrayList<Item>();

        edit_name = (EditText) findViewById(R.id.edit_name);
        edit_description = (EditText) findViewById(R.id.edit_description);
        edit_rate = (EditText) findViewById(R.id.edit_rate);
        edit_quantity = (EditText) findViewById(R.id.edit_quantity);

        spinner_items = (Spinner) findViewById(R.id.spinner_items);
        spinner_items.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(Spinner parent, View view, int position, long id) {
                if (position != 0)
                {
                    Item item = array_list_items.get(position);

                    edit_name.setText(item.Name);
                    edit_description.setText(item.Description);
                    edit_rate.setText(String.format("%.2f", item.Rate));
                    edit_quantity.setText("1");
                }
            }
        });

        currentItem = (Item)getIntent().getSerializableExtra("data");
        itemPosition = getIntent().getIntExtra("position", -1);

        if (currentItem != null)
        {
            edit_name.setText(currentItem.Name);
            edit_description.setText(currentItem.Description);
            edit_rate.setText(String.format("%.2f", currentItem.Rate));
            edit_quantity.setText(String.valueOf(currentItem.Quantity));
        }

        api_parameter = new JSONObject();

        try
        {
            api_parameter.put("user_id", settings.getInt("id", 0));
        }
        catch(JSONException ex) {}

        RunGetItemService();
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
        getMenuInflater().inflate(R.menu.menu_item_picker, menu);

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
                Item newItem = new Item();
                newItem.Name = edit_name.getText().toString();
                newItem.Description = edit_description.getText().toString();

                String rate = edit_rate.getText().toString();
                String quantity = edit_quantity.getText().toString();

                newItem.Rate = Double.parseDouble((rate.isEmpty() ? "0" : rate));
                newItem.Quantity = Integer.parseInt((quantity.isEmpty() ? "0" : quantity));

                Intent returnIntent = new Intent();
                returnIntent.putExtra("data", newItem);

                if (currentItem != null) returnIntent.putExtra("position", itemPosition);

                setResult(Activity.RESULT_OK, returnIntent);
                finish();

                return true;
            }
            else
            {
                return false;
            }
        }
        else if (id == android.R.id.home)
        {
            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_CANCELED,returnIntent);
            finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean isFormValid()
    {
        boolean isValid = true;

        if (edit_name.getText().toString().trim().length() == 0) {
            edit_name.setError("Name required");
            isValid = false;
        } else
            edit_name.setError(null);

        return isValid;
    }

    public void RunGetItemService()
    {
        progressDialog.show();

        JsonObjectRequest postRequest = new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "items/get", api_parameter, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        try
                        {
                            JSONObject result = ((JSONObject)response.get("data"));
                            JSONArray items = (JSONArray)result.get("items");

                            array_items = new String[ items.length() + 1];
                            array_list_items.clear();

                            Item selectItem = new Item();
                            selectItem.Id = 0;
                            selectItem.Name = "Select Item";
                            array_list_items.add(selectItem);
                            array_items[0] = "Select Item";

                            for (int i = 0; i < items.length(); i++) {
                                JSONObject obj = items.getJSONObject(i);

                                Item item = new Item();

                                item.Id = obj.optInt("id");
                                item.UserId = obj.optInt("user_id");

                                item.Name = helper_string.optString(obj, "name");
                                item.Description = helper_string.optString(obj, "description");
                                item.Rate = obj.optDouble("rate");

                                item.Created = obj.optInt("created_on", 0);
                                item.Updated = obj.optInt("updated_on", 0);

                                array_list_items.add(item);
                                array_items[i+1] = item.Name;
                            }

                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(ItemPickerActivity.this, R.layout.custom_simple_spinner_item, array_items);
                            spinner_items.setAdapter(adapter);

                            if (itemPosition != null)
                            {

                            }
                        }
                        catch(Exception ex)
                        {
                            Toast.makeText(ItemPickerActivity.this, R.string.error_try_again_support, Toast.LENGTH_LONG).show();
                        }

                        if (progressDialog != null && progressDialog.isShowing()) {
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
                                Toast.makeText(ItemPickerActivity.this, json.has("message") ? json.getString("message") : json.getString("error"), Toast.LENGTH_LONG).show();
                            }
                            catch (JSONException e)
                            {
                                Toast.makeText(ItemPickerActivity.this, R.string.error_try_again_support, Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                        {
                            Toast.makeText(ItemPickerActivity.this, error != null && error.getMessage() != null ? error.getMessage() : error.toString(), Toast.LENGTH_LONG).show();
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
        RequestQueue queue = MySingleton.getInstance(ItemPickerActivity.this).getRequestQueue();

        //Used to mark the request, so we can cancel it on our onStop method
        postRequest.setTag(MainActivity.TAG);

        MySingleton.getInstance(ItemPickerActivity.this).addToRequestQueue(postRequest);
    }
}
