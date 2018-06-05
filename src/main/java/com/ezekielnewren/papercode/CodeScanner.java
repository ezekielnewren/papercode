package com.ezekielnewren.papercode;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD5Digest;

import com.github.sarxos.webcam.Webcam;

public class CodeScanner implements Closeable {

	static boolean DEBUG = false;
	static {
		assert(DEBUG=true);
	}
	
	final Object mutex = new Object();

	static final String closed_message = "CodeScanner Closed";
	
	int i=0;
	final int ST_CANCEL = 1<<(i++);
	final int ST_IDLE = 1<<(i++);
	final int ST_OPEN = 1<<(i++);
	final int ST_SCANNING = 1<<(i++);
	final int ST_CLOSING = 1<<(i++);;
	final int ST_CLOSED = 1<<(i++);
	
	int state = ST_OPEN;

	LinkedList<byte[]> queue = new LinkedList<byte[]>();
	ArrayList<ImageScanListener> isl = new ArrayList<>();
	BackgroundScanner bs = new BackgroundScanner();
	
	Webcam wc;
	final CodeTranslator ct;
	
	public CodeScanner(CodeTranslator _ct, Webcam _wc) {
		if (_wc!=null) wc = _wc;
		else wc = WebcamExtension.getDefault();
		this.ct = _ct;
		synchronized(mutex) {
			try {
				state = ST_IDLE;
			} finally {
				mutex.notifyAll();
			}
		}
		bs.start();
	}
	
	
	class BackgroundScanner extends Thread {
		
		@Override
		public void run() {
			
			Digest d = new MD5Digest();
			byte[] previous = new byte[d.getDigestSize()];
			byte[] hash = new byte[previous.length];
			
			while (true) {
				Arrays.fill(hash, (byte) 0);
				BufferedImage image = null;
				if (wc!=null) {
					image = wc.getImage();
					try{Thread.sleep(1000/20);}catch(InterruptedException ie) {}
				} else {
					wc = WebcamExtension.getDefault();
				}
				byte[] data = null;
				try {
					if (image==null) data = null;
					else data = ct.decode(image);
				} catch (TranslateException e) {
					data = null;
				}
				
				if (data!=null) {
					d.update(data, 0, data.length);
					d.doFinal(hash, 0);
					if (Arrays.equals(hash, previous)) data = null;
					System.arraycopy(hash, 0, previous, 0, hash.length);
				}
				
				synchronized(mutex) {
					try {
						checkAccessibility();
						ImageScanEvent ise = new ImageScanEvent();
						
						if (state==ST_CANCEL) {
							wc.close();
							state=ST_IDLE;
							Arrays.fill(previous, (byte) 0);
							//System.err.println(queue.size());
							//queue.clear();
						}
						while (state==ST_IDLE) linger();
						if (state==ST_OPEN) {
							if (wc!=null) {
								wc.open();
								state=ST_SCANNING;
							}
						}
						if (data!=null) {
							queue.add(data);
						}
						if (image!=null) {
							for (int i=0; i<isl.size(); i++) {
								ise.setData(data);
								ise.setImage(image);
								isl.get(i).onImageScan(ise);
							}
							if (ise.isConsumed()) queue.clear();
						}
					} catch (IOException e) {
						if (state==ST_CLOSING) {
							state=ST_CLOSED;
						}
						if (DEBUG) e.printStackTrace();
						break;
					} finally {
						mutex.notifyAll();
					}
				}
			}
			
			assert(false);
			
		}
	}
	
	public void addImageScanListener(ImageScanListener _isl) {
		synchronized(mutex) {
			try {
				isl.add(_isl);
			} finally {
				mutex.notifyAll();
			}
		}
	}
	
	void linger(long millis) throws IOException {
		try {
			mutex.notifyAll();
			if ( (state&(ST_CLOSING|ST_CLOSED))!=0 || !bs.isAlive()) throw new IOException();
			mutex.wait(millis);
		} catch (InterruptedException ie) {
			closeQuietly();
			throw new IOException(ie);
		}
	}
	
	void linger() throws IOException {
		linger(0);
	}
	
	void checkAccessibility() throws IOException {
		if (isClosed()) throw new IOException(closed_message);
	}
	
	public boolean isClosed() {
		synchronized(mutex) {
			try {
				if (!bs.isAlive()) return true;
				while (state==ST_CLOSING) linger();
				return state==ST_CLOSED;
			} catch (IOException e) {
				return true;
			}
		}
	}
	
	public byte[] scan(boolean block) throws IOException {
		synchronized(mutex) {
			try {
				if (isClosed()) throw new IOException(closed_message);
				if (!queue.isEmpty())
					return queue.pop();
				if (state==ST_IDLE) {
					state=ST_OPEN;
					mutex.notifyAll();
				}
				if (block) {
					while (queue.isEmpty()&&(state!=ST_CANCEL)) linger();
					if (state==ST_CANCEL) throw new IOException("scan cancelled");
					return queue.pop();
				}
				return null;
			} finally {
				mutex.notifyAll();
			}
		}
	}
	
	public void cancel() throws IOException {
		synchronized(mutex) {
			try {
				if (isClosed()) throw new IOException(closed_message);
				if (state==ST_SCANNING) {
					state = ST_CANCEL;
				}
				if (Thread.currentThread()!=bs) while (state!=ST_IDLE&&!isClosed()) linger();
			} finally {
				mutex.notifyAll();
			}
		}
	}
	
	void closeQuietly() {
		if (!bs.isAlive()) {
			synchronized(mutex) {
				try {
					state = ST_CLOSED;
				} finally {
					mutex.notifyAll();
				}
			}
		} else {
			try{close();}catch(IOException e){e.printStackTrace();}
		}
	}
	
	@Override
	public void close() throws IOException {
		synchronized(mutex) {
			while (state==ST_CLOSING) {
				try { mutex.wait(); } catch (InterruptedException ie) { break; }
			}
			if (state==ST_CLOSED) return;
			try {
				state = ST_CLOSING;
			} finally {
				state = ST_CLOSED;
			}
		}
	}
}
