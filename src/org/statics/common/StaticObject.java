package org.statics.common;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class StaticObject implements SystemConfig
{
    private static final Gson json = new Gson();
    private String sid;
    private String type;
    private Map<String, String> meta;
    private String metadata = "{}";
    private byte[] data;
    
    public static final StaticObject NoneObject = new StaticObject();
    

    public StaticObject()
    {
        meta = new HashMap<String, String>();
    }
    

    public StaticObject(String sid, String type,Map<String, String> meta, byte[] data)
    {
        super();
        this.sid = sid;
        this.type = type;
        this.setMeta(meta);
        this.data = data;
    }
    
    


    public byte[] packer() throws Exception
    {
        
        if(sid==null||sid.length()==0)
            throw new Exception("无效的SID");
        if(type==null||type.length()==0)
            throw new Exception("无效的数据类型（type）");
        if(data==null||data.length==0)
            throw new Exception("无效的数据");
        
        byte[] sidbt = sid.getBytes(DEFAULT_ENCODE);
        byte[] typebt = type.getBytes(DEFAULT_ENCODE);
        byte[] metabt = "{}".getBytes();
        if (meta != null && !meta.isEmpty())
        {
            metadata = json.toJson(meta, Map.class);
            metabt = metadata.getBytes(DEFAULT_ENCODE);
        }
        int len = 4+sidbt.length+4+typebt.length+4+metabt.length+4+data.length;
        ByteBuffer buffer = ByteBuffer.allocate(4+len);
        buffer.putInt(len);
        buffer.putInt(sidbt.length);
        buffer.put(sidbt);
        buffer.putInt(typebt.length);
        buffer.put(typebt);
        buffer.putInt(metabt.length);
        buffer.put(metabt);
        buffer.putInt(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer.array();
    }

    public StaticObject unPacker(ByteBuffer buffer)throws Exception
    {
        buffer.flip();
        int sidLen = buffer.getInt();
        byte[] sidbt = new byte[sidLen];
        buffer.get(sidbt);
        sid = new String(sidbt);
        
        int typeLen = buffer.getInt();
        byte[] typebt = new byte[typeLen];
        buffer.get(typebt);
        type = new String(typebt);
        
        int metaLen = buffer.getInt();
        byte[] metabt = new byte[metaLen];
        buffer.get(metabt);
        metadata = new String(metabt);
        Type typ = new TypeToken<HashMap<String,String>>(){}.getType();
        meta = json.fromJson(metadata,typ );
        
        int dataLen = buffer.getInt();
        data = new byte[dataLen];
        buffer.get(data);
        return this;
    }
    
    public boolean isValid()
    {
        return this!=NoneObject;
    }


    public String getSid()
    {
        return sid;
    }

    public void setSid(String sid)
    {
        this.sid = sid;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public void setMeta(Map<String, String> meta)
    {
        this.meta = meta;
    }
    public Map<String, String> getMeta()
    {
        return meta;
    }
    

    public void addMeta(String key, String value)
    {
        this.meta.put(key, value);
    }

    public byte[] getData()
    {
        return data;
    }

    public void setData(byte[] data)
    {
        this.data = data;
    }


}
