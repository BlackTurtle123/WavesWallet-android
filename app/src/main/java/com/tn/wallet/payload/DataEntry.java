package com.tn.wallet.payload;

public class DataEntry {
    public String type;
    public String value;
    public String key;

    public DataEntry(String type, String value, String key) {
        this.type = type;
        this.value = value;
        this.key = key;
    }
}
