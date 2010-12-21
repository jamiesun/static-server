package org.statics.server;

import java.awt.Dimension;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import magick.ImageInfo;
import magick.MagickImage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.statics.common.StaticObject;
import org.statics.common.SystemConfig;
import org.statics.common.Utils;
import org.statics.server.store.StaticAccessor;
import org.statics.server.store.StaticData;
import org.statics.server.store.StaticFile;
import org.statics.service.service.StoreService;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.multiplexed.INonBlockingPipeline;
import org.xsocket.connection.multiplexed.IPipelineConnectHandler;
import org.xsocket.connection.multiplexed.IPipelineDataHandler;
import org.xsocket.connection.multiplexed.IPipelineDisconnectHandler;


public class StaticHandler implements IPipelineDataHandler,IPipelineConnectHandler,IPipelineDisconnectHandler,SystemConfig
{
    private StoreService storeService;
    private StoreHARouter storeHARouter;
    private final static Log log = LogFactory.getLog(StaticHandler.class);
    public void setStoreService(StoreService storeService)
    {
        this.storeService = storeService;
    }
    public void setStoreHARouter(StoreHARouter storeHARouter)
    {
        this.storeHARouter = storeHARouter;
    }
    public boolean onData(INonBlockingPipeline pipeline) throws IOException,
        BufferUnderflowException, ClosedChannelException,
        MaxReadSizeExceededException
    {
        if(!pipeline.isOpen())
            return true;
        
        if(log.isInfoEnabled())
            log.info(pipeline+" onData...");
        
        pipeline.setEncoding(DEFAULT_ENCODE);
        pipeline.markReadPosition();
        try
        {
            int opType = pipeline.readInt();
            if((char)opType==IDENTIFIER_PING)
            {
                Executor exec = pipeline.getWorkerpool();
                exec.execute(new StaticPingProcess(pipeline));
            }
            else if((char)opType==IDENTIFIER_ADD)
            {
                int length = pipeline.readInt();
                pipeline.removeReadMark();
                pipeline.setHandler(new StaticAddHandler(length));
            }
            else if((char)opType==IDENTIFIER_DEL)
            {
                int sidLen = pipeline.readInt();
                String sid = pipeline.readStringByLength(sidLen);
                Executor exec = pipeline.getWorkerpool();
                exec.execute(new StaticDelProcess(pipeline,sid));
                
            }
            else if((char)opType==IDENTIFIER_GET)
            {
                char q = (char) pipeline.readByte();
                int sidLen = pipeline.readInt();
                String sid = pipeline.readStringByLength(sidLen);
                Executor exec = pipeline.getWorkerpool();
                exec.execute(new StaticGetProcess(pipeline,sid,q));
            }
        }
        catch (Exception e)
        {
            log.error("接收数据时出错", e);
        }
        return true;
    }

    public boolean onConnect(INonBlockingPipeline pipeline) throws IOException,
            BufferUnderflowException, MaxReadSizeExceededException {
        if(log.isInfoEnabled())
            log.info(pipeline + " connected");
        return true;
    }

    public boolean onDisconnect(INonBlockingPipeline pipeline) throws IOException {
        if(log.isInfoEnabled())
            log.info(pipeline+" closed");
        return true;
    }  
    
    /**
     * 文件预读处理
     */
    class StaticAddHandler  implements IPipelineDataHandler
    {
        private int remaining = 0;
        private ByteBuffer buffer = null;
        
        public StaticAddHandler(int dlength)
        {
            remaining = dlength;
            buffer = ByteBuffer.allocate(dlength);
        }
        public boolean onData(INonBlockingPipeline nbc) throws IOException,
            BufferUnderflowException, ClosedChannelException,
            MaxReadSizeExceededException
        {
            if(!nbc.isOpen())
                return true;
            try
            {
                int available = nbc.available();
                int lengthToRead = remaining;
                if (available < remaining) { 
                   lengthToRead = available;
                }
                byte[] dates = nbc.readBytesByLength(lengthToRead);
                buffer.put(dates);
                remaining -= lengthToRead;
                
                if (remaining == 0) {  
                    StaticObject sto = new StaticObject().unPacker(buffer);
                    Executor exec = nbc.getWorkerpool();
                    exec.execute(new StaticAddProcess(nbc,sto));
                }
            }
            catch (Exception e)
            {
                log.error("接收文件时出错", e);
                nbc.write(1);
                return true;
            }
            return true;
        }
    }
    
    /**
     * 客户端ping处理，返回当前集群中的主节点地址
     */
    class StaticPingProcess implements Runnable{
        private INonBlockingPipeline pipeline;
        public StaticPingProcess(INonBlockingPipeline pipeline) {
            this.pipeline =  pipeline;
        }
        public void run()
        {
            try
            {
                if(log.isInfoEnabled())
                    log.info(pipeline+"ping...");
                
                InetSocketAddress masterNode = storeHARouter.getNodeMasterAddress();
                if(masterNode==null)
                {
                    synchronized (pipeline)
                    {
                        pipeline.setAutoflush(false);
                        pipeline.write(IDENTIFIER_PING_ACK);
                        pipeline.write(FAILURE);
                        pipeline.flush();
                    }
                    return;
                }
                
                
                long ip = Utils.ip2long(masterNode.getAddress().getHostAddress());
                int port = masterNode.getPort();
                synchronized (pipeline)
                {
                    pipeline.setAutoflush(false);
                    pipeline.write(IDENTIFIER_PING_ACK);
                    pipeline.write(SUCCESS);
                    pipeline.write(ip);
                    pipeline.write(port);
                    pipeline.flush();
                }
            }
            catch (Exception e)
            {
                log.error("StaticPingProcess error",e);
            }
        }
    }
    
    /**
     * 新增文件
     */
    class StaticAddProcess implements Runnable{
        private INonBlockingPipeline pipeline;
        private StaticObject sto;
        public StaticAddProcess(INonBlockingPipeline pipeline,StaticObject sto) {
            this.pipeline =  pipeline;
            this.sto = sto;
        }
        public void run() {
            try
            {
                /**
                 * 当此节点为非主节点时，不允许写入。
                 */
                if(!storeHARouter.checkMaster())
                {
                    log.error("当前节点不可写入");
                    synchronized (pipeline)
                    {
                        pipeline.write(IDENTIFIER_ADD_ACK);
                        pipeline.write(NOT_MASTER);
                        pipeline.flush();
                        pipeline.close();
                    }
                    return;
                }
                
                StaticAccessor dao = storeService.getStaticAccess();
                String sid = sto.getSid();
                String type = sto.getType(); 
                Map<String, String> meta = new HashMap<String, String>(sto.getMeta());
                
                String originalId = UUID.randomUUID().toString();
                byte[] data = sto.getData();

                String smallId = null;
                byte[] smallData = null;
                String mediumId = null;
                byte[] mediumData = null;
                
                if(dao.getStaticFile(sid)!=null)
                {
                    log.error("sid("+sid+")已经存在");
                    synchronized (pipeline)
                    {
                        pipeline.write(IDENTIFIER_ADD_ACK);
                        pipeline.write(FAILURE);
                        pipeline.flush();
                    }
                    return;
                }
                
                try
                {
                    if(type.startsWith("image"))
                    {        
                        //缩略图处理
                        ImageInfo info = new ImageInfo();
                        MagickImage image = new MagickImage(info,data);
                        Dimension dim = image.getDimension();
                        int width = dim.width;
                        int height = dim.height;
                        
                       //小图片文件的处理
                        if(width>120)
                        {
                            int sw = 120;
                            float rate = width*1.0f / 120;
                            int sh = (int) (height*1.0f/rate);
                            MagickImage scaleSmall = image.scaleImage(sw, sh);
                            smallId = UUID.randomUUID().toString();
                            smallData  = scaleSmall.imageToBlob(info);
                        }

                        //中图片文件的处理
                        if(width>640)
                        {
                            int mw = 640;
                            float rate = width*1.0f / 640;
                            int mh = (int) (height*1.0f/rate);
                            MagickImage scaleMedium = image.scaleImage(mw, mh);
                            mediumId = UUID.randomUUID().toString();
                            mediumData = scaleMedium.imageToBlob(info);
                        }

                    }
                }
                catch (Exception e)
                {
                    log.error("图形处理失败");
                    synchronized (pipeline)
                    {
                        pipeline.write(IDENTIFIER_ADD_ACK);
                        pipeline.write(FAILURE);
                        pipeline.flush();
                    }
                    return;
                }
                
                StaticFile sf = new StaticFile(sid,type,meta,smallId,mediumId,originalId);
                StaticData originalSD = new StaticData(originalId,data);
                StaticData smaillSD = null;
                StaticData mediumSD = null;
                if(smallId!=null)
                    smaillSD = new StaticData(smallId,smallData);
                if(mediumId!=null)
                    mediumSD = new StaticData(mediumId,mediumData);

                int ACK = dao.add(sf, originalSD,smaillSD,mediumSD)?SUCCESS:FAILURE;
                if(log.isInfoEnabled())
                    log.info("StaticAddProcess execute:id="+sto.getSid()+";ack="+ACK);
                
                synchronized (pipeline)
                {
                    pipeline.write(IDENTIFIER_ADD_ACK);
                    pipeline.write(ACK);
                    pipeline.flush();
                }


            }
            catch (Exception e)
            {
                log.error(sto.getSid()+" addStatic error",e);
            }
        }
    }
    /**
     * 删除文件
     */
    class StaticDelProcess implements Runnable{
        private INonBlockingPipeline pipeline;
        private String sid;
        public StaticDelProcess(INonBlockingPipeline pipeline,String  sid) {
            this.pipeline =  pipeline;
            this.sid = sid;
        }
        public void run() {
            
            try 
            {
                /**
                 * 当此节点为非主节点时，不允许写入。
                 */
                if(!storeHARouter.checkMaster())
                {
                    log.error("当前节点不可写入");
                    synchronized (pipeline)
                    {
                        pipeline.write(IDENTIFIER_ADD_ACK);
                        pipeline.write(NOT_MASTER);
                        pipeline.flush();
                        pipeline.close();
                    }
                    return;
                }
                
                int ACK = storeService.getStaticAccess().delete(sid)?SUCCESS:FAILURE;
                
                if(log.isInfoEnabled())
                    log.info("StaticDelProcess execute:id="+sid+";ack="+ACK);
                synchronized (pipeline)
                { 
                    pipeline.write(IDENTIFIER_DEL_ACK);
                    pipeline.write(ACK);
                    pipeline.flush();
                }

            }
            catch (Exception e) 
            {
                log.error("StaticDelProcess send resp error",e);
            }
        }
    }

    /**
     * 读取文件
     */
    class StaticGetProcess implements Runnable{
        private INonBlockingConnection pipeline;
        private String sid;
        private char q;
        public StaticGetProcess(INonBlockingPipeline pipeline,String  sid,char q) {
            this.pipeline =  pipeline;
            this.sid = sid;
            this.q = q;
        }
        public void run() 
        {
            
          StaticFile sfile= storeService.getStaticAccess().getStaticFile(sid);
          StaticObject sto = null;
          
          if(sfile!=null)
          {
              byte[] data = null;
              if(q==QUALITY_SMALL&&sfile.getSmall()!=null)
                  data = storeService.getStaticAccess().getStaticData(sfile.getSmall()).getData();
              else if(q==QUALITY_MEDIUM&&sfile.getMedium()!=null)
                  data = storeService.getStaticAccess().getStaticData(sfile.getMedium()).getData();
              else 
                  data = storeService.getStaticAccess().getStaticData(sfile.getOriginal()).getData();
              sto = new StaticObject();
              sto.setSid(sfile.getSid());
              sto.setType(sfile.getType());
              sto.setMeta(sfile.getMeta());
              sto.setData(data);
          }
          
          int ACK = sfile==null?FAILURE:SUCCESS;
          
          try {
              
              synchronized (pipeline)
              {
                  pipeline.write(IDENTIFIER_GET_ACK);
                  pipeline.write(ACK);
                  if(sfile!=null)
                  {
                      pipeline.write(sto.packer());
                  }
                  pipeline.flush();
              }
              
              if(log.isInfoEnabled())
                  log.info("StaticGetProcess execute:id="+sid+";ack="+ACK);
              
          } 
          catch (Exception e) 
          {
              log.error("StaticGetProcess send resp error",e);
          }
        }
        
    }


    

}
