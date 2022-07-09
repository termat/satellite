package net.termat.geo.satellite;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.gdal.gdal.ColorTable;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;
import org.gdal.osr.osrConstants;

import net.termat.geo.util.PCUtil;

public class BandUtil {

	public static Point2D xyToLonlat(int num,double xx,double yy){
		return LonLatXY.xyToLonlat(num,xx,yy);
	}
	
	public static Point2D lonlatToXY(int num,double lon,double lat){
		return LonLatXY.lonlatToXY(num, lon, lat);
	}
	
	public static SpatialReference createSpatialReference(int epsg) {
		SpatialReference srs=new SpatialReference();
		srs.ImportFromEPSG(epsg);
		srs.SetAxisMappingStrategy(osrConstants.OAMS_TRADITIONAL_GIS_ORDER);
		return srs;
	}
	
	public static CoordinateTransformation getCoordinateTransformation(SpatialReference src,SpatialReference target) {
		return osr.CreateCoordinateTransformation(src, target);
	}
	
	public static CoordinateTransformation getCoordinateTransformation(int src,int target) {
		return osr.CreateCoordinateTransformation(
				createSpatialReference(src), createSpatialReference(target));
	}
	
	public static AffineTransform loadTransform(File path)throws IOException{
		BufferedReader br=new BufferedReader(new FileReader(path));
		List<Double> dd=new ArrayList<Double>();
		String line=null;
		while((line=br.readLine())!=null){
			double d=Double.parseDouble(line);
			dd.add(d);
		}
		br.close();
		double[] p=new double[dd.size()];
		for(int i=0;i<p.length;i++){
			p[i]=dd.get(i);
		}
		return new AffineTransform(p);
	}
	
	public static void writeTransform(AffineTransform af,File path)throws IOException{
		BufferedWriter bw=new BufferedWriter(new FileWriter((path)));
		bw.write(af.getScaleX()+"\n");
		bw.write(af.getShearX()+"\n");
		bw.write(af.getShearY()+"\n");
		bw.write(af.getScaleY()+"\n");
		bw.write(af.getTranslateX()+"\n");
		bw.write(af.getTranslateY()+"\n");
		bw.flush();
		bw.close();
	}
	
	public static void transTifToPng(BandReader br,int ch,DataTranslator trans,File out) throws IOException {
		float[][] val=br.getBand(ch);
		BufferedImage img=new BufferedImage(val.length,val[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<val.length;i++) {
			for(int j=0;j<val[0].length;j++) {
				img.setRGB(i, j, trans.getRGB(val[i][j]));
			}
		}
		ImageIO.write(img, "png", out);
		writeTransform(br.getTransform(),new File(out.getAbsolutePath().replace(".png", ".pgw")));
	}
	
	public interface DataTranslator {
		public int getRGB(float val);
	}
	
	public static BandReader demPng2Tif(int epsg,File png) throws IOException {
		AffineTransform af=loadTransform(new File(png.getAbsolutePath().replace(".png", ".pgw")));
		BufferedImage img=ImageIO.read(png);
		float[][] val=new float[img.getWidth()][img.getHeight()];
		for(int i=0;i<val.length;i++) {
			for(int j=0;j<val[0].length;j++) {
				double zz=PCUtil.getZ(img.getRGB(i, j));
				if(Double.isNaN(zz)) {
					val[i][j]=0.0f;
				}else {
					val[i][j]=(float)zz;
				}
			}
		}
		return BandReader.createReader(epsg, af, val);
	}
	
	public static void demTif2Png(File tif) throws IOException {
		BandReader br=BandReader.createReader(tif);
		AffineTransform af=br.getTransform();
		float[][] ff=br.getBand(0);
		BufferedImage img=new BufferedImage(ff.length,ff[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<ff.length;i++) {
			for(int j=0;j<ff[0].length;j++) {
				int col=PCUtil.getRGB(ff[i][j]);
				img.setRGB(i, j, col);
			}
		}
		ImageIO.write(img, "png", new File(tif.getAbsolutePath().replace(".tif", ".png")));
		PCUtil.writeTransform(af, new File(tif.getAbsolutePath().replace(".tif", ".pgw")));
	}
	
	public static void tif2Png(File tif) throws IOException {
		BandReader br=BandReader.createReader(tif);
		AffineTransform af=br.getTransform();
		float[][] f1=br.getBand(0);
		float[][] f2=br.getBand(1);
		float[][] f3=br.getBand(2);
		float[] s1=getStat(f1);
		float[] s2=getStat(f2);
		float[] s3=getStat(f3);
		BufferedImage img=new BufferedImage(f1.length,f1[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<f1.length;i++) {
			for(int j=0;j<f1[0].length;j++) {
				float v1=(f1[i][j]-s1[4])/(s1[5]-s1[4]);
				float v2=(f2[i][j]-s2[4])/(s2[5]-s2[4]);
				float v3=(f3[i][j]-s3[4])/(s3[5]-s3[4]);
				int i1=(int)Math.max(Math.min(255, v1*255),0);
				int i2=(int)Math.max(Math.min(255, v2*255),0);
				int i3=(int)Math.max(Math.min(255, v3*255),0);
				Color c=new Color(i1,i2,i3);
				img.setRGB(i, j, c.getRGB());
			}
		}
		ImageIO.write(img, "png", new File(tif.getAbsolutePath().replace(".tif", ".png")));
		PCUtil.writeTransform(af, new File(tif.getAbsolutePath().replace(".tif", ".pgw")));
	}
	
	public static void tif2PngIndexed(File tif) throws IOException {
		BandReader br=BandReader.createReader(tif);
		AffineTransform af=br.getTransform();
		float[][] f1=br.getBand(0);
		ColorTable tb=br.getColorTable(0);
		BufferedImage img=new BufferedImage(f1.length,f1[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<f1.length;i++) {
			for(int j=0;j<f1[0].length;j++) {
				Color c=tb.GetColorEntry((int)f1[i][j]);
				img.setRGB(i, j, c.getRGB());
			}
		}
		ImageIO.write(img, "png", new File(tif.getAbsolutePath().replace(".tif", ".png")));
		PCUtil.writeTransform(af, new File(tif.getAbsolutePath().replace(".tif", ".pgw")));
	}
	
	private static float[] getStat(float[][] b) {
		float ave=0;
		float num=0;
		float min=100000;
		float max=-100000;
		for(int i=0;i<b.length;i++) {
			for(int j=0;j<b[0].length;j++) {
				if(Float.isNaN(b[i][j]))continue;
				ave +=b[i][j];
				num++;
				min=Math.min(min, b[i][j]);
				max=Math.max(max, b[i][j]);
			}
		}
		ave=ave/num;
		float val=0;
		for(int i=0;i<b.length;i++) {
			for(int j=0;j<b[0].length;j++) {
				if(Float.isNaN(b[i][j]))continue;
				val +=(b[i][j]-ave)*(b[i][j]-ave);
			}
		}
		val=val/num;
		val=(float)Math.sqrt(val);
		return new float[] {ave,val,min,max,Math.max(min, ave-val*3),Math.min(max, ave+val*3)};
	}
	
	public static void main(String[] args) throws IOException {
		demTif2Png(new File("E:\\業務\\四国中央市\\AD3D_DSM.tif"));
	}
}
