package org.statics.service.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
public class ConfigService {

	private Properties cfg;
	private final static Log log = LogFactory.getLog(ConfigService.class);
	
	public ConfigService()
    {
		load();
    }
	
	public void load() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("./config.xml");
			cfg = new Properties();
			cfg.loadFromXML(fis);
		} catch (Exception e) {
			log.error("加载配置失败，请检查config.xml文件", e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public int getInt(String key) {
		return Integer.parseInt(cfg.getProperty(key));
	}

	public int getInt(String key, int defaultValue) {
		return Integer.parseInt(cfg.getProperty(key, String
				.valueOf(defaultValue)));
	}

	public String getString(String key) {
		return cfg.getProperty(key);
	}

	public String getString(String key, String defaultValue) {
		return cfg.getProperty(key, defaultValue);
	}

}
