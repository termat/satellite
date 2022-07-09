package net.termat.geo.tiledb;

import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.j256.ormlite.logger.Level;
import com.j256.ormlite.logger.Logger;

public class ServerRunner {
	private JFrame frame;
	
	public ServerRunner() {
		frame=new JFrame();
		frame.setTitle(" サーバーテスト");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			SwingUtilities.updateComponentTreeUI(frame);
		}catch(Exception e){
			try {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
				SwingUtilities.updateComponentTreeUI(frame);
			}catch(Exception ee){
				ee.printStackTrace();
			}
		}
		WindowAdapter wa=new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		};
		frame.addWindowListener(wa);
		frame.setSize(300, 200);
		frame.setResizable(false);
		frame.getContentPane().setLayout(new GridLayout(4,1));
		JLabel la=new JLabel("Server Running!");
		la.setFont(new Font(Font.SANS_SERIF,Font.BOLD,16));
		la.setHorizontalAlignment(JLabel.CENTER);
		frame.getContentPane().add(la);
		JButton bt3=new JButton("Show 2D Map（Single）");
		bt3.addActionListener(e->{
			browerOpen3();
		});
		frame.getContentPane().add(bt3);
		JButton bt1=new JButton("Show 2D Map（Divide）");
		bt1.addActionListener(e->{
			browerOpen1();
		});
		frame.getContentPane().add(bt1);
		JButton bt2=new JButton("Show 3D Map");
		bt2.addActionListener(e->{
			browerOpen2();
		});
		frame.getContentPane().add(bt2);
		Runnable r=new Runnable() {
			public void run() {
				TileServer.runServer();
				Runnable r=new Runnable() {
					public void run() {
						frame.setLocationRelativeTo(null);
						frame.setVisible(true);
					}
				};
				SwingUtilities.invokeLater(r);
			}
		};
		new Thread(r).start();
	}
	
	private static void browerOpen1() {
		Desktop desktop = Desktop.getDesktop();
		try {
		    desktop.browse(new URI("http://localhost:4567/map2d"));
		} catch (IOException e) {
		    e.printStackTrace();
		} catch (URISyntaxException e) {
		    e.printStackTrace();
		}
	}
	
	private static void browerOpen2() {
		Desktop desktop = Desktop.getDesktop();
		try {
		    desktop.browse(new URI("http://localhost:4567/map3d"));
		} catch (IOException e) {
		    e.printStackTrace();
		} catch (URISyntaxException e) {
		    e.printStackTrace();
		}
	}
	
	private static void browerOpen3() {
		Desktop desktop = Desktop.getDesktop();
		try {
		    desktop.browse(new URI("http://localhost:4567/map2d2"));
		} catch (IOException e) {
		    e.printStackTrace();
		} catch (URISyntaxException e) {
		    e.printStackTrace();
		}
	}
	
	private void close(){
		int id=JOptionPane.showConfirmDialog(frame, "Exit?", "Info", JOptionPane.YES_NO_OPTION,JOptionPane.INFORMATION_MESSAGE);
		if(id==JOptionPane.YES_OPTION){
			frame.setVisible(false);
			System.exit(0);
		}
	}
	
	public static void main(String[] args) {
		Logger.setGlobalLogLevel(Level.ERROR);
		new ServerRunner();
	}
}
