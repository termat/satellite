package net.termat.geo.satellite;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.gdal.gdal.Band;
import org.gdal.gdal.ColorTable;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;
import org.jetbrains.bio.npy.NpyFile;

import net.termat.components.gradient.Gradient;
import net.termat.components.gradient.Range;

public class BandWriter {
	
	public static void writeNpy(BandReader sat,int channel,File out) {
		float[][] val=sat.getBand(channel);
		float[] data=new float[val.length*val[0].length];
		int k=0;
		for(int i=0;i<val.length;i++){
			for(int j=0;j<val[i].length;j++){
				data[k]=val[i][j];
				k++;
			}
		}
		int[] shape=new int[]{val[0].length,val.length};
		NpyFile.write(out.toPath(), data, shape);
	}
	
	public static void writeIndexedImage(BandReader sat,int channel,ColorTable cols,File out) throws IOException {
		float[][] d1=sat.getBand(channel);
		int xsize = d1.length;
		int ysize = d1[0].length;
		String name=out.getName().toLowerCase();
		if(name.endsWith(".png")||name.endsWith(".jpg")) {
			BufferedImage img=new BufferedImage(xsize,ysize,BufferedImage.TYPE_INT_RGB);
			Color[] col=new Color[cols.GetCount()];
			for(int i=0;i<col.length;i++) {
				col[i]=cols.GetColorEntry(i);
			}
			for(int y=0;y<ysize;y++) {
				for(int x=0;x<xsize;x++) {
					int v=(int)d1[x][y];
					if(v>=0&&v<col.length) {
						img.setRGB(x, y,col[v].getRGB());
					}
				}
			}
			if(name.endsWith(".png")) {
				ImageIO.write(img, "PNG", out);
				File out2=new File(out.getAbsolutePath().replace(".png", ".pgw").replace(".PNG", ".pgw"));
				BandUtil.writeTransform(sat.getTransform(),out2);
			}else {
				ImageIO.write(img, "JPG", out);
				File out2=new File(out.getAbsolutePath().replace(".jpg", ".jgw").replace(".JPG", ".jgw"));
				BandUtil.writeTransform(sat.getTransform(),out2);
			}
		}else if(name.endsWith(".tif")) {
			gdal.AllRegister();
			Driver driver =gdal.GetDriverByName("GTiff");
			String[] sOptions = new String[]{"COMPRESS=LZW","PREDICTOR=2"};
			Dataset dataset=driver.Create(out.getAbsolutePath(), xsize, ysize, 1, gdalconst.GDT_Byte,sOptions);
			dataset.SetGeoTransform(parseTransform(sat.getTransform()));
			dataset.SetProjection(sat.getSrs().ExportToWkt());
			Band bd = dataset.GetRasterBand(1);
			bd.SetColorTable(cols);
			byte[] byteArray = new byte[xsize];
			for(int j=0;j<ysize;j++) {
				for(int i=0;i<xsize;i++) {
					byteArray[i]=(byte)d1[i][j];
				}
				bd.WriteRaster(0, j, xsize, 1, byteArray);
			}
		}
	}
	
	public static void writeIndexedImage(BandReader sat,int channel,Color[] col,File out) throws IOException {
		float[][] d1=sat.getBand(channel);
		int xsize = d1.length;
		int ysize = d1[0].length;
		String name=out.getName().toLowerCase();
		if(name.endsWith(".png")||name.endsWith(".jpg")) {
			BufferedImage img=new BufferedImage(xsize,ysize,BufferedImage.TYPE_INT_RGB);
			for(int y=0;y<ysize;y++) {
				for(int x=0;x<xsize;x++) {
					int v=(int)d1[x][y];
					if(v>=0&&v<col.length) {
						img.setRGB(x, y,col[v].getRGB());
					}
				}
			}
			if(name.endsWith(".png")) {
				ImageIO.write(img, "PNG", out);
				File out2=new File(out.getAbsolutePath().replace(".png", ".pgw").replace(".PNG", ".pgw"));
				BandUtil.writeTransform(sat.getTransform(),out2);
			}else {
				ImageIO.write(img, "JPG", out);
				File out2=new File(out.getAbsolutePath().replace(".jpg", ".jgw").replace(".JPG", ".jgw"));
				BandUtil.writeTransform(sat.getTransform(),out2);
			}
		}else if(name.endsWith(".tif")) {
			gdal.AllRegister();
			Driver driver =gdal.GetDriverByName("GTiff");
			String[] sOptions = new String[]{"COMPRESS=LZW","PREDICTOR=2"};
			Dataset dataset=driver.Create(out.getAbsolutePath(), xsize, ysize, 1, gdalconst.GDT_Byte,sOptions);
			dataset.SetGeoTransform(parseTransform(sat.getTransform()));
			dataset.SetProjection(sat.getSrs().ExportToWkt());
			Band bd = dataset.GetRasterBand(1);
			ColorTable table=new ColorTable();
			for(int i=0;i<col.length;i++) {
				table.SetColorEntry(i, col[i]);
			}
			bd.SetColorTable(table);
			byte[] byteArray = new byte[xsize];
			for(int j=0;j<ysize;j++) {
				for(int i=0;i<xsize;i++) {
					byteArray[i]=(byte)d1[i][j];
				}
				bd.WriteRaster(0, j, xsize, 1, byteArray);
			}
		}
	}
	
	public static void writeSingleImage(BandReader sat,int channel,File out,boolean isCompress) throws IOException {
		float[][] d1=sat.getBand(channel);
		int xsize = d1.length;
		int ysize = d1[0].length;
		String name=out.getName().toLowerCase();
		if(name.endsWith(".png")||name.endsWith(".jpg")) {
			BufferedImage img=new BufferedImage(xsize,ysize,BufferedImage.TYPE_INT_RGB);
			double[] minmax1=getRegulerMinMax(calStat(d1));
			float range1=(float)(minmax1[1]-minmax1[0]);
			float min1=(float)minmax1[0];
			for(int y=0;y<ysize;y++) {
				for(int x=0;x<xsize;x++) {
					float v1=(float)d1[x][y];
					v1=Math.max(Math.min(1.0f,(float)((v1-min1)/range1)),0);
					Color c=new Color(v1,v1,v1);
					img.setRGB(x, y,c.getRGB());
				}
			}
			if(name.endsWith(".png")) {
				ImageIO.write(img, "PNG", out);
				File out2=new File(out.getAbsolutePath().replace(".png", ".pgw").replace(".PNG", ".pgw"));
				BandUtil.writeTransform(sat.getTransform(),out2);
			}else {
				ImageIO.write(img, "JPG", out);
				File out2=new File(out.getAbsolutePath().replace(".jpg", ".jgw").replace(".JPG", ".jgw"));
				BandUtil.writeTransform(sat.getTransform(),out2);
			}
		}else if(name.endsWith(".tif")) {
			gdal.AllRegister();
			Driver driver =gdal.GetDriverByName("GTiff");
			String[] sOptions=null;
			if(isCompress) {
				sOptions = new String[]{"COMPRESS=LZW","PREDICTOR=2"};
			}else {
				sOptions = new String[]{};
			}
			Dataset dataset=driver.Create(out.getAbsolutePath(), xsize, ysize, 1, gdalconst.GDT_Float32,sOptions);
			dataset.SetGeoTransform(parseTransform(sat.getTransform()));
			dataset.SetProjection(sat.getSrs().ExportToWkt());
			Band bd = dataset.GetRasterBand(1);
			float[] floatArray = new float[xsize];
			for(int j=0;j<ysize;j++) {
				for(int i=0;i<xsize;i++) {
					floatArray[i]=d1[i][j];
				}
				bd.WriteRaster(0, j, xsize, 1, floatArray);
			}
		}
	}
	
	public static void writeGeoTifImage(BandReader sat,File out) throws IOException {
		int n=sat.getBandNum();
		gdal.AllRegister();
		float[][] ff=sat.getBand(0);
		int xsize=ff.length;
		int ysize=ff[0].length;
		Driver driver =gdal.GetDriverByName("GTiff");
		String[] sOptions = new String[]{"COMPRESS=LZW","PREDICTOR=2"};
		Dataset dataset=driver.Create(out.getAbsolutePath(), xsize, ysize, n, gdalconst.GDT_Float32,sOptions);
		dataset.SetGeoTransform(parseTransform(sat.getTransform()));
		dataset.SetProjection(sat.getSrs().ExportToWkt());
		for(int l=0;l<n;l++) {
			float[][] d1=sat.getBand(l);
			Band bd = dataset.GetRasterBand(l+1);
			float[] floatArray = new float[xsize];
			for(int j=0;j<ysize;j++) {
				for(int i=0;i<xsize;i++) {
					floatArray[i]=d1[i][j];
				}
				bd.WriteRaster(0, j, xsize, 1, floatArray);
			}
		}
		
	}
	
	public static void writeMultiImage(BandReader sat,int r,int g,int b,File out) throws IOException {
		float[][] d1=sat.getBand(r);
		float[][] d2=sat.getBand(g);
		float[][] d3=sat.getBand(b);
		int xsize = d1.length;
		int ysize = d1[0].length;
		String name=out.getName().toLowerCase();
		if(name.endsWith(".png")||name.endsWith(".jpg")) {
			BufferedImage img=new BufferedImage(xsize,ysize,BufferedImage.TYPE_INT_RGB);
			double[] minmax1=getRegulerMinMax(calStat(d1));
			float range1=(float)(minmax1[1]-minmax1[0]);
			float min1=(float)minmax1[0];
			double[] minmax2=getRegulerMinMax(calStat(d2));
			float range2=(float)(minmax2[1]-minmax2[0]);
			float min2=(float)minmax2[0];
			double[] minmax3=getRegulerMinMax(calStat(d3));
			float range3=(float)(minmax3[1]-minmax3[0]);
			float min3=(float)minmax3[0];
			for(int y=0;y<ysize;y++) {
				for(int x=0;x<xsize;x++) {
					float v1=(float)d1[x][y];
					v1=Math.max(Math.min(1.0f,(float)((v1-min1)/range1)),0);
					float v2=(float)d2[x][y];
					v2=Math.max(Math.min(1.0f,(float)((v2-min2)/range2)),0);
					float v3=(float)d3[x][y];
					v3=Math.max(Math.min(1.0f,(float)((v3-min3)/range3)),0);
					Color c=new Color(v1,v2,v3);
					img.setRGB(x, y,c.getRGB());
				}
			}
			if(name.endsWith(".png")) {
				ImageIO.write(img, "PNG", out);
				File out2=new File(out.getAbsolutePath().replace(".png", ".pgw").replace(".PNG", ".pgw"));
				BandUtil.writeTransform(sat.getTransform(),out2);
			}else {
				ImageIO.write(img, "JPG", out);
				File out2=new File(out.getAbsolutePath().replace(".jpg", ".jgw").replace(".JPG", ".jgw"));
				BandUtil.writeTransform(sat.getTransform(),out2);
			}
		}else if(name.endsWith(".tif")) {
			gdal.AllRegister();
			Driver driver =gdal.GetDriverByName("GTiff");
			String[] sOptions = new String[]{"COMPRESS=LZW","PREDICTOR=2"};
			Dataset dataset=driver.Create(out.getAbsolutePath(), xsize, ysize, 3, gdalconst.GDT_Float32,sOptions);
			dataset.SetGeoTransform(parseTransform(sat.getTransform()));
			dataset.SetProjection(sat.getSrs().ExportToWkt());
			Band bd = dataset.GetRasterBand(1);
			float[] floatArray = new float[xsize];
			for(int j=0;j<ysize;j++) {
				for(int i=0;i<xsize;i++) {
					floatArray[i]=d1[i][j];
				}
				bd.WriteRaster(0, j, xsize, 1, floatArray);
			}
			bd = dataset.GetRasterBand(2);
			for(int j=0;j<ysize;j++) {
				for(int i=0;i<xsize;i++) {
					floatArray[i]=d2[i][j];
				}
				bd.WriteRaster(0, j, xsize, 1, floatArray);
			}
			bd = dataset.GetRasterBand(3);
			for(int j=0;j<ysize;j++) {
				for(int i=0;i<xsize;i++) {
					floatArray[i]=d3[i][j];
				}
				bd.WriteRaster(0, j, xsize, 1, floatArray);
			}
		}
	}
	
	public static void writeImage(ImageReader ir,File out) throws IOException {
		String name=out.getName().toLowerCase();
		BufferedImage img=ir.getBand();
		AffineTransform af=ir.getTransform();
		if(name.endsWith(".jpg")) {
			ImageIO.write(img, "JPG",out);
			File gw=new File(out.getAbsolutePath().replace(".jpg", ".jgw").replace(".JPG", ".jgw"));
			BandUtil.writeTransform(af,gw);
		}else if(name.endsWith(".png")) {
			ImageIO.write(img, "PNG",out);
			File gw=new File(out.getAbsolutePath().replace(".png", ".pgw").replace(".PNG", ".pgw"));
			BandUtil.writeTransform(af,gw);
		}else if(name.endsWith(".tif")) {
			gdal.AllRegister();
			int xsize=img.getWidth();
			int ysize=img.getHeight();
			Driver driver =gdal.GetDriverByName("GTiff");
			String[] sOptions = new String[]{"COMPRESS=LZW","PREDICTOR=2"};
			Dataset dataset=driver.Create(out.getAbsolutePath(), xsize, ysize, 3, gdalconst.GDT_Byte,sOptions);
			dataset.SetGeoTransform(parseTransform(af));
			dataset.SetProjection(ir.getSrs().ExportToWkt());
			Band b1 = dataset.GetRasterBand(1);
			Band b2 = dataset.GetRasterBand(2);
			Band b3 = dataset.GetRasterBand(3);
			byte[][] array = new byte[3][xsize];
			for(int j=0;j<ysize;j++) {
				for(int i=0;i<xsize;i++) {
					Color col=new Color(img.getRGB(i, j));
					array[0][i]=(byte)col.getRed();
					array[1][i]=(byte)col.getGreen();
					array[2][i]=(byte)col.getBlue();
				}
				b1.WriteRaster(0, j, xsize, 1, array[0]);
				b2.WriteRaster(0, j, xsize, 1, array[1]);
				b3.WriteRaster(0, j, xsize, 1, array[2]);
			}
		}
	}

	public static BufferedImage createImage(float[][] ff,Gradient grad,Range range) {
		BufferedImage img=new BufferedImage(ff.length,ff[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<ff.length;i++) {
			for(int j=0;j<ff[0].length;j++) {
				img.setRGB(i, j, grad.getColorByInt(range.getValue(ff[i][j])));
			}
		}
		return img;
	}
	
	public static BufferedImage createImage(float[][][] ff,Range[] range) {
		BufferedImage img=new BufferedImage(ff[0].length,ff[0][0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<ff[0].length;i++) {
			for(int j=0;j<ff[0][0].length;j++) {
				float c1=(float)range[0].getValue(ff[0][i][j]);
				float c2=(float)range[1].getValue(ff[1][i][j]);
				float c3=(float)range[2].getValue(ff[2][i][j]);
				img.setRGB(i, j, new Color(c1,c2,c3).getRGB());
			}
		}
		return img;
	}
	
	public static void imageToTiff(int epsg,File in,File tif) throws IOException {
		BufferedImage img=ImageIO.read(in);
		AffineTransform af=BandUtil.loadTransform(new File(in.getAbsolutePath().replace(".png", ".pgw")
				.replace(".PNG", ".pgw").replace(".jpg", ".jgw").replace(".JPG", ".jgw")));
		gdal.AllRegister();
		int xsize=img.getWidth();
		int ysize=img.getHeight();
		Driver driver =gdal.GetDriverByName("GTiff");
		String[] sOptions = new String[]{"COMPRESS=LZW","PREDICTOR=2"};
		Dataset dataset=driver.Create(tif.getAbsolutePath(), xsize, ysize, 3, gdalconst.GDT_Byte,sOptions);
		dataset.SetGeoTransform(parseTransform(af));
		SpatialReference srs=new SpatialReference();
		srs.ImportFromEPSG(epsg);
		dataset.SetProjection(srs.ExportToWkt());
		Band b1 = dataset.GetRasterBand(1);
		Band b2 = dataset.GetRasterBand(2);
		Band b3 = dataset.GetRasterBand(3);
		byte[][] array = new byte[3][xsize];
		for(int j=0;j<ysize;j++) {
			for(int i=0;i<xsize;i++) {
				Color col=new Color(img.getRGB(i, j));
				array[0][i]=(byte)col.getRed();
				array[1][i]=(byte)col.getGreen();
				array[2][i]=(byte)col.getBlue();
			}
			b1.WriteRaster(0, j, xsize, 1, array[0]);
			b2.WriteRaster(0, j, xsize, 1, array[1]);
			b3.WriteRaster(0, j, xsize, 1, array[2]);
		}
	}

	private static double[] getRegulerMinMax(float[] b) {
		float min=b[0];
		float max=b[1];
		float mean=b[2];
		float std=b[3];
		double minV=Math.max(min, mean-std*3);
		double maxV=Math.min(max, mean+std*3);
		return new double[] {minV,maxV};
	}
	
	private static double[] parseTransform(AffineTransform af) {
		return new double[] {af.getTranslateX(),af.getScaleX(),0,af.getTranslateY(),0,af.getScaleY()};
	}
	
	private static float[] calStat(float[][] data) {
		float min=Float.MAX_VALUE;
		float max=-Float.MAX_VALUE;
		float sum=0;
		int n=0;
		for(int i=0;i<data.length;i++) {
			for(int j=0;j<data[i].length;j++) {
				if(!Float.isNaN(data[i][j])) {
					min=Math.min(min, data[i][j]);
					max=Math.max(max, data[i][j]);
					sum +=data[i][j];
					n++;
				}
			}
		}
		float ave=(float)sum/(float)n;
		float ss=0;
		for(int i=0;i<data.length;i++) {
			for(int j=0;j<data[i].length;j++) {
				if(!Float.isNaN(data[i][j])) {
					ss +=Math.pow(data[i][j]-ave,2);
				}
			}
		}
		float std;
		if(n==1) {
			std=(float)Math.sqrt(ss/(float)n);
		}else {
			std=(float)Math.sqrt(ss/((float)n-1));
		}
		return new float[] {min,max,ave,std};
	}
}
