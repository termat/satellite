package net.termat.geo.tiledb;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import net.termat.geo.mapdb.MapUtil;
import net.termat.geo.satellite.MeshUtil;
import net.termat.geo.web.LooseHostnameVerifier;
import net.termat.geo.web.LooseTrustManager;

public class TileDBBuilder {
	
	public static void buildTileWeb(String url,File out,Rectangle2D rectWgs84,int zoom) throws ClassNotFoundException, SQLException {
		TileDB db=TileDB.open(out);
		System.out.println(zoom);
		List<Point> pt=MeshUtil.getTileList(rectWgs84, zoom);
		List<Tile> li=new ArrayList<>();
		for(Point p : pt) {
			String uu=url.replace("{z}", Integer.toString(zoom));
			uu=uu.replace("{x}", Integer.toString(p.x));
			uu=uu.replace("{y}", Integer.toString(p.y));
			try {
				HttpsURLConnection con=(HttpsURLConnection)new URL(uu).openConnection();
				SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null,
						new X509TrustManager[] { new LooseTrustManager() },
						new SecureRandom());
				con.setSSLSocketFactory(sslContext.getSocketFactory());
				con.setHostnameVerifier(new LooseHostnameVerifier());
		        BufferedImage tmp=ImageIO.read(con.getInputStream());
				if(tmp!=null) {
					Tile t=new Tile();
					t.x=p.x;
					t.y=p.y;
					t.z=zoom;
					t.bytes=TileUtil.biToBytes(tmp,getExt(uu));
					if(li.size()>1000) {
						db.add(li);
						li.clear();
					}
				}
			}catch(IOException | KeyManagementException | NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		db.add(li);
	}
	
	public static void buildMapboxDEMTileWeb(String url,File out,Rectangle2D rectWgs84,int zoom) throws ClassNotFoundException, SQLException {
		TileDB db=TileDB.open(out);
		System.out.println(zoom);
		List<Point> pt=MeshUtil.getTileList(rectWgs84, zoom);
		List<Tile> li=new ArrayList<>();
		for(Point p : pt) {
			String uu=url.replace("{z}", Integer.toString(zoom));
			uu=uu.replace("{x}", Integer.toString(p.x));
			uu=uu.replace("{y}", Integer.toString(p.y));
			try {
				HttpsURLConnection con=(HttpsURLConnection)new URL(uu).openConnection();
				SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null,
						new X509TrustManager[] { new LooseTrustManager() },
						new SecureRandom());
				con.setSSLSocketFactory(sslContext.getSocketFactory());
				con.setHostnameVerifier(new LooseHostnameVerifier());
		        BufferedImage tmp=ImageIO.read(con.getInputStream());
				if(tmp!=null) {
					tmp=toMapbox(tmp);
					Tile t=new Tile();
					t.x=p.x;
					t.y=p.y;
					t.z=zoom;
					t.bytes=TileUtil.biToBytes(tmp,getExt(uu));
					li.add(t);
					if(li.size()>1000) {
						db.add(li);
						li.clear();
					}
				}
			}catch(IOException | KeyManagementException | NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		db.add(li);
	}
	
	private static BufferedImage toMapbox(BufferedImage img) {
		BufferedImage ret=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);
		Color c=new Color(MapUtil.getRGBmapbox(0));
		Graphics2D g=ret.createGraphics();
		g.setBackground(c);
		g.clearRect(0, 0, 256, 256);
		g.dispose();
		int x=Math.min(256, img.getWidth());
		int y=Math.min(256, img.getHeight());
		for(int i=0;i<x;i++) {
			for(int j=0;j<y;j++) {
				double h=MapUtil.getZ(img.getRGB(i,j));
				if(!Double.isNaN(h)) {
					ret.setRGB(i, j, MapUtil.getRGBmapbox(h));
				}
			}
		}
		return ret;
	}
	
	private static String getExt(String str) {
		int id=str.lastIndexOf(".");
		return str.substring(id+1, str.length());
	}
}
