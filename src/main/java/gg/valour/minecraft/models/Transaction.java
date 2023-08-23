package gg.valour.minecraft.models;

import java.math.BigDecimal;

public class Transaction {
    public String id;
    public long planetId;
    public long userFromId;
    public long accountFromId;
    public long userToId;
    public long accountToId;
    public String timeStamp;
    public String description;
    public BigDecimal amount;
    public String data;
    public String fingerprint;
    public Long forcedBy;
}
