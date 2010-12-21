package org.statics.service.service;

import org.apache.log4j.xml.DOMConfigurator;

public class LoggerService  {

    public LoggerService() {
        DOMConfigurator.configure("./log4j.xml");
    }

}
