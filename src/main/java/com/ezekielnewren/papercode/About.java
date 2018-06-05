package com.ezekielnewren.papercode;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.commons.collections4.iterators.IteratorIterable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.logging.log4j.Logger;

import com.ezekielnewren.papercode.resource.Resource;

public class About {
	private static Logger log = Resource.getLogger();
	
	final String version;
	final long buildtime;
	final String name;
	final File propFile;
	final File settingDirectory;
	final Map<String, Object> setting = new HashMap<>();
	
	static About inst;
	static {
		try {
			inst = new About();
		} catch (IOException e) {
			e.printStackTrace();
			log.fatal("failed to load about information exiting...");
			System.exit(3);
		}
	}
	
	
	private About() throws IOException {
		
		InputStream is = Resource.getAbout();
		LineIterator li = IOUtils.lineIterator(is, StandardCharsets.UTF_8);
		IteratorIterable<String> input = new IteratorIterable<String>(li);
		
		String prefix;

		String v = null;
		long b = 0;
		String n = null;
		for (String line: input) {
			if (line.startsWith(prefix="version ")) {
				v = line.substring(prefix.length());
			}
			if (line.startsWith(prefix="buildtime ")) {
				b = Long.parseLong(line.substring(prefix.length()));
			}
			if (line.startsWith(prefix="name")) {
				n = line.substring(prefix.length());
			}
		}
		version = v;
		buildtime = b;
		name = n.trim();
		
		settingDirectory = new File(System.getProperty("user.home")+"/."+name);
		if (!settingDirectory.exists()&&!settingDirectory.mkdir()) throw new IOException("cannot create settingDirectory");
		
		propFile = new File(settingDirectory, ".properties");
	}
	
	public static void main(String[] args) {
		About a = About.getInstance();
		String ver = a.getVersion();
		Date built = new Date(a.getBuildtime());
		log.info("version: "+ver+" buildtime: "+built);
	}

	public static About getInstance() {
		return inst;
	}
	
	public String getVersion() {
		return version;
	}
	
	public long getBuildtime() {
		return buildtime;
	}
	
	public String getName() {
		return name;
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> loadProperties() throws IOException, ClassNotFoundException {
		FileInputStream fis;
		try {
			fis = new FileInputStream(propFile);
		} catch (FileNotFoundException fnfe) {
			return setting;
		}
		Map<String, Object> tmp;
		try {
			ObjectInputStream ois = new ObjectInputStream(fis);
			tmp = (Map<String, Object>) ois.readObject();
			ois.close();
		} catch (EOFException eof) {
			tmp = new HashMap<String, Object>();
		}

		setting.clear();
		for (String key: tmp.keySet()) setting.put(key, tmp.get(key));
		
		return setting;
	}
	
	public void storeProperties() throws IOException {
		FileOutputStream fos = new FileOutputStream(propFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(setting);
		oos.close();
	}
	
	public Map<String, Object> getProperties() {
		return setting;
	}
}



