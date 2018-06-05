package com.ezekielnewren.papercode;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.Logger;

import com.ezekielnewren.papercode.resource.Resource;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDevice;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;

public class WebcamExtension extends Webcam {
	private static final Logger log = Resource.getLogger();
	
	
	static HashMap<String,WebcamExtension> allWebcam = new HashMap<>();
	
	static final Object mutex = new Object();
	
	static WebcamExtension defaultWC;
	static {
		Webcam.addDiscoveryListener(new WebcamDiscoveryListener() {
			public void webcamFound(WebcamDiscoveryEvent event) {
				Webcam web = event.getWebcam();
				WebcamDevice wd = web.getDevice();
				synchronized(mutex) {
					if (allWebcam.containsKey(wd.getName())) {
						WebcamExtension we = allWebcam.get(wd.getName());
						we.connected.set(true);
					} else {
						allWebcam.put(wd.getName(), new WebcamExtension(wd));
					}
				}
			}
			public void webcamGone(WebcamDiscoveryEvent event) {
				Webcam web = event.getWebcam();
				WebcamDevice wd = web.getDevice();
				synchronized(mutex) {
					if (allWebcam.containsKey(wd.getName())) {
						WebcamExtension we = allWebcam.get(wd.getName());
						we.connected.set(false);
					}
				}
			}
		});
		
		synchronized(mutex) {
			List<Webcam> lw = Webcam.getWebcams();
			for (Webcam w: lw) {
				WebcamDevice wd = w.getDevice();
				String id = wd.getName();
				allWebcam.put(id, new WebcamExtension(wd));
			}
		}
	}
	
	AtomicBoolean connected = new AtomicBoolean(true);
	WebcamExtension inst;
	protected WebcamExtension(WebcamDevice device) {
		super(device);
		inst = this;
		
		Dimension[] mode = getViewSizes();
		long largest = 0;
		Dimension maxRes = null;
		for (Dimension d: mode) {
			long t = d.width*d.height;
			if (t>largest) {
				largest = t;
				maxRes = d;
			}
		}
		
		if (maxRes!=null) {
			setViewSize(maxRes);
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run() {
				if (inst!=null) inst.close();
				else log.fatal("how is 'this' null?");
			}
		});
	}

	public static Webcam getDefault() {
		synchronized(mutex) {
			if (defaultWC==null) {
				Webcam wc = Webcam.getDefault();
				if (wc==null) return null;
				defaultWC = new WebcamExtension(wc.getDevice());
			}
			return defaultWC;
		}
	}
	
	@Override
	public boolean isOpen() {
		return super.isOpen() && isConnected();
	}
	
	public boolean isConnected() {
		synchronized(mutex) {
			return connected.get();
		}
	}
	
	@Override
	public boolean close() {
		return super.close();
//		synchronized(mutex) {
//			boolean status = super.close();
//			while (connected.get()) {
//				try {mutex.wait(5000);} catch (InterruptedException e) {break;}
//			}
//			return status;
//		}
	}
	
}
