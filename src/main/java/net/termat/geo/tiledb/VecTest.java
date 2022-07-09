package net.termat.geo.tiledb;

import java.awt.Point;
import java.awt.geom.AffineTransform;
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

import net.termat.geo.satellite.BandReader;
import net.termat.geo.satellite.MeshUtil;
import net.termat.geo.util.LonLatXY;
import net.termat.geo.util.PCUtil;
import net.termat.geo.web.LooseHostnameVerifier;
import net.termat.geo.web.LooseTrustManager;

public class VecTest {

	public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {
		BufferedImage img=ImageIO.read(new File("E:\\衛星\\鳥取\\JGD_2020_DEM.png"));
		AffineTransform af=PCUtil.loadTransform(new File("E:\\衛星\\鳥取\\JGD_2020_DEM.pgw"));
		Point2D p1=af.transform(new Point2D.Double(0, 0), new Point2D.Double());
		Point2D p2=af.transform(new Point2D.Double(img.getWidth(), img.getHeight()), new Point2D.Double());
		Point2D pt1=LonLatXY.xyToLonlat(5, p1.getX(), p1.getY());
		Point2D pt2=LonLatXY.xyToLonlat(5, p2.getX(), p2.getY());
		double xmin=Math.min(pt1.getX(), pt2.getX());
		double xmax=Math.max(pt1.getX(), pt2.getX());		
		double ymin=Math.min(pt1.getY(), pt2.getY());
		double ymax=Math.max(pt1.getY(), pt2.getY());
		Rectangle2D r=new Rectangle2D.Double(xmin, ymin,xmax-xmin, ymax-ymin);
		File dir=new File("E:\\衛星\\JAXA_AW3D30");
		List<BandReader> li=new ArrayList<>();
		for(File f : dir.listFiles()) {
			for(File fo : f.listFiles()) {
				if(fo.getName().contains("DSM")) {
					BandReader br=BandReader.createReader(fo);
					if(r.intersects(br.getBounds()))li.add(br);
				}
			}
		}
		
		File out=new File("C:\\Workspace\\java\\20211110\\net.termat.satellite\\db\\AW3D30DSM.db");
		TileDB db=TileDB.open(out);
		TileProperty tp=new TileProperty();
		tp.minZoom=9;
		tp.maxZoom=14;
		tp.copyright="&copy; <a href='https://www.eorc.jaxa.jp/ALOS/jp/dataset/lulc/lulc_v2111_j.htm'>JAXA;</a>";
		tp.name="AW3D_DSM";
		tp.content="image";
		tp.type="png";
		db.setProperty(tp);
		
		
		
		
		
		
	}
	
	public static void main2(String[] args) throws IOException, ClassNotFoundException, SQLException {
		File out=new File("C:\\Workspace\\java\\20211110\\net.termat.satellite\\db\\ベクトルタイル.db");
		TileDB db=TileDB.open(out);
		TileProperty tp=new TileProperty();
		tp.minZoom=9;
		tp.maxZoom=16;
		tp.copyright="&copy; <a href='https://maps.gsi.go.jp/development/ichiran.html'>国土地理院;</a>";
		tp.name="ベクトルタイル";
		tp.content="application";
		tp.type="vnd.mapbox-vector-tile";
		db.setProperty(tp);
		BufferedImage img=ImageIO.read(new File("E:\\衛星\\鳥取\\JGD_2020_DEM.png"));
		AffineTransform af=PCUtil.loadTransform(new File("E:\\衛星\\鳥取\\JGD_2020_DEM.pgw"));
		Point2D p1=af.transform(new Point2D.Double(0, 0), new Point2D.Double());
		Point2D p2=af.transform(new Point2D.Double(img.getWidth(), img.getHeight()), new Point2D.Double());
		
		Point2D pt1=LonLatXY.xyToLonlat(5, p1.getX(), p1.getY());
		Point2D pt2=LonLatXY.xyToLonlat(5, p2.getX(), p2.getY());
		
		double xmin=Math.min(pt1.getX(), pt2.getX());
		double xmax=Math.max(pt1.getX(), pt2.getX());		
		double ymin=Math.min(pt1.getY(), pt2.getY());
		double ymax=Math.max(pt1.getY(), pt2.getY());

		Rectangle2D r=new Rectangle2D.Double(xmin, ymin,xmax-xmin, ymax-ymin);
		
		String url="https://cyberjapandata.gsi.go.jp/xyz/experimental_bvmap/{z}/{x}/{y}.pbf";
		for(int i=tp.minZoom;i<=tp.maxZoom;i++) {
			List<Point> pt=MeshUtil.getTileList(r, i);
			List<Tile> tl=new ArrayList<>();
			System.out.println(i);
			int it=0;
			for(Point p : pt) {
				if(it%100==0)System.out.println(i+":"+it);
				it++;
				String uu=new String(url);
				uu=uu.replace("{z}", Integer.toString(i));
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
						tl.add(t);
					}
				}catch(IOException | KeyManagementException | NoSuchAlgorithmException e) {}
			}
			db.add(tl);
		}
	}
}
