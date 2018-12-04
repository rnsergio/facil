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
import com.bytecodr.invoicing.model.Invoice;

import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class InvoiceAdapter extends ArrayAdapter<Invoice>
{
    private final Context context;
    private final ArrayList<Invoice> values;
    private String currency;
    private SimpleDateFormat dateFormat;

    public InvoiceAdapter(Context context, ArrayList<Invoice> values) {
        super(context, R.layout.layout_item_row, values);

        this.context = context;
        this.values = values;

        SharedPreferences settings = context.getSharedPreferences(LoginActivity.SESSION_USER, context.MODE_PRIVATE);
        currency = settings.getString(SettingActivity.CURRENCY_SYMBOL_KEY, "$");
        dateFormat = new SimpleDateFormat("dd. MMM yyyy");
    }

    public Invoice getItem(int position)
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

        View rowView = inflater.inflate(R.layout.layout_invoice_row, parent, false);

        Invoice invoice = values.get(position);

        TextView text_client_name = (TextView) rowView.findViewById(R.id.text_client_name);
        TextView text_invoice_number = (TextView) rowView.findViewById(R.id.text_invoice_number);
        TextView text_invoice_date = (TextView) rowView.findViewById(R.id.text_invoice_date);
        TextView text_total_amount = (TextView) rowView.findViewById(R.id.text_total_amount);
        TextView text_invoice_status = (TextView) rowView.findViewById(R.id.text_invoice_status);

        text_client_name.setText(invoice.ClientName);
        text_invoice_number.setText(invoice.getInvoiceName());

        if (invoice.InvoiceDate != 0)   text_invoice_date.setText(dateFormat.format(invoice.getInvoiceDate()));

        text_total_amount.setText(currency + String.format( "%.2f", invoice.TotalMoney));

        if (invoice.IsPaid) text_invoice_status.setText(getContext().getResources().getString(R.string.paid));
        return rowView;
    }
}
