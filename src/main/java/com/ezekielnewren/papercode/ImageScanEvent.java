package com.ezekielnewren.papercode;

import java.awt.image.BufferedImage;

public class ImageScanEvent {

	BufferedImage image;
	byte[] data;
	boolean consumed;
	
	public ImageScanEvent() {
		//setImage(image);
	}
	
	public BufferedImage getImage() {
		if (image==null) throw new NullPointerException();
		return image;
	}
	
	public void setImage(BufferedImage bi) {
		if (bi==null) throw new NullPointerException();
		image = bi;
	}
	
	public void setData(byte[] b) {
		consumed = false;
		data = b;
	}
	
	public byte[] getData() {
		consumed = true;
		return data;
	}
	
	boolean isConsumed() {
		return consumed;
	}
	
}
