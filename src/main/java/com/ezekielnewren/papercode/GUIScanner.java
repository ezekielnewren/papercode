package com.ezekielnewren.papercode;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;

import javax.swing.*;

import org.apache.logging.log4j.Logger;

import com.ezekielnewren.papercode.resource.Resource;
import com.github.sarxos.webcam.Webcam;

public class GUIScanner implements Closeable {
	private static final Logger log = Resource.getLogger();
	
	Webcam wc;
	JFrame frame = new JFrame();
	CodeScanner cs;
	
	public GUIScanner() {
		wc = WebcamExtension.getDefault();
		cs = new CodeScanner(new QRCodeTranslator(), wc);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel base = new JPanel();
		frame.add(base);
		
		final JLabel picture = new JLabel();
		picture.setPreferredSize(wc.getViewSize());
		base.add(picture);
		frame.pack();
		
		
		ImageScanListener isl = new ImageScanListener() {
			public void onImageScan(ImageScanEvent ise) {
				BufferedImage scan = ise.getImage();
				double iar = (double)scan.getWidth()/(double)scan.getHeight();
				double far = (double)frame.getWidth()/(double)frame.getHeight();
				
				Dimension parent = picture.getParent().getSize();
				
				Image scale;
				if (iar<far) {
					scale = ise.getImage().getScaledInstance(-1, parent.height, Image.SCALE_FAST);
				} else {
					scale = ise.getImage().getScaledInstance(parent.width, -1, Image.SCALE_FAST);
				}

				ImageIcon view = new ImageIcon(scale);
				picture.setIcon(view);
				picture.setPreferredSize(new Dimension(view.getIconWidth(), view.getIconHeight()));
				
				if (ise.getData()!=null) {
					String str = new String(ise.getData());
					log.info("scan success: "+str);
				} else {
					log.info("scan failed");
				}
				
			}
		};
		
		cs.addImageScanListener(isl);
		
	}
	
	public static void main(String[] args) throws Exception {
		//Webcam wc = AutoCloseWebcam.getDefault();
		//wc.setViewSize(new Dimension(1280,720));
		GUIScanner gs = new GUIScanner();
		//System.out.println(gs.wc.getDevice().getName());
		
		long sleep = 0;
		log.info("waiting "+(sleep/1000)+" seconds");
		Thread.sleep(sleep);
		
		log.info("scanning qr code");
		log.info("");
		for (int i=0; i<1; i++) {
			byte[] data = gs.scan();
			log.info(new String(data));
		}
		
		//System.err.println("doing other things "+(sleep*5/1000)+" seconds");
		//Thread.sleep(sleep);
		
		
		gs.close();
	}
	
	public byte[] scan() throws IOException {
		return scan(Long.MAX_VALUE);
	}

	public byte[] scan(long millis) throws IOException {
		try {
			frame.setVisible(true);
			return cs.scan(true);
		} finally {
			frame.setVisible(false);
		}
	}
	
	public void close() throws IOException {
		frame.dispose();
		cs.close();
	}
}





