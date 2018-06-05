package com.ezekielnewren.papercode;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;

import org.apache.commons.lang3.Conversion;

public class QRCodeTranslator implements CodeTranslator {

	private final boolean extended;
	
	public QRCodeTranslator(boolean extended) {
		this.extended = extended;
	}
	
	public QRCodeTranslator() {
		this(false);
	}
	
	@Override
	public BufferedImage encode(byte[] b, int off, int len, Dimension d) 
			throws TranslateException {
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		
		BitMatrix bitMatrix;
		try {
			if (extended) {
				CRC32 crc = new CRC32();
				crc.update(b, off, len);
				byte[] crcCode = new byte[4];
				Conversion.longToByteArray(crc.getValue(), 0, crcCode, 0, 4);
				
				String input = Hex.toHexString(b, off, len)+Hex.toHexString(crcCode);
				bitMatrix = qrCodeWriter.encode(input, BarcodeFormat.QR_CODE, d.width, d.height);
			} else {
				String input = new String(b, off, len);
				bitMatrix = qrCodeWriter.encode(input, BarcodeFormat.QR_CODE, d.width, d.height);
			}
		} catch (WriterException we) {
			throw new TranslateException(we);
		}

		return MatrixToImageWriter.toBufferedImage(bitMatrix);
	}
	
	@Override
	public BufferedImage encode(byte[] b, Dimension d) throws TranslateException {
		return encode(b, 0, b.length, d);
	}

	@Override
	public byte[] decode(BufferedImage input) throws TranslateException {
		LuminanceSource source = new BufferedImageLuminanceSource(input);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
		
		try {
			if (extended) {
				Result result = new MultiFormatReader().decode(bitmap);
				byte[] hex = result.getText().getBytes(StandardCharsets.UTF_8);
				byte[] raw = Hex.decode(hex);
				CRC32 crc = new CRC32();
				crc.update(raw, 0, raw.length-4);
				long a = crc.getValue();
				
				long b = Conversion.byteArrayToLong(raw, raw.length-4, 0, 0, 4);
				if (a!=b) 
					throw new TranslateException("checksum does not match");
				
				return Arrays.copyOf(raw, raw.length-4);
			} else {
				Result result = new MultiFormatReader().decode(bitmap);
				return result.getText().getBytes(StandardCharsets.UTF_8);
			}
		} catch (NotFoundException e) {
			throw new TranslateException(e);
		}
	}

}
