package com.bytecodr.invoicing.model;

import java.io.Serializable;

/**
 * Created by GuriSingh on 08/05/2016.
 */
public class Item extends BaseModel implements Serializable
{
    public String Name;
    public String Description;
    public double Rate;
    public int Quantity;

    public double getTotal()
    {
        return Quantity * Rate;
    }
}
