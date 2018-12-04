package com.bytecodr.invoicing.model;

import java.io.Serializable;

public class BaseModel implements Serializable
{
    public long Id;
    public long UserId;

    public int Updated;
    public int Created;
}
