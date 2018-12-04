package com.bytecodr.invoicing.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by GuriSingh on 08/05/2016.
 */
public class Estimate extends BaseModel implements Serializable
{
    public long ClientId;
    public String ClientName;
    public long EstimateNumber;

    public int EstimateDate;
    public int EstimateDueDate;

    public String ClientNote;
    public boolean IsInvoiced;

    public double TaxRate;
    public double TotalMoney;

    public String getEstimateName()
    {
        return "EST-" + getEstimateNumberFormatted();
    }

    public String getEstimateNumberFormatted()
    {
        return String.format("%04d", EstimateNumber);
    }

    public Date getEstimateDate()
    {
        return EstimateDate == 0 ? null : new Date(EstimateDate * 1000L);
    }

    public Date getEstimateDueDate()
    {
        return EstimateDueDate == 0 ? null : new Date(EstimateDueDate * 1000L);
    }
}
