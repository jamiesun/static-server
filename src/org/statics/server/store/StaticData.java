package org.statics.server.store;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class StaticData {
    @PrimaryKey
    String id;
    byte[] data;
    StaticData(){}
    public StaticData(String did, byte[] data) {
        super();
        this.data = data;
        this.id = did;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public byte[] getData() {
        return data;
    }
    public void setData(byte[] data) {
        this.data = data;
    }
    
    

}
