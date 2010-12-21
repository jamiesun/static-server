package org.statics.server.store;

import java.util.HashMap;
import java.util.Map;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;


@Entity
public class StaticFile {
    @PrimaryKey
    String sid;
    String type;
    Map<String, String> meta = new HashMap<String, String>();
    String original;
    String medium;
    String small;
    
    public StaticFile(String sid, String type, Map<String, String> meta,
            String small, String medium, String original) {
        this.sid = sid;
        this.type = type;
        this.meta = meta;
        this.small = small;
        this.medium = medium;
        this.original = original;
    }

    public StaticFile() {}  

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, String> meta) {
        this.meta = meta;
    }

    public String getOriginal() {
        return original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    public String getSmall() {
        return small;
    }

    public void setSmall(String small) {
        this.small = small;
    }

    public String toString() {
        return new StringBuffer().append("StaticFile[")
                                 .append("sid=").append(sid).append(";")
                                 .append("type=").append(type).append(";")
                                 .append("meta=").append(meta).append(";")
                                 .append("small=").append(small).append(";")
                                 .append("medium=").append(medium).append(";")
                                 .append("original=").append(original).append(";") 
                                 .append("]").toString();
    }


}   

