package com.ezekielnewren.papercode.resource;

import java.io.InputStream;

public class Resource {
	
	
	private static final boolean debug;
//	private static final Logger log;
	static {
		boolean tmp = false;
		assert(tmp=true);
		debug = tmp;
		
//		try {
//			//StatusLogger.getLogger().setLevel(Level.OFF);
//			InputStream is = Resource.class.getResourceAsStream("log4j2.xml");
//			ConfigurationSource source = new ConfigurationSource(is);
//			Configurator.initialize(null, source);
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//		
//		if (debug) Configurator.setRootLevel(Level.DEBUG);
//		log = LogManager.getRootLogger();
	}
	
	public static void main(String[] args) {}
	
	public static boolean isDebug() {
		return debug;
	}
	
	public static InputStream getAbout() {
		return Resource.class.getResourceAsStream("about.txt");
	}
	
//	public static Logger getLogger() {
//		return log;
//	}
}
