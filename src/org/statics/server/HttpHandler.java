package org.statics.server;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.statics.common.Services;
import org.statics.common.SystemConfig;
import org.statics.common.Utils;
import org.statics.server.store.StaticAccessor;
import org.statics.server.store.StaticFile;
import org.statics.service.service.StoreService;
import org.xlightweb.BadMessageException;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequest;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.Mapping;



@Mapping("/*")
public class HttpHandler implements IHttpRequestHandler, SystemConfig
{
    private final static Log log = LogFactory.getLog(HttpHandler.class);
    private final static int expireDay = Main.getInt("cache.expireDay");

    public void onRequest(IHttpExchange http) throws IOException,
        BadMessageException
    {
        IHttpRequest request = http.getRequest();
        
        if(request.getMethod().equals("POST"))
        {
        	http.forward(request);
        	return;
        }
        
        
        log.info(request.getRequestURI());
        String etag = request.getHeader("If-None-Match");
        String path = request.getPathInfo();
       

        String[] params = path.split("/");

        if (params.length < 2)
        {
            http.sendError(404);
            return;
        }

        String sid = params[1];
        
        if("favicon.ico".equals(sid))
        {
        	http.forward(request);
        	return;
        }
        
        if(Utils.md5Encoder(path).equals(etag))
        {
            http.sendError(304);
            return;
        }
        
        StaticAccessor staticAccess = Services.getBean(StoreService.class).getStaticAccess();
		StaticFile sfile = staticAccess.getStaticFile(sid);
        if(sfile==null)
        {
        	http.sendError(404);
        	return;
        }
        
        char q = QUALITY_ORIGINAL;
        if (params.length == 3 && params[2].length() >= 1)
            q = params[2].toUpperCase().charAt(0);

        try
        {
            HttpResponse resp = null;
            byte [] data = null;
            
        	if(q==QUALITY_SMALL&&sfile.getSmall()!=null)
        		data = staticAccess.getStaticData(sfile.getSmall()).getData();
        	else if(q==QUALITY_MEDIUM&&sfile.getMedium()!=null)
        		data = staticAccess.getStaticData(sfile.getMedium()).getData();
        	else 
        		data = staticAccess.getStaticData(sfile.getOriginal()).getData();
            
        	resp = new HttpResponse(200,sfile.getType(),data);
        	resp.setExpireHeaders(expireDay*24*60*60);
        	resp.setHeader("Etag", Utils.md5Encoder(path));
            http.send(resp);
        }
        catch (Exception e)
        {
            log.error(e);
            http.sendError(404);
        }

    }
    

}
