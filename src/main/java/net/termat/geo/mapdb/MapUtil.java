package net.termat.geo.mapdb;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;

import net.termat.geo.satellite.BandUtil;

public class MapUtil {
	public static int NA=new Color(128,0,0).getRGB();
	
	public static void demPngToTif(int epsg,File png,File out) throws IOException {
		BufferedImage img=ImageIO.read(png);
		AffineTransform af=BandUtil.loadTransform(new File(png.getAbsolutePath().replace(".png", ".pgw").replace(".PNG", ".pgw")));
		gdal.AllRegister();
		Driver driver =gdal.GetDriverByName("GTiff");
		String[] sOptions = new String[]{"COMPRESS=LZW","PREDICTOR=2"};
		int xsize=img.getWidth();
		int ysize=img.getHeight();
		Dataset dataset=driver.Create(out.getAbsolutePath(), xsize, ysize, 1, gdalconst.GDT_Float32,sOptions);
		dataset.SetGeoTransform(parseTransform(af));
		SpatialReference srs=new SpatialReference();
		srs.ImportFromEPSG(epsg);
		dataset.SetProjection(srs.ExportToWkt());
		Band bd = dataset.GetRasterBand(1);
		float[] floatArray = new float[img.getWidth()];
		for(int j=0;j<ysize;j++) {
			for(int i=0;i<xsize;i++) {
				int col=img.getRGB(i, j);
				double z=getZ(col);
				if(!Double.isNaN(z)) {
					floatArray[i]=(float)z;
				}
			}
			bd.WriteRaster(0, j, xsize, 1, floatArray);
		}
	}
	
	private static double[] parseTransform(AffineTransform af) {
		return new double[] {af.getTranslateX(),af.getScaleX(),0,af.getTranslateY(),0,af.getScaleY()};
	}

	/**
	 * RGB（int)を標高に変換するメソッド
	 *
	 * @param  color RGB:int
	 * @return 標高:double
	 */
	public static double getZ(int color){
		color=(color << 8) >> 8;
		if(color==8388608||color==-8388608){
			return Double.NaN;
		}else if(color<8388608){
			return color * 0.01;
		}else{
			return (color-16777216)*0.01;
		}
	}

	/**
	 * 標高をRGB（INT）に変換するメソッド
	 *
	 * @param  z 標高 :double
	 * @return RGB:int
	 */
	public static int getRGB(double z){
		if(Double.isNaN(z)){
			return NA;
		}else if(z<-83886||z>83886) {
			return NA;
		}else{
			int i=(int)Math.round(z*100);
			if(z<0)	i=i+0x1000000;
			int r=Math.max(0,Math.min(i >> 16,255));
			int g=Math.max(0,Math.min(i-(r << 16) >> 8,255));
			int b=Math.max(0,Math.min(i-((r << 16)+(g << 8)),255));
			return new Color(r,g,b).getRGB();
		}
	}
	
	public static int getRGBmapbox(double h){
	    int rgb = (int)Math.floor((h + 10000)/0.1);
	    int r= (rgb & 0xff0000) >> 16;
		int g=(rgb & 0x00ff00) >> 8;
		int b=(rgb & 0x0000ff);
		return new Color(r,g,b).getRGB();
	}
	
	public static double getZmapbox(int color){
		color=(color << 8) >> 8;
		if(color==8388608||color==-8388608){
			return Double.NaN;
		}else if(color<8388608){
			return color * 0.01;
		}else{
			return (color-16777216)*0.01;
		}
	}
	
	public static void main(String[] args) {
		File f=new File("F:\\衛星\\鳥取\\data\\JGD_2020_DEM.png");
		File o=new File("F:\\衛星\\鳥取\\data\\JGD_2020_DEM.tif");
		try {
			demPngToTif(6673,f,o);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
