package com.ezekielnewren.papercode;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

public interface CodeTranslator {

	public BufferedImage encode(byte[] input, int off, int len, Dimension d)
			throws TranslateException;
	
	public BufferedImage encode(byte[] input, Dimension d) 
			throws TranslateException;
	
	public byte[] decode(BufferedImage input)
			throws TranslateException;
	
}
