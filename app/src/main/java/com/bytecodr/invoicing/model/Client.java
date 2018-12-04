package com.bytecodr.invoicing.model;

import java.io.Serializable;

/**
 * Created by GuriSingh on 08/05/2016.
 */
public class Client extends BaseModel implements Serializable
{
    public String Name;
    public String Email;

    public String Address1;
    public String Address2;
    public String City;
    public String State;
    public String Postcode;
    public String Country;

    public double TotalMoney;
}
