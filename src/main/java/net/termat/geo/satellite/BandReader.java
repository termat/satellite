package net.termat.geo.satellite;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.gdal.gdal.Band;
import org.gdal.gdal.ColorTable;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.GCP;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osrConstants;

import net.termat.components.gradient.Gradient;
import net.termat.components.gradient.Range;

public class BandReader {
	private SpatialReference srs;
	private AffineTransform atrans;
	private List<float[][]> band;
	private List<ColorTable> table;
	private int epsg;
	private List<String> chName;
	
	private BandReader() {
		gdal.AllRegister();
		band=new ArrayList<>();
		chName=new ArrayList<>();
		table=new ArrayList<>();
	}
	
	public static BandReader createReader(File f) {
		BandReader ret=new BandReader();
		ret.readFile(f);
		return ret;
	}
	
	public static BandReader createReaderAsByte(File f) {
		BandReader ret=new BandReader();
		ret.readFileByte(f);
		return ret;
	}
	
	public static BandReader createReader(int epsg,AffineTransform af,float[][] val) {
		BandReader ret=new BandReader();
		ret.srs=BandUtil.createSpatialReference(epsg);
		ret.epsg=epsg;
		ret.atrans=af;
		ret.table.add(null);
		ret.chName.add("Channel-1");
		ret.band.add(val);
		return ret;
	}

	private void readFileByte(File f) {
		Dataset data=gdal.Open(f.getAbsolutePath());
		srs=new SpatialReference(data.GetProjection());
		srs.SetAxisMappingStrategy(osrConstants.OAMS_TRADITIONAL_GIS_ORDER);
		try {
			epsg=Integer.parseInt(srs.GetAttrValue("AUTHORITY",1));
			double[] tr=data.GetGeoTransform();
			atrans=createTransform(tr);
			int bandNum=data.GetRasterCount();
			for(int i=0;i<bandNum;i++) {
				Band b=data.GetRasterBand(i+1);
				table.add(b.GetColorTable());
				int w=b.getXSize();
				int h=b.getYSize();
				byte[] array=new byte[w*h];
				b.ReadRaster(0, 0, w, h, gdalconst.GDT_Byte, array);				
				band.add(transArray(array, w, h));
				chName.add("Channel-"+Integer.toString(i+1));
			}
		}catch(java.lang.NumberFormatException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void readFile(File f) {
		Dataset data=gdal.Open(f.getAbsolutePath());
		srs=new SpatialReference(data.GetProjection());
		srs.SetAxisMappingStrategy(osrConstants.OAMS_TRADITIONAL_GIS_ORDER);
		try {
			epsg=Integer.parseInt(srs.GetAttrValue("AUTHORITY",1));
			double[] tr=data.GetGeoTransform();
			atrans=createTransform(tr);
			int bandNum=data.GetRasterCount();
			for(int i=0;i<bandNum;i++) {
				Band b=data.GetRasterBand(i+1);
				table.add(b.GetColorTable());
				int w=b.getXSize();
				int h=b.getYSize();
				float[] array=new float[w*h];
				b.ReadRaster(0, 0, w, h, gdalconst.GDT_Float32, array);
//				byte[] array=new byte[w*h];
//				b.ReadRaster(0, 0, w, h, gdalconst.GDT_Byte, array);
				band.add(transArray(array, w, h));
				chName.add("Channel-"+Integer.toString(i+1));
			}
		}catch(java.lang.NumberFormatException e) {
			epsg=4326;
			srs=BandUtil.createSpatialReference(epsg);
			srs.SetAxisMappingStrategy(osrConstants.OAMS_TRADITIONAL_GIS_ORDER);
			Vector<GCP> vec=(Vector<GCP>)data.GetGCPs();
			GCPMapper map=new GCPMapper(vec);
			Band b=data.GetRasterBand(1);
			Rectangle2D rect=map.getBounds();
			int w=b.getXSize();
			int h=b.getYSize();
			float[] array=new float[w*h];
			b.ReadRaster(0, 0, w, h, gdalconst.GDT_Float32, array);
			float[][] val=transArray(array, w, h);
			atrans=new AffineTransform(new double[] {
				rect.getWidth()/(double)w,0,0,-rect.getHeight()/(double)h,rect.getX(),rect.getY()+rect.getHeight()});
//			double[] dd=new double[6];
//			gdal.GCPsToGeoTransform(vec.toArray(new GCP[vec.size()]), dd);
			try {
				AffineTransform iaf=atrans.createInverse();
				float[][] tmp=new float[w][h];
				for(int i=0;i<w;i++) {
					for(int j=0;j<h;j++) {
						double[] xy=map.getXY(i, j);
//						double dx=(dd[0]+i*dd[1]+j*dd[2]);
//						double dy=(dd[3]+i*dd[4]+j*dd[5]);
//						double[] xy=new double[] {dx,dy};
						Point2D p=iaf.transform(new Point2D.Double(xy[0],xy[1]),new Point2D.Double());
						int px=(int)Math.floor(p.getX());
						int py=(int)Math.floor(p.getY());
						if(px>=0&&px<w&&py>=0&&py<h) {
							tmp[px][py]=val[i][j];
						}
					}
				}
				band.add(tmp);
				chName.add("Channel-1");
			} catch (NoninvertibleTransformException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public void addBand(File f) {
		float[][] base=band.get(0);
		Dataset data=gdal.Open(f.getAbsolutePath());
		double[] tr=data.GetGeoTransform();
		AffineTransform af2=createTransform(tr);
		int bandNum=data.GetRasterCount();
		for(int i=0;i<bandNum;i++) {
			Band b=data.GetRasterBand(i+1);
			table.add(b.GetColorTable());
			int w=b.getXSize();
			int h=b.getYSize();
			float[] array=new float[w*h];
			b.ReadRaster(0, 0, w, h, gdalconst.GDT_Float32, array);
			if(base.length!=w||base[0].length!=h) {
				try {
					float[][] val=integrateResolution(array,new Dimension(w,h),atrans,af2,base.length,base[0].length);
					band.add(val);
				} catch (NoninvertibleTransformException e) {
					e.printStackTrace();
				}
			}else {
				band.add(transArray(array, base.length, base[0].length));
			}
			double[] min=new double[1];
			double[] max=new double[1];
			double[] mean=new double[1];
			double[] std=new double[1];
			b.ComputeStatistics(true, min, max, mean, std);
			chName.add("Cannel-"+Integer.toString(band.size()));
		}
	}
	
	public void addNDI(int firstVal,int secondVal,String name) {
		float[][] ir=band.get(firstVal);
		float[][] r=band.get(secondVal);
		float[][] ret=new float[ir.length][ir[0].length];
		for(int i=0;i<ret.length;i++) {
			for(int j=0;j<ret[0].length;j++) {
				if(ir[i][j]+r[i][j]==0) {
					ret[i][j]=-1.0f;
				}else {
					ret[i][j]=(float)Math.round(((ir[i][j]-r[i][j])/(ir[i][j]+r[i][j]))*100)/100;
				}
			}
		}
		band.add(ret);
		chName.add(name);
		table.add(null);
	}
	
	public BandReader createProjectionData(int target_epsg) {
		if(target_epsg==epsg)return this;
		SpatialReference target=BandUtil.createSpatialReference(target_epsg);
		CoordinateTransformation ct=BandUtil.getCoordinateTransformation(srs,target);
		Rectangle2D rect=null;
		float[][] b=band.get(0);
		for(int x=0;x<b.length;x++) {
			for(int y=0;y<b[0].length;y++) {
				Point2D sp=atrans.transform(new Point2D.Double(x,y), new Point2D.Double());
				double[] p2=ct.TransformPoint(sp.getX(), sp.getY());
				if(rect==null) {
					rect=new Rectangle2D.Double(p2[0],p2[1],0,0);
				}else {
					rect.add(p2[0],p2[1]);
				}
			}
		}
		double sx=rect.getWidth()/b.length;
		double sy=rect.getHeight()/b[0].length;
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
		BandReader sd=new BandReader();
		sd.srs=target;
		sd.atrans=af;
		sd.epsg=target_epsg;
		sd.chName.addAll(chName);
		sd.table.addAll(table);
		CoordinateTransformation ct2=BandUtil.getCoordinateTransformation(target,srs);
		for(float[][] array : band) {
			int sw=array.length;
			int sh=array[0].length;
			float[][] tmp=new float[ww][hh];
			for(int x=0;x<ww;x++) {
				for(int y=0;y<hh;y++) {
					Point2D p1=af.transform(new Point2D.Double(x,y), new Point2D.Double());
					double[] pt=ct2.TransformPoint(p1.getX(), p1.getY());
					Point2D p2=iaf.transform(new Point2D.Double(pt[0], pt[1]), new Point2D.Double());
					int xx=(int)p2.getX();
					int yy=(int)p2.getY();
					if(xx>=0&&xx<sw&&yy>=0&&yy<sh) {
						tmp[x][y]=array[xx][yy];
					}
				}
			}
			sd.band.add(tmp);
		}
		return sd;
	}
	
	public static BandReader connectReader(BandReader src,BandReader dst) {
		if(src.band.size()!=dst.band.size()) {
			throw new IllegalArgumentException("Band numbers do not match");
		}else if(src.epsg!=dst.epsg) {
			throw new IllegalArgumentException("EPSG do not match");
		}
		BandReader sd=new BandReader();
		sd.srs=src.srs;
		sd.epsg=src.epsg;
		AffineTransform af1=src.atrans;
		AffineTransform af2=dst.atrans;
		Rectangle2D rect=null;
		List<float[][]> li=new ArrayList<>();
		int bandNum=src.band.size();
		for(int i=0;i<bandNum;i++) {
			float[][] b1=src.getBand(i);
			float[][] b2=dst.getBand(i);
			if(rect==null)rect=getAppendBounds(b1,af1,b2,af2);
			float[][] newval=appendDataPoint(rect,b1,af1,b2,af2);
			li.add(newval);
		}
		double[] param=new double[] {af1.getScaleX(),0,0,af1.getScaleY(),rect.getX(),rect.getY()+rect.getHeight()};
		sd.atrans=new AffineTransform(param);
		sd.band.clear();
		sd.band.addAll(li);
		sd.table.addAll(src.table);
		sd.chName.addAll(src.chName);
		return sd;
	}
	
	public BandReader createSubImage(Rectangle2D rect) {
		double[] param=new double[] {atrans.getScaleX(),0,0,atrans.getScaleY(),rect.getX(),rect.getY()+rect.getHeight()};
		AffineTransform af=new AffineTransform(param);
		BandReader sd=new BandReader();
		sd.srs=srs;
		sd.epsg=epsg;
		sd.atrans=af;
		int bandNum=band.size();
		for(int i=0;i<bandNum;i++) {
			float[][] b1=getBand(i,rect);
			sd.band.add(b1);
		}
		sd.table.addAll(table);
		sd.chName.addAll(chName);
		return sd;
	}
	
	public BandReader createSubImage(Rectangle2D rect,double res) {
		double[] param=new double[] {res,0,0,-res,rect.getX(),rect.getY()+rect.getHeight()};
		AffineTransform af=new AffineTransform(param);
		BandReader sd=new BandReader();
		sd.srs=srs;
		sd.epsg=epsg;
		sd.atrans=af;
		int bandNum=band.size();
		for(int i=0;i<bandNum;i++) {
			float[][] b1=getBand(i,rect,res);
			sd.band.add(b1);
		}
		sd.table.addAll(table);
		sd.chName.addAll(chName);
		return sd;
	}
	
	public float[][] createSubData(Rectangle2D r,int xsize,int ysize,int channael) {
		double sx=r.getWidth()/(double)xsize;
		double sy=r.getHeight()/(double)ysize;
		AffineTransform iaf=atrans;
		try {
			iaf=iaf.createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		AffineTransform at=new AffineTransform(new double[] {sx,0,0,-sy,r.getX(),r.getY()+r.getHeight()});
		float[][] src=band.get(channael);
		float[][] ret=new float[xsize][ysize];
		for(int i=0;i<xsize;i++) {
			for(int j=0;j<ysize;j++) {
				Point2D p=at.transform(new Point2D.Double(i,j), new Point2D.Double());
				p=iaf.transform(p, new Point2D.Double());
				int xx=(int)Math.floor(p.getX());
				int yy=(int)Math.floor(p.getY());
				if(xx>=0&&xx<src.length&&yy>=0&&yy<src[0].length) {
					ret[i][j]=src[xx][yy];
				}
			}
		}
		return ret;
	}
	
	public SpatialReference getSrs() {
		return srs;
	}

	public int getBandNum() {
		return band.size();
	}
	
	public float[][] getBand(int i){
		return band.get(i);
	}
	
	public float[][] getPointValue(int i,Shape sp){
		List<float[]> ret=new ArrayList<>();
		float[][] src=band.get(i);
		for(int x=0;x<src.length;x++) {
			for(int y=0;y<src[0].length;y++) {
				Point2D p=atrans.transform(new Point2D.Double(x, y), new Point2D.Double());
				if(sp.contains(p)) {
					ret.add(new float[] {(float)p.getX(),(float)p.getY(),src[x][y]});
				}
			}
		}
		return ret.toArray(new float[ret.size()][]);
	}
	
	public float[][] getBand(int i,Rectangle2D rect) {
		float[][] src=band.get(i);
		int w=(int)Math.round(rect.getWidth()/atrans.getScaleX());
		int h=(int)Math.abs(Math.round(rect.getHeight()/atrans.getScaleY()));
		float[][] ret=new float[w][h];
		double[] param=new double[] {atrans.getScaleX(),0,0,atrans.getScaleY(),rect.getX(),rect.getY()+rect.getHeight()};
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
				int xx=(int)Math.floor(p.getX());
				int yy=(int)Math.floor(p.getY());
				if(xx>=0&&xx<src.length&&yy>=0&&yy<src[0].length) {
					ret[x][y]=src[xx][yy];
				}
			}
		}
		return ret;
	}
	
	public float[][] getBand(int i,Rectangle2D rect,double res) {
		float[][] src=band.get(i);
		int w=(int)Math.round(rect.getWidth()/res);
		int h=(int)Math.abs(Math.round(rect.getHeight()/res));
		float[][] ret=new float[w][h];
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
				int xx=(int)Math.floor(p.getX());
				int yy=(int)Math.floor(p.getY());
				if(xx>=0&&xx<src.length&&yy>=0&&yy<src[0].length) {
					ret[x][y]=src[xx][yy];
				}
			}
		}
		return ret;
	}
	
	public ColorTable getColorTable(int i) {
		return table.get(i);
	}
	
	public AffineTransform getTransform() {
		return atrans;
	}
	
	public Rectangle2D getBounds() {
		float[][] ff=band.get(0);
		Rectangle2D ret=new Rectangle2D.Double(0,0,ff.length,ff[0].length);
		return atrans.createTransformedShape(ret).getBounds2D();
	}
	
	public int getEPSG() {
		return epsg;
	}
	
	public String[] getChannelNames() {
		return chName.toArray(new String[chName.size()]);
	}
	
	public String getChannelName(int i) {
		return chName.get(i);
	}
	
	public void setChannelName(int i,String name) {
		chName.remove(i);
		chName.add(i, name);
	}
	
	public String getCRSName() {
		return srs.GetName();
	}
	
	public void setMaskImage(File mask) throws IOException {
		int col=Color.BLACK.getRGB();
		float[][] val=band.get(0);
		BufferedImage img=ImageIO.read(mask);
		for(int i=0;i<val.length;i++) {
			for(int j=0;j<val[i].length;j++) {
				if(img.getRGB(i, j)==col) {
					for(float[][] b : band) {
						b[i][j]=Float.NaN;
					}
				}
			}
		}
	}
	
	private float[][] integrateResolution(float[] src,Dimension sd,AffineTransform af1,AffineTransform af2,int w,int h) throws NoninvertibleTransformException {
		float[][] ret=new float[w][h];
		AffineTransform iaf2=af2.createInverse();
		for(int y=0;y<h;y++) {
			for(int x=0;x<w;x++) {
				Point2D p1=af1.transform(new Point2D.Double(x,y), new Point2D.Double());
				Point2D p2=iaf2.transform(p1, new Point2D.Double());
				int xx=(int)p2.getX();
				int yy=(int)p2.getY();
				if(xx>=0&&xx<w&yy>=0&&yy<h) {
					if(yy*sd.width+xx<src.length)ret[x][y]=src[yy*sd.width+xx];
				}
			}
		}
		return ret;
	}
	
	private static float[][] transArray(float[] array,int w,int h) {
		float[][] ret=new float[w][h];
		int it=0;
		for(int i=0;i<h;i++) {
			for(int j=0;j<w;j++) {
				ret[j][i]=array[it++];
			}
		}
		return ret;
	}
	
	private static float[][] transArray(byte[] array,int w,int h) {
		float[][] ret=new float[w][h];
		int it=0;
		for(int i=0;i<h;i++) {
			for(int j=0;j<w;j++) {
				ret[j][i]=(int)array[it++];
			}
		}
		return ret;
	}
	
	private static AffineTransform createTransform(double[] d) {
		return new AffineTransform(new double[] {
			d[1],0,0,d[5],d[0],d[3]});
	}
	
	private static Rectangle2D getAppendBounds(float[][] f1,AffineTransform af1,float[][] f2,AffineTransform af2) {
		Rectangle2D rect=null;
		for(int i=0;i<f1.length;i++) {
			for(int j=0;j<f1[0].length;j++) {
				Point2D p=af1.transform(new Point2D.Double(i, j), new Point2D.Double());
				if(rect==null) {
					rect=new Rectangle2D.Double(p.getX(),p.getY(),0,0);
				}else {
					rect.add(p);
				}
			}
		}
		for(int i=0;i<f2.length;i++) {
			for(int j=0;j<f2[0].length;j++) {
				Point2D p=af2.transform(new Point2D.Double(i, j), new Point2D.Double());
				rect.add(p);
			}
		}
		return rect;
	}

	private static float[][] appendDataPoint(Rectangle2D rect,float[][] f1,AffineTransform af1,float[][] f2,AffineTransform af2) {
		double[] param=new double[] {af1.getScaleX(),0,0,af1.getScaleY(),rect.getX(),rect.getY()+rect.getHeight()};
		AffineTransform af=new AffineTransform(param);
		int ww=(int)Math.abs(rect.getWidth()/af.getScaleX());
		int hh=(int)Math.abs(rect.getHeight()/af.getScaleY());
		float[][] tmp=new float[ww][hh];
		AffineTransform iaf1=null,iaf2=null;
		try {
			iaf1=af1.createInverse();
			iaf2=af2.createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		for(int i=0;i<ww;i++) {
			for(int j=0;j<hh;j++) {
				Point2D p=af.transform(new Point2D.Double(i, j), new Point2D.Double());
				Point2D pt=iaf1.transform(p, new Point2D.Double());
				int xx=(int)pt.getX();
				int yy=(int)pt.getY();
				if(xx>=0&&xx<f1.length&&yy>=0&&yy<f1[0].length&&f1[xx][yy]>0) {
					tmp[i][j]=f1[xx][yy];
				}else {
					pt=iaf2.transform(p, new Point2D.Double());
					xx=(int)pt.getX();
					yy=(int)pt.getY();
					if(xx>=0&&xx<f2.length&&yy>=0&&yy<f2[0].length&&f2[xx][yy]>0) {
						tmp[i][j]=f2[xx][yy];
					}
				}
			}
		}
		return tmp;
	}
	
	public BufferedImage createIndexedImage(int channel){
		float[][] ff=band.get(channel);
		BufferedImage img=new BufferedImage(ff.length,ff[0].length,BufferedImage.TYPE_INT_RGB);
		ColorTable ct=table.get(channel);
		for(int i=0;i<ff.length;i++) {
			for(int j=0;j<ff[0].length;j++) {
				int pt=(int)ff[i][j];
				img.setRGB(i, j, ct.GetColorEntry(pt).getRGB());
			}
		}
		return img;
	}
	
	public BufferedImage createValueImage(int channel,Gradient grad,Range range){
		float[][] ff=band.get(channel);
		BufferedImage img=new BufferedImage(ff.length,ff[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<ff.length;i++) {
			for(int j=0;j<ff[0].length;j++) {
				img.setRGB(i, j,grad.getColorByInt(range.getValue(ff[i][j])));
			}
		}
		return img;
	}
}
