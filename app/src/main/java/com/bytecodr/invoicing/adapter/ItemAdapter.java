package com.bytecodr.invoicing.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bytecodr.invoicing.R;
import com.bytecodr.invoicing.main.LoginActivity;
import com.bytecodr.invoicing.main.SettingActivity;
import com.bytecodr.invoicing.model.Item;

import java.util.ArrayList;


public class ItemAdapter extends ArrayAdapter<Item>
{
    private final Context context;
    private final ArrayList<Item> values;
    private String currency;

    public ItemAdapter(Context context, ArrayList<Item> values) {
        super(context, R.layout.layout_item_row, values);

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

        View rowView = inflater.inflate(R.layout.layout_item_row, parent, false);

        TextView text_name = (TextView) rowView.findViewById(R.id.text_name);
        TextView text_rate = (TextView) rowView.findViewById(R.id.text_rate);

        text_name.setText(values.get(position).Name);
        text_rate.setText(currency + String.format( "%.2f", values.get(position).Rate));

        return rowView;
    }
}
