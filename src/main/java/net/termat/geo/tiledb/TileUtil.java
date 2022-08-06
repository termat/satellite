package net.termat.geo.tiledb;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import com.google.protobuf.ByteString;
import com.luciad.imageio.webp.WebPWriteParam;

import net.termat.geo.mapdb.MapUtil;

public class TileUtil {
	
	public static byte[] biToBytes(BufferedImage bi,String ext) throws IOException {
		if(ext.equals("webp")) {
			return bi2BytesWebp(bi);
		}else {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(bi,ext,baos);
			baos.flush();
			return baos.toByteArray();
		}
	}
	
	public static byte[] biToBytes(File f,String ext) throws IOException {
		BufferedImage img=ImageIO.read(f);
		return biToBytes(img,ext);
	}
	
	public static byte[] bi2BytesWebp(BufferedImage img)throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageOutputStream  ios =  ImageIO.createImageOutputStream(baos);
		ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
		WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());
		writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		writeParam.setCompressionType(writeParam.getCompressionTypes()[WebPWriteParam.LOSSLESS_COMPRESSION]);
		writer.setOutput(ios);
		writer.write(null, new IIOImage(img, null, null), writeParam);
		ios.flush();
		baos.flush();
		return baos.toByteArray();
	}
	
	/**
	 * byte[]をBufferedImageに変換
	 * @param raw byte[]
	 * @return BufferedImage
	 * @throws IOException
	 */
	public static BufferedImage bytesToBi(byte[] raw)throws IOException{
		ByteArrayInputStream bais = new ByteArrayInputStream(raw);
		BufferedImage img=ImageIO.read(bais);
		return img;
	}
	
	public static byte[] getProto(File f) throws IOException {
		ByteString bs=ByteString.readFrom(new FileInputStream(f));
		return bs.toByteArray();
	}
	
	public static List<Integer> getRGBs(BufferedImage img,Polygon mvt,int ext){
		List<Integer> ret=new ArrayList<>();
		Area area=getShape(mvt,ext);
		Rectangle2D rec=area.getBounds2D();
		int xx=(int)Math.max(0,Math.floor(rec.getX()));
		int yy=(int)Math.max(0,Math.floor(rec.getY()));
		int ww=(int)Math.min(256,Math.ceil(rec.getWidth()));
		int hh=(int)Math.min(256,Math.ceil(rec.getHeight()));
		ww=Math.min(ww+xx, 256);
		hh=Math.min(hh+yy, 256);
		for(int i=xx;i<ww;i++) {
			for(int j=yy;j<hh;j++) {
				if(area.contains(i, j)) {
					ret.add(img.getRGB(i, j));
				}
			}
		}
		return ret;
	}
	
	public static Area getShape(Polygon mvt,int ext) {
		Coordinate[] ls=mvt.getExteriorRing().getCoordinates();
		GeneralPath gp=new GeneralPath();
		gp.moveTo(ls[0].getX()/ext, ls[0].getY()/ext);
		for(int i=0;i<ls.length;i++) {
			gp.lineTo(ls[0].getX()/ext, ls[0].getY()/ext);
		}
		gp.closePath();
		Area area=new Area(gp);
		for(int i=0;i<mvt.getNumInteriorRing();i++) {
			ls=mvt.getInteriorRingN(i).getCoordinates();
			gp=new GeneralPath();
			gp.moveTo(ls[0].getX()/ext, ls[0].getY()/ext);
			for(int j=0;j<ls.length;j++) {
				gp.lineTo(ls[0].getX()/ext, ls[0].getY()/ext);
			}
			gp.closePath();
			area.subtract(new Area(gp));
		}
		return area;
	}

	public static BufferedImage resize(BufferedImage img,int w,int h) throws IOException, NoninvertibleTransformException{
		double scx=(double)w/(double)img.getWidth();
		double scy=(double)h/(double)img.getHeight();
		double sx=1.0/scx;
		double sy=1.0/scy;
		AffineTransform at=new AffineTransform(new double[]{sx,0,0,sy,0,0});
		BufferedImage ret=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<w;i++){
			for(int j=0;j<h;j++){
				Point2D p=at.transform(new Point2D.Double(i, j), new Point2D.Double());
				int xx=(int)Math.max(Math.min(p.getX(), 255),0);
				int yy=(int)Math.max(Math.min(p.getY(), 255),0);
				ret.setRGB(i, j, img.getRGB(xx, yy));
			}
		}
		return ret;
	}
	
	public static BufferedImage toMapbox(BufferedImage img) {
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
}
