package net.termat.geo.satellite;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.gdal.gdal.gdal;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;

import net.termat.geo.util.PCUtil;

public class ImageReader {
	private SpatialReference srs;
	private AffineTransform atrans;
	private BufferedImage band;
	private int epsg;
	private String chName;
	
	private ImageReader() throws IOException {}
	
	public static ImageReader createReader(int epsg,File f,String name) throws IOException {
		gdal.AllRegister();
		ImageReader sd=new ImageReader();
		sd.srs=BandUtil.createSpatialReference(epsg);
		sd.epsg=epsg;
		sd.band=ImageIO.read(f);
		sd.atrans=PCUtil.loadTransform(new File(f.getAbsolutePath().replace(".png", ".pgw").replace(".PNG", ".pgw").replace(".jpg", ".jgw").replace(".JPG", ".jgw")));
		sd.chName=name;
		return sd;
	}
	
	
	public static ImageReader createReader(int epsg,BufferedImage img,AffineTransform af,String name) throws IOException {
		gdal.AllRegister();
		ImageReader sd=new ImageReader();
		sd.srs=BandUtil.createSpatialReference(epsg);
		sd.epsg=epsg;
		sd.band=img;
		sd.atrans=af;
		sd.chName=name;
		return sd;
	}
	
	public ImageReader createSubImage(Rectangle2D rect,double res) throws IOException {
		double[] param=new double[] {res,0,0,-res,rect.getX(),rect.getY()+rect.getHeight()};
		AffineTransform af=new AffineTransform(param);
		ImageReader sd=new ImageReader();
		sd.srs=srs;
		sd.epsg=epsg;
		sd.atrans=af;
		sd.band=getBand(rect,res);
		sd.chName=chName;
		return sd;
	}
	
	public ImageReader createSubImage(Rectangle2D rect) throws IOException {
		double[] param=new double[] {atrans.getScaleX(),0,0,atrans.getScaleY(),rect.getX(),rect.getY()+rect.getHeight()};
		AffineTransform af=new AffineTransform(param);
		ImageReader sd=new ImageReader();
		sd.srs=srs;
		sd.epsg=epsg;
		sd.atrans=af;
		sd.band=getBand(rect);
		sd.chName=chName;
		return sd;
	}
	
	public BufferedImage getBand() {
		return band;
	}
	
	public BufferedImage getBand(Rectangle2D rect,double res) {
		int ww=band.getWidth();
		int hh=band.getHeight();
		int w=(int)Math.round(rect.getWidth()/res);
		int h=(int)Math.abs(Math.round(rect.getHeight()/res));
		BufferedImage ret=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		double[] param=new double[] {res,0,0,-res,rect.getX(),rect.getY()+rect.getHeight()};
		AffineTransform af=new AffineTransform(param);
		AffineTransform iaf=null;
		try {
			iaf=atrans.createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		for(int x=0;x<w;x++) {
			for(int y=0;y<h;y++) {
				Point2D p=af.transform(new Point2D.Double(x, y), new Point2D.Double());
				p=iaf.transform(p, new Point2D.Double());
				int xx=(int)Math.round(p.getX());
				int yy=(int)Math.round(p.getY());
				if(xx>=0&&xx<ww&&yy>=0&&yy<hh) {
					ret.setRGB(x, y, band.getRGB(xx, yy));
				}
			}
		}
		return ret;
	}
	
	public BufferedImage getBand(Rectangle2D rect) {
		int w=(int)Math.round(rect.getWidth()/atrans.getScaleX());
		int h=(int)Math.abs(Math.round(rect.getHeight()/atrans.getScaleY()));
		BufferedImage ret=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		double[] param=new double[] {atrans.getScaleX(),0,0,atrans.getScaleY(),rect.getX(),rect.getY()+rect.getHeight()};
		AffineTransform af=new AffineTransform(param);
		AffineTransform iaf=null;
		try {
			iaf=atrans.createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		int ww=band.getWidth();
		int hh=band.getHeight();
		for(int x=0;x<w;x++) {
			for(int y=0;y<h;y++) {
				Point2D p=af.transform(new Point2D.Double(x, y), new Point2D.Double());
				p=iaf.transform(p, new Point2D.Double());
				int xx=(int)Math.round(p.getX());
				int yy=(int)Math.round(p.getY());
				if(xx>=0&&xx<ww&&yy>=0&&yy<hh) {
					ret.setRGB(x, y, band.getRGB(xx, yy));
				}
			}
		}
		return ret;
	}
	
	public SpatialReference getSrs() {
		return srs;
	}
	
	public AffineTransform getTransform() {
		return atrans;
	}
	
	public BufferedImage getImage() {
		return band;
	}
	
	public int[][] getRGB(){
		int[][] ret=new int[band.getWidth()][band.getHeight()];
		for(int i=0;i<ret.length;i++) {
			for(int j=0;j<ret[0].length;j++) {
				ret[i][j]=band.getRGB(i, j);
			}
		}
		return ret;
	}
	
	
	public Rectangle2D getBounds() {
		Rectangle2D ret=new Rectangle2D.Double(0,0,band.getWidth(),band.getHeight());
		return atrans.createTransformedShape(ret).getBounds2D();
	}
	
	public int getEPSG() {
		return epsg;
	}
	
	public String getChannelName() {
		return chName;
	}
	
	public ImageReader createProjectionData(int target_epsg) throws IOException {
		SpatialReference target=BandUtil.createSpatialReference(target_epsg);
		CoordinateTransformation ct=BandUtil.getCoordinateTransformation(srs,target);
		Rectangle2D rect=null;
		for(int x=0;x<band.getWidth();x++) {
			for(int y=0;y<band.getHeight();y++) {
				Point2D sp=atrans.transform(new Point2D.Double(x,y), new Point2D.Double());
				double[] p2=ct.TransformPoint(sp.getX(), sp.getY());
				if(rect==null) {
					rect=new Rectangle2D.Double(p2[0],p2[1],0,0);
				}else {
					rect.add(p2[0],p2[1]);
				}
			}
		}
		double sx=rect.getWidth()/band.getWidth();
		double sy=rect.getHeight()/band.getHeight();
		double[] p=new double[] {sx,0,0,-sy,rect.getX(),rect.getY()+rect.getHeight()};
		AffineTransform af=new AffineTransform(p);
		int ww=(int)Math.abs(rect.getWidth()/sx);
		int hh=(int)Math.abs(rect.getHeight()/sy);
		AffineTransform iaf=null;
		try {
			iaf=atrans.createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		ImageReader sd=new ImageReader();
		sd.srs=target;
		sd.atrans=af;
		sd.epsg=target_epsg;
		CoordinateTransformation ct2=BandUtil.getCoordinateTransformation(target,srs);
		sd.band=new BufferedImage(ww,hh,band.getType());
		int sw=sd.band.getWidth();
		int sh=sd.band.getHeight();
		for(int x=0;x<ww;x++) {
			for(int y=0;y<hh;y++) {
				Point2D p1=af.transform(new Point2D.Double(x,y), new Point2D.Double());
				double[] pt=ct2.TransformPoint(p1.getX(), p1.getY());
				Point2D p2=iaf.transform(new Point2D.Double(pt[0], pt[1]), new Point2D.Double());
				int xx=(int)p2.getX();
				int yy=(int)p2.getY();
				if(xx>=0&&xx<sw&&yy>=0&&yy<sh) {
					sd.band.setRGB(x, y, band.getRGB(xx, yy));
				}
			}
		}
		return sd;
	}
	
	public void setMaskImage(File mask) throws IOException {
		int col=Color.BLACK.getRGB();
		int val=new Color(0,0,0,0).getRGB();
		int w=band.getWidth();
		int h=band.getHeight();
		BufferedImage tmp=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
		
		BufferedImage img=ImageIO.read(mask);
		for(int i=0;i<w;i++) {
			for(int j=0;j<h;j++) {
				if(img.getRGB(i, j)==col) {
					tmp.setRGB(i, j, val);
				}else {
					tmp.setRGB(i, j, band.getRGB(i, j));
				}
			}
		}
		band=tmp;
	}
}
