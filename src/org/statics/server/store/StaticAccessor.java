package org.statics.server.store;


import java.util.ArrayList;
import java.util.List;

import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;


public class StaticAccessor {

    PrimaryIndex<String,StaticFile> staticFileBySid;
    PrimaryIndex<String,StaticData> staticDataById;
    EntityStore store;
    

    public PrimaryIndex<String, StaticFile> getStaticFileBySid() {
        return staticFileBySid;
    }


    public PrimaryIndex<String, StaticData> getStaticDataById() {
        return staticDataById;
    }
    
    
    public StaticFile getStaticFile(String key)
    {
        if(key==null||key.equals(""))
            return null;
        return staticFileBySid.get(key);
    }
    
    public StaticData getStaticData(String key)
    {
        if(key==null||key.equals(""))
            return null;
        return staticDataById.get(key);
    }
    
    public boolean add(StaticFile sf,StaticData...sds)
    {
        Transaction trans = store.getEnvironment().beginTransaction(null, null);
        if(sds.length==0)
            return false;
        
        staticFileBySid.putNoReturn(trans,sf);
        
        for (StaticData staticData : sds) {
            if(staticData!=null)
                staticDataById.putNoReturn(trans,staticData);
        }
        
        trans.commit();
        
        return true;
    }
    
    public boolean delete(String key)
    {
        Transaction trans = store.getEnvironment().beginTransaction(null, null);
        StaticFile sf = staticFileBySid.get(key);
        
        if(sf==null)
            return true;

        if(sf.getSmall()!=null)
            staticDataById.delete(trans,sf.getSmall());
        if(sf.getMedium()!=null)
            staticDataById.delete(trans,sf.getMedium());
        if(sf.getOriginal()!=null)
            staticDataById.delete(trans,sf.getOriginal());
        
        staticFileBySid.delete(trans,key);
        
        trans.commit();
        return true;
        
    }
    
    
    public long size()
    {
        return staticFileBySid.count();
    }
    
    public List<StaticFile> listFiles(int n)
    {
        List<StaticFile> rs = new ArrayList<StaticFile>();
        EntityCursor<StaticFile> cur = staticFileBySid.entities();
        int i = 0;
        for (StaticFile sf : cur)
        {
            ++i;
            if(i>n)break;
            rs.add(sf);
            
        }
        return rs;
    }

    public StaticAccessor(EntityStore store) {
        this.store = store;
        staticFileBySid = store.getPrimaryIndex(String.class, StaticFile.class);
        staticDataById = store.getPrimaryIndex(String.class, StaticData.class);
    }
    
}   
