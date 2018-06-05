package com.ezekielnewren.papercode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;

public class PaperCode implements Closeable {

	// logic
	Webcam wc;
	CodeScanner cs;
	QRCodeTranslator qrct;
	
	// graphical
	JFrame frame = new JFrame();
	JLabel status = new JLabel("status");
	
	
	public PaperCode() {
		// logic
		wc = WebcamExtension.getDefault();
		qrct = new QRCodeTranslator();
		cs = new CodeScanner(qrct,wc);
		
		//cs.cancel();
		
		// graphical
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setMinimumSize(new Dimension(100,100));
		frame.setTitle(this.getClass().getSimpleName());
		
		// specific elements for information exchange
		JMenuBar menu = new JMenuBar();
		
		final JTextArea txtReader = new JTextArea();
		final JTextArea txtWriter = new JTextArea();
		
		// setup gui
		
		// base panel to hold everything
		JPanel base = new JPanel();
		base.setLayout(new BorderLayout());
		
		frame.add(base);
		
		// TODO menu
		menu.add(new JMenu("File"));
		
		base.add(menu, BorderLayout.PAGE_START);
		
		// TODO status
		JPanel statusPanel = new JPanel();
		statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
		
		status.setHorizontalAlignment(SwingConstants.LEFT);
		statusPanel.add(status);
		base.add(statusPanel, BorderLayout.PAGE_END);
		
		// TODO center
		int initialWidth = 500;
		
		JSplitPane center = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		center.setResizeWeight(1);
		
		JPanel pictureIO = new JPanel(new GridLayout());
		JPanel textIO = new JPanel(new GridLayout());

		final JLabel canvasReader = new JLabel();
		final JLabel canvasWriter = new JLabel();

		canvasReader.setOpaque(true);
		canvasWriter.setOpaque(true);
		
		canvasReader.setBackground(Color.RED);
		canvasWriter.setBackground(Color.BLUE);

		pictureIO.add(canvasReader);
		pictureIO.add(canvasWriter);
		
		// text
		txtReader.setEditable(false);
		
		Border border = BorderFactory.createLineBorder(Color.BLACK);
	    txtReader.setBorder(BorderFactory.createCompoundBorder(border,
	            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
	    txtWriter.setBorder(BorderFactory.createCompoundBorder(border,
	            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
		
		pictureIO.setPreferredSize(new Dimension(initialWidth*2, 500));
		
		textIO.add(txtReader);
		textIO.add(txtWriter);
		textIO.setPreferredSize(new Dimension(initialWidth*2, 100));
		
		center.add(pictureIO);
		center.add(textIO);
		
		base.add(center, BorderLayout.CENTER);

		// TODO option
		//String placeHolder = "-------------";
		int optionWidth = 150;
		
		// TODO option reader
		JPanel optionReader = new JPanel();
		optionReader.setPreferredSize(new Dimension(optionWidth, 1));
		//optionReader.setLayout(new GridLayout(2,1));
		
		JButton scanReader = new JButton("scan");
		scanReader.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				readerScan(canvasReader, txtReader);
			}
		});
		
		JButton copyReader = new JButton("copy");
		copyReader.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				copyToClipboard(txtReader);
			}
		});
		
		JButton clearReader = new JButton("clear");
		clearReader.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				readerCancel(canvasReader, txtReader);
				clearText(txtReader, canvasReader);
			}
		});
		
		JButton cancelReader = new JButton("cancel");
		cancelReader.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				readerCancel(canvasReader, txtReader);
			}
		});
		
		
		optionReader.add(scanReader);
		optionReader.add(copyReader);
		optionReader.add(clearReader);
		optionReader.add(cancelReader);
		
		base.add(optionReader, BorderLayout.LINE_START);

		ImageScanListener isl = new ImageScanListener() {
			public void onImageScan(ImageScanEvent ise) {
				BufferedImage scan = ise.getImage();
				double iar = (double)scan.getWidth()/(double)scan.getHeight();
				double far = (double)frame.getWidth()/(double)frame.getHeight();
				
				Dimension container = canvasReader.getSize();
				
				Image scale;
				if (iar<far) {
					scale = ise.getImage().getScaledInstance(-1, container.height, Image.SCALE_FAST);
				} else {
					scale = ise.getImage().getScaledInstance(container.width, -1, Image.SCALE_FAST);
				}

				ImageIcon view = new ImageIcon(scale);
				canvasReader.setIcon(view);
				
				if (ise.getData()!=null) {
					String str = new String(ise.getData());
					readerScanSuccess(canvasReader, txtReader, str);
				}
				
				//picture.setPreferredSize(new Dimension(view.getIconWidth(), view.getIconHeight()));
			}
		};
		cs.addImageScanListener(isl);
		
		// TODO option writer
		JPanel optionWriter = new JPanel();
		//optionWriter.add(new JLabel(placeHolder));
		optionWriter.setPreferredSize(new Dimension(optionWidth, 1));
		
		JButton copyWriter = new JButton("copy");
		copyWriter.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				copyToClipboard(txtWriter);
			}
		});
		
		JButton pasteWriter = new JButton("pase");
		pasteWriter.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				writerPaste(txtWriter);
			}
		});
		
		JButton clearWriter = new JButton("clear");
		clearWriter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				clearText(txtWriter, canvasWriter);
			}
		});
		
		final JButton generateWriter = new JButton("generate");
		generateWriter.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				writerGenerate(canvasWriter, txtWriter);
			}
		});
		
		optionWriter.add(copyWriter);
		optionWriter.add(pasteWriter);
		optionWriter.add(clearWriter);
		optionWriter.add(generateWriter);
		
		base.add(optionWriter, BorderLayout.LINE_END);

		
		// final touches
		Webcam.addDiscoveryListener(new WebcamDiscoveryListener() {
			public void webcamFound(WebcamDiscoveryEvent event) {
				status.setText("webcam connected: "+event.getWebcam().getName());
			}

			@Override
			public void webcamGone(WebcamDiscoveryEvent event) {
				status.setText("webcam disconnected: "+event.getWebcam().getName());
			}
			
		});
		
		frame.pack();
		frame.setVisible(true);
	}
	
	// TODO gui actions
	void copyToClipboard(JTextArea jtext) {
		StringSelection text = new StringSelection(jtext.getText());
	    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    clipboard.setContents(text, text);
	    
	    status.setText("text copied to clipboard");
	}
	
	void clearText(JTextArea jtext, JLabel canvas) {
		jtext.setText("");
		canvas.setIcon(null);
		status.setText("cleared");
	}
	
	// TODO reader
	void readerScan(final JLabel canvas, final JTextArea jtext) {
		try {
			if (wc==null) {
				wc = WebcamExtension.getDefault();
				if (wc==null) {
					status.setText("no webcam detected");
					return;
				}
			}
			status.setText("scanning...");
			jtext.setText("");
			//b.setText("cancel");
			byte[] data = cs.scan(false);
			if (data==null) return;
			readerScanSuccess(canvas, jtext, new String(data));
		} catch (IOException e) {
			status.setText("scan interrupted");
		}
	}
	
	void readerScanSuccess(JLabel canvas, final JTextArea jtext, String text) {
		canvas.setIcon(null);
		jtext.setText(text);
		status.setText("scan success!");
		try { cs.cancel(); } catch (IOException e) {closeQuietly();}
	}
	
	void readerCancel(JLabel canvas, final JTextArea jtext) {
		try {
			cs.cancel();
		} catch (IOException e) {
			closeQuietly();
			e.printStackTrace();
		}
		canvas.setIcon(null);
		status.setText("scan cancelled");
	}
	
	// TODO writer
	void writerPaste(JTextArea jtext) {
		Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
	    Transferable t = c.getContents(this);
	    if (t == null)
	        return;
	    try {
	    	if (!t.isDataFlavorSupported(DataFlavor.stringFlavor)) return;
	    	Object data = t.getTransferData(DataFlavor.stringFlavor);
	        jtext.setText(data.toString());
	    } catch (Exception e) {
	    }
	    
	    status.setText("text pasted from clipboard");
	}

	void writerGenerate(JLabel canvas, JTextArea jtext) {
		byte[] text = jtext.getText().getBytes(StandardCharsets.UTF_8);
		if (text==null||(text!=null&&text.length==0)) {
			status.setText("enter some text first");
			return;
		}
		int size = Math.min(canvas.getWidth(), canvas.getHeight());
		try {
			BufferedImage bi = qrct.encode(text, new Dimension(size,size));
			canvas.setIcon(new ImageIcon(bi));
		} catch (TranslateException e) {
			e.printStackTrace();
			status.setText(e.getMessage());
		}
	}
	
	private void closeQuietly() {
		try { close(); } catch (IOException ioe) {}
	}
	
	@Override
	public void close() throws IOException {
		cs.close();
		wc.close();
	}

	
	public static void main(String[] args) throws Exception {
		PaperCode xxx = new PaperCode();
		Thread.sleep(Long.MAX_VALUE);
		xxx.close();
	}
	
}
