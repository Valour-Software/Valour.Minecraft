package gg.valour.minecraft.models;

import java.util.Date;

public class PlanetMessage {
    public long id;
    public long planetId;
    public Long replyToId;
    public long authorUserId;
    public long authorMemberId;
    public String content;
    public Date timeSent;
    public long channelId;
    public String embedData;
    public String mentionsData;
    public String attachmentsData;
    public Date editedTime;
    public String fingerprint;
}
