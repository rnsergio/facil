package com.bytecodr.invoicing.main;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class SettingLogoActivity extends AppCompatActivity
{
    public static final String TAG = "SettingActivity";
    private MaterialDialog progressDialog;
    private JSONObject api_parameter;

    private final static int RESULT_SELECT_IMAGE = 100;
    public static final String SETTING_LOGO_UPLOAD_NAME = "logo_base64";
    public static final String SETTING_LOGO_NAME = "logo_path";
    private ImageView logo_image_view;

    private String api_key;
    private Integer user_id;
    private Button deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_logo);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences settings = getSharedPreferences(LoginActivity.SESSION_USER, MODE_PRIVATE);

        //Checks whether a user is logged in, otherwise redirects to the Login screen
        if (settings == null || settings.getInt("logged_in", 0) == 0 || settings.getString("api_key", "").equals(""))
        {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        api_key = settings.getString("api_key", "");
        user_id = settings.getInt("id", 0);

        progressDialog = new MaterialDialog.Builder(this)
                .title(R.string.progress_dialog)
                .content(R.string.please_wait)
                .cancelable(false)
                .progress(true, 0).build();

        logo_image_view = (ImageView) findViewById(R.id.logoImageView);
        deleteButton = (Button) findViewById(R.id.deleteLogo);

        api_parameter = new JSONObject();

        try
        {
            api_parameter.put("user_id", user_id);
            RunGetLogoService();
        }
        catch (JSONException ex) {}


    }

    public void uploadLogo(View view) {
        try{
            //Pick Image From Gallery
            Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, RESULT_SELECT_IMAGE);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void deleteLogo(View view) {
        try{
            api_parameter = new JSONObject();
            api_parameter.put("user_id", user_id);
            api_parameter.put("setting_name", SETTING_LOGO_NAME);
            api_parameter.put("setting_value", "");

            RunUpdateSettingsService();
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode){
            case RESULT_SELECT_IMAGE:

                if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                    try{
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA };
                        Cursor cursor = getContentResolver().query(selectedImage,
                                filePathColumn, null, null, null);
                        cursor.moveToFirst();
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String picturePath = cursor.getString(columnIndex);
                        cursor.close();

                        //return Image Path to the Main Activity
                        /*Intent returnFromGalleryIntent = new Intent();
                        returnFromGalleryIntent.putExtra("picturePath",picturePath);
                        setResult(RESULT_OK,returnFromGalleryIntent);
                        finish();*/
                        Bitmap logo = scaleBitmap(BitmapFactory.decodeFile((picturePath)),180, 80 );
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        logo.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                        byte[] logoArray = byteArrayOutputStream .toByteArray();

                        api_parameter = new JSONObject();
                        api_parameter.put("user_id", user_id);
                        api_parameter.put("setting_name", SETTING_LOGO_UPLOAD_NAME);
                        api_parameter.put("setting_value", Base64.encodeToString(logoArray,Base64.DEFAULT));

                        RunUpdateSettingsService();

                    }catch(Exception e){
                        e.printStackTrace();
                        Intent returnFromGalleryIntent = new Intent();
                        setResult(RESULT_CANCELED, returnFromGalleryIntent);
                        finish();
                    }
                }else{

                    Intent returnFromGalleryIntent = new Intent();
                    setResult(RESULT_CANCELED, returnFromGalleryIntent);
                    finish();
                }
                break;
        }
    }

    private Bitmap scaleBitmap(Bitmap bm, int maxWidth, int maxHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();

        if (width > maxWidth || height > maxHeight)
        {
            if (width > height)
            {
                // landscape
                float ratio = (float) width / maxWidth;
                width = maxWidth;
                height = (int) (height / ratio);

                if (height > maxHeight)
                {
                    width = width - 10;
                    return scaleBitmap(bm, width, maxHeight);
                }

            } else if (height > width)
            {
                // portrait
                float ratio = (float) height / maxHeight;
                height = maxHeight;
                width = (int) (width / ratio);

                if (width > maxWidth)
                {
                    height = height - 10;
                    return scaleBitmap(bm, maxWidth, height);
                }
            } else
            {
                // square
                height = maxHeight;
                width = maxWidth;
            }
        }

        return Bitmap.createScaledBitmap(bm, width, height, false);
    }

    public void RunGetLogoService()
    {
        progressDialog.show();

        JsonObjectRequest postRequest = new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "settings/get", api_parameter, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        try
                        {
                            JSONObject result = ((JSONObject)response.get("data"));
                            JSONObject settings = (JSONObject)result.get("settings");

                            String logoBase64 = helper_string.optString(settings, "logo");

                            if (!logoBase64.isEmpty())
                            {
                                byte[] decodedString = Base64.decode(logoBase64, Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                                logo_image_view.setImageBitmap(decodedByte);
                            }
                            else
                            {
                                deleteButton.setVisibility(View.GONE);
                            }
                        }
                        catch(Exception ex)
                        {
                                Toast.makeText(SettingLogoActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();

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
                                Toast.makeText(SettingLogoActivity.this, json.has("message") ? json.getString("message") : json.getString("error"), Toast.LENGTH_LONG).show();
                            }
                            catch (JSONException e)
                            {
                                Toast.makeText(SettingLogoActivity.this, R.string.error_try_again_support, Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                        {
                            Toast.makeText(SettingLogoActivity.this, error != null && error.getMessage() != null ? error.getMessage() : error.toString(), Toast.LENGTH_LONG).show();
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
        RequestQueue queue = MySingleton.getInstance(SettingLogoActivity.this).getRequestQueue();

        //Used to mark the request, so we can cancel it on our onStop method
        postRequest.setTag(MainActivity.TAG);

        MySingleton.getInstance(SettingLogoActivity.this).addToRequestQueue(postRequest);
    }

    public void RunUpdateSettingsService()
    {
        progressDialog.show();

        JsonObjectRequest postRequest = new JsonObjectRequest
                (Request.Method.POST, Network.API_URL + "settings/update", api_parameter, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        try
                        {
                            JSONObject data = ((JSONObject) response.get("data"));

                            String logoBase64 = data.getString("setting_value");

                            if (!logoBase64.isEmpty())
                            {

                                deleteButton.setVisibility(View.VISIBLE);
                            }
                            else
                            {
                                deleteButton.setVisibility(View.GONE);
                            }

                            //String logoBase64 = data.getString("logo");
                            byte[] decodedString = Base64.decode(logoBase64, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                            logo_image_view.setImageBitmap(decodedByte);
                        }
                        catch (JSONException ex) {
                            Toast.makeText(SettingLogoActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
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
                        NetworkResponse response = error.networkResponse;
                        if (response != null && response.data != null)
                        {
                            try
                            {
                                JSONObject json = new JSONObject(new String(response.data));
                                Toast.makeText(SettingLogoActivity.this, json.has("message") ? json.getString("message") : json.getString("error"), Toast.LENGTH_LONG).show();
                            }
                            catch (JSONException e)
                            {
                                Toast.makeText(SettingLogoActivity.this, R.string.error_try_again_support, Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                        {
                            Toast.makeText(SettingLogoActivity.this, error != null && error.getMessage() != null ? error.getMessage() : error.toString(), Toast.LENGTH_LONG).show();
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
        RequestQueue queue = MySingleton.getInstance(SettingLogoActivity.this).getRequestQueue();

        //Used to mark the request, so we can cancel it on our onStop method
        postRequest.setTag(TAG);

        MySingleton.getInstance(SettingLogoActivity.this).addToRequestQueue(postRequest);
    }


}
