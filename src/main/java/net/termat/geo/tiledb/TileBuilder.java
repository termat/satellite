package net.termat.geo.tiledb;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
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
import net.termat.geo.satellite.BandUtil;
import net.termat.geo.satellite.ImageReader;
import net.termat.geo.satellite.MeshUtil;
import net.termat.geo.util.PCUtil;
import net.termat.geo.web.LooseHostnameVerifier;
import net.termat.geo.web.LooseTrustManager;

public class TileBuilder {
	
	public static void main01(String[] args) {
		TileProperty tp=new TileProperty();
		tp.maxZoom=17;
		tp.minZoom=10;
		tp.content="application";
		tp.type="vnd.mapbox-vector-tile";
		File dir=new File("E:\\業務\\衛星_丹波篠山市\\mvt\\pbf2");
		File out=new File("E:\\業務\\五條土木\\mvt\\建物.db");
		try {
			buildMVT(dir,out,tp);
		} catch (ClassNotFoundException | SQLException | IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		TileProperty tp=new TileProperty();
		tp.maxZoom=16;
		tp.minZoom=10;
		tp.content="image";
		tp.type="jpg";
		tp.copyright="欧州宇宙機関";
		tp.name="FALSE画像";
		try {
			BufferedImage img=ImageIO.read(new File("E:\\業務\\隠岐の島町\\隠岐の島GIS\\FALSE.png"));
			AffineTransform af=PCUtil.loadTransform(new File("E:\\業務\\隠岐の島町\\隠岐の島GIS\\FALSE.pgw"));
			ImageReader ir=ImageReader.createReader(6671, img, af, "map");
			File out=new File("E:\\業務\\隠岐の島町\\app\\db\\FALSE.db");
			buildImage(ir,out,tp,"png");
		} catch (ClassNotFoundException | SQLException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void buildImageTileFromPNG(Rectangle2D rec,File dir,File dbf,int minZoom,int maxZoom,int size) throws IOException, ClassNotFoundException, SQLException {
		int BL=Color.BLACK.getRGB();
		TileDB db=TileDB.open(dbf);
		for(File f : dir.listFiles()) {
			if(!f.getName().endsWith(".png"))continue;
			System.out.println(f);
			ImageReader br=ImageReader.createReader(4326, ImageIO.read(f), BandUtil.loadTransform(new File(f.getAbsolutePath().replace(".png", ".pgw"))),"data");
			Rectangle2D r2=br.getBounds();
			if(!rec.intersects(r2))continue;
			for(int i=minZoom;i<=maxZoom;i++) {
				List<Point> zz=MeshUtil.getTileList(rec, i);
				List<Tile> tl=new ArrayList<>();
				for(Point p : zz) {
					Tile t=db.getTile(i, p.x, p.y);
					if(t==null) {
						t=new Tile();
						t.x=p.x;
						t.y=p.y;
						t.z=i;
						Rectangle2D rx=MeshUtil.getTileBounds(i, p.x, p.y);
						ImageReader tmp=br.createSubImage(rx);
						BufferedImage bi=createSizeImage(tmp.getBand(),size);
						t.bytes=TileUtil.biToBytes(bi,"png");
						tl.add(t);
					}else {
						Rectangle2D rx=MeshUtil.getTileBounds(i, p.x, p.y);
						ImageReader tmp=br.createSubImage(rx);
						BufferedImage bi=createSizeImage(tmp.getBand(),size);
						BufferedImage ba=TileUtil.bytesToBi(t.bytes);
						for(int x=0;x<size;x++) {
							for(int y=0;y<size;y++) {
								if(ba.getRGB(x, y)==BL)ba.setRGB(x, y, bi.getRGB(x, y));
							}
						}
						t.bytes=TileUtil.biToBytes(ba,"png");
						db.update(t);
					}
				}
				db.add(tl);
			}
		}
	}
	

	
	public static void buildWebImageTile(String url,File out,TileProperty tp,Rectangle2D rectWgs84) throws ClassNotFoundException, SQLException {
		TileDB db=TileDB.open(out);
		db.setProperty(tp);
		for(int i=tp.minZoom;i<=tp.maxZoom;i++) {
			System.out.println(i);
			List<Point> pt=MeshUtil.getTileList(rectWgs84, i);
			List<Tile> li=new ArrayList<>();
			for(Point p : pt) {
				String uu=url.replace("{z}", Integer.toString(i));
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
						t.z=i;
						t.bytes=TileUtil.biToBytes(tmp,getExt(uu));
						li.add(t);
					}
				}catch(IOException | KeyManagementException | NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
			}
			db.add(li);
		}
	}
	
	public static void buildMapboxDEMImageTile(String url,File out,TileProperty tp,Rectangle2D rectWgs84) throws ClassNotFoundException, SQLException {
		TileDB db=TileDB.open(out);
		db.setProperty(tp);
		for(int i=tp.minZoom;i<=tp.maxZoom;i++) {
			System.out.println(i);
			List<Point> pt=MeshUtil.getTileList(rectWgs84, i);
			List<Tile> li=new ArrayList<>();
			for(Point p : pt) {
				String uu=url.replace("{z}", Integer.toString(i));
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
						t.z=i;
						t.bytes=TileUtil.biToBytes(tmp,getExt(uu));
						li.add(t);
					}
				}catch(IOException | KeyManagementException | NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
			}
			db.add(li);
		}
	}
	

	
	public static void buildWebVectorTile(String url,File out,TileProperty tp,Rectangle2D rectWgs84) throws ClassNotFoundException, SQLException {
		TileDB db=TileDB.open(out);
		db.setProperty(tp);
		for(int i=tp.minZoom;i<=tp.maxZoom;i++) {
			List<Point> pt=MeshUtil.getTileList(rectWgs84, i);
			List<Tile> li=new ArrayList<>();
			for(Point p : pt) {
				String uu=url.replace("{z}", Integer.toString(i));
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
					byte[] tmp=con.getInputStream().readAllBytes();
					if(tmp!=null) {
						Tile t=new Tile();
						t.x=p.x;
						t.y=p.y;
						t.z=i;
						t.bytes=tmp;
						li.add(t);
					}
				}catch(IOException | KeyManagementException | NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
				
			}
			db.add(li);
		}
	}
	
	public static void buildImage(File dir,File out,TileProperty tp,String ext) throws ClassNotFoundException, SQLException, IOException {
		TileDB db=TileDB.open(out);
		for(File zf : dir.listFiles()) {
			int z=Integer.parseInt(zf.getName());
			tp.minZoom=Math.min(z, tp.minZoom);
			tp.maxZoom=Math.min(z, tp.maxZoom);
			for(File xf : zf.listFiles()) {
				int x=Integer.parseInt(xf.getName());
				List<Tile> li=new ArrayList<>();
				for(File yf : xf.listFiles()) {
					int y=Integer.parseInt(yf.getName());
					Tile t=new Tile();
					t.x=x;
					t.y=y;
					t.z=z;
					t.bytes=TileUtil.biToBytes(yf,ext);
					li.add(t);
				}
				db.add(li);
			}
		}
		db.setProperty(tp);
	}
	
	public static void buildImage(ImageReader ir,File out,TileProperty tp,String ext) throws ClassNotFoundException, SQLException, IOException {
		TileDB db=TileDB.open(out);
		ir=ir.createProjectionData(4326);
		for(int i=tp.minZoom;i<=tp.maxZoom;i++) {
			System.out.println(i);
			List<Point> pl=MeshUtil.getTileList(ir.getBounds(), i);
			List<Tile> li=new ArrayList<>();
			int[][] col=ir.getRGB();
			AffineTransform af=ir.getTransform();
			try {
				af=af.createInverse();
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
			for(Point p : pl) {
				Rectangle2D r=MeshUtil.getTileBounds(i, p.x, p.y);
				Tile t=new Tile();
				t.x=p.x;
				t.y=p.y;
				t.z=i;
				t.bytes=TileUtil.biToBytes(createTileImage(col,af,r),ext);
				li.add(t);
			}
			db.add(li);
		}
		db.setProperty(tp);
	}
	
	public static void buildImageDem(ImageReader ir,File out,TileProperty tp,String ext) throws ClassNotFoundException, SQLException, IOException {
		TileDB db=TileDB.open(out);
		ir=ir.createProjectionData(4326);
		for(int i=tp.minZoom;i<=tp.maxZoom;i++) {
			System.out.println(i);
			List<Point> pl=MeshUtil.getTileList(ir.getBounds(), i);
			List<Tile> li=new ArrayList<>();
			int[][] col=ir.getRGB();
			AffineTransform af=ir.getTransform();
			try {
				af=af.createInverse();
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
			for(Point p : pl) {
				Rectangle2D r=MeshUtil.getTileBounds(i, p.x, p.y);
				Tile t=new Tile();
				t.x=p.x;
				t.y=p.y;
				t.z=i;
				BufferedImage bi=createTileImage(col,af,r);
				bi=toMapbox(bi);
				t.bytes=TileUtil.biToBytes(bi,ext);
				li.add(t);
			}
			db.add(li);
		}
		db.setProperty(tp);
	}
	
	public static void buildMVT(File dir,File out,TileProperty tp) throws ClassNotFoundException, SQLException, IOException {
		TileDB db=TileDB.open(out);
		for(File zf : dir.listFiles()) {
			int z=Integer.parseInt(zf.getName());
			System.out.println(z);
			for(File xf : zf.listFiles()) {
				int x=Integer.parseInt(xf.getName());
				List<Tile> li=new ArrayList<>();
				for(File yf : xf.listFiles()) {
					int y=Integer.parseInt(delExt(yf.getName()));
					Tile t=new Tile();
					t.x=x;
					t.y=y;
					t.z=z;
					t.bytes=TileUtil.getProto(yf);
					li.add(t);
				}
				db.add(li);
			}
		}
		db.setProperty(tp);
	}
	
	private static String delExt(String str) {
		int id=str.lastIndexOf(".");
		return str.substring(0, id);
	}
	
	private static String getExt(String str) {
		int id=str.lastIndexOf(".");
		return str.substring(id+1, str.length());
	}
	
	private static BufferedImage createTileImage(int[][] src,AffineTransform iaf,Rectangle2D r) {
		BufferedImage ret=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);
		double sx=r.getWidth()/256;
		double sy=r.getHeight()/256;
		AffineTransform at=new AffineTransform(new double[] {sx,0,0,-sy,r.getX(),r.getY()+r.getHeight()});
		for(int i=0;i<256;i++) {
			for(int j=0;j<256;j++) {
				Point2D p=at.transform(new Point2D.Double(i,j), new Point2D.Double());
				p=iaf.transform(p, new Point2D.Double());
				int xx=(int)Math.floor(p.getX());
				int yy=(int)Math.floor(p.getY());
				if(xx>=0&&xx<src.length&&yy>=0&&yy<src[0].length) {
					ret.setRGB(i, j, src[xx][yy]);
				}
			}
		}
		return ret;
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
	
	private static BufferedImage createSizeImage(BufferedImage tmp,double size) {
		double ww=tmp.getWidth();
		double hh=tmp.getHeight();
		AffineTransform af=new AffineTransform(new double[] {ww/size,0,0,-hh/size,0,hh});
		BufferedImage ret=new BufferedImage((int)size,(int)size,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<size;i++) {
			for(int j=0;j<size;j++) {
				Point2D p=af.transform(new Point2D.Double(i, j),new Point2D.Double());
				int xx=(int)Math.floor(p.getX());
				int yy=(int)Math.floor(p.getY());
				if(xx>=0&&xx<ww&&yy>=0&&yy<hh) {
					ret.setRGB(i, j, tmp.getRGB(xx, yy));
				}
			}
		}
		return ret;
	}
}
