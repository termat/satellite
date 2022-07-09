package net.termat.geo.pointcloud;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import com.google.gson.Gson;

import net.termat.geo.util.PCUtil;

public abstract class AbstractTerrainVector implements GeojsonData{
	protected double[][] dem;
	protected AffineTransform af;
	protected Cell[][] cell;
	protected Map<Object,List<Area>> map;
	protected Map<String,Object> geojson;
	protected List<Map<String,Object>> features;
	protected Gson gson=new Gson();
	protected static GeoJsonWriter writer=new GeoJsonWriter();
	protected double dx,dy;
	protected Rectangle2D bounds;

	public AbstractTerrainVector(BufferedImage png,AffineTransform af){
		dem=new double[png.getWidth()][png.getHeight()];
		this.af=af;
		for(int i=0;i<dem.length;i++){
			for(int j=0;j<dem[i].length;j++){
				dem[i][j]=PCUtil.getZ(png.getRGB(i, j));
			}
		}
		geojson=new HashMap<>();
		geojson.put("type","FeatureCollection");
		features=new ArrayList<>();
		geojson.put("features", features);
		map=new HashMap<>();
		bounds=af.createTransformedShape(new Rectangle2D.Double(0,0,png.getWidth(),png.getHeight())).getBounds2D();
	}

	abstract public void create(int size);
	abstract protected void createGeojson();

	protected void createCell(int size){
		int ww=dem.length;
		int hh=dem[0].length;
		cell=new Cell[ww/size][hh/size];
		int x=0;
		int y=0;
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				cell[i][j]=new Cell();
				cell[i][j].x=new int[size];
				cell[i][j].y=new int[size];
				for(int m=0;m<size;m++){
					cell[i][j].y[m]=y;
					y=(y+1)%hh;
				}
				for(int m=0;m<size;m++){
					cell[i][j].x[m]=x+m;
				}
				if(y==0)x=x+size;
			}
		}
	}

	public double[] minmax(){
		double min=Double.MAX_VALUE;
		double max=-Double.MAX_VALUE;
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				min=Math.min(min, cell[i][j].val);
				max=Math.max(max, cell[i][j].val);
			}
		}
		return new double[]{min,max};
	}

	public void setName(String name){
		geojson.put("name", name);
	}

	public void setCoordSys(int coodId){
		String crs="{ 'type': 'name', 'properties': { 'name': 'urn:ogc:def:crs:EPSG::"+Integer.toString(coodId+6668)+"'}}";
		geojson.put("crs",gson.fromJson(crs, Map.class));
	}

	public String getGeojson() {
		createGeojson();
		return gson.toJson(geojson);
	}
	
	public Rectangle2D getBounds() {
		return bounds;
	}
	
	public void outGeojson(File f) throws IOException{
		createGeojson();
		BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f),"UTF-8"));
		bw.write(gson.toJson(geojson));
		bw.flush();
		bw.close();
	}

	protected double[][] getPixelDataCell(int x,int y,Cell[][] cell){
		double[][] ret=new double[3][3];
		for(int i=0;i<3;i++){
			for(int j=0;j<3;j++){
				ret[i][j]=cell[x+i-1][y+j-1].getH();
			}
		}
		return ret;
	}


	@SuppressWarnings("unchecked")
	protected  void addFeature(Shape sp,Map<String,Object> p){
		Map<String,Object> f=new HashMap<>();
		f.put("type", "Feature");
		f.put("properties", p);
		Geometry g=shpToPoly(sp);
		String json=writer.write(g);
		Map<String,Object> mm=gson.fromJson(json, Map.class);
		mm.remove("crs");
		f.put("geometry", mm);
		features.add(f);
	}

	protected  static Polygon shpToPoly(Shape gp){
		AffineTransform af=AffineTransform.getScaleInstance(1.0, 1.0);
		PathIterator pi=gp.getPathIterator(af);
		List<Coordinate> coord=new ArrayList<Coordinate>();
		GeometryFactory gf=new GeometryFactory();
		double[] p=new double[6];
		while(!pi.isDone()){
			switch(pi.currentSegment(p)){
				case PathIterator.SEG_MOVETO:
					coord.add(new Coordinate(p[0],p[1]));
					break;
				case PathIterator.SEG_LINETO:
					coord.add(new Coordinate(p[0],p[1]));
					break;
				case PathIterator.SEG_QUADTO:
					coord.add(new Coordinate(p[2],p[3]));
					break;
				case PathIterator.SEG_CUBICTO:
					coord.add(new Coordinate(p[4],p[5]));
					break;
				case PathIterator.SEG_CLOSE:
					Coordinate c=coord.get(0);
					coord.add(new Coordinate(c.x,c.y));
					break;
			}
			pi.next();
		}
		return gf.createPolygon(coord.toArray(new Coordinate[coord.size()]));
	}

	protected  List<Shape> getShapes(Area a){
		List<Shape> ret=new ArrayList<>();
		PathIterator pi=a.getPathIterator(AffineTransform.getScaleInstance(1.0, 1.0));
		int it=0;
		double[] p=new double[6];
		List<double[]> tmp=new ArrayList<>();
		while(!pi.isDone()){
			switch(pi.currentSegment(p)){
				case PathIterator.SEG_MOVETO:
				case PathIterator.SEG_LINETO:
				case PathIterator.SEG_QUADTO:
				case PathIterator.SEG_CUBICTO:
					tmp.add(new double[]{p[0],p[1]});
					it++;
					break;
				case PathIterator.SEG_CLOSE:
					tmp.add(tmp.get(0));
					if(it>1){
						ret.add(createShape(tmp));
					}
					tmp.clear();
					break;
			}
			pi.next();
		}
		return ret;
	}

	protected  Shape createShape(List<double[]> list){
		GeneralPath gp=new GeneralPath();
		int n=list.size();
		for(int i=0;i<n;i++){
			double[] p=list.get(i);
			if(i==0){
				gp.moveTo(p[0], p[1]);
			}else if(i==n-1){
				gp.lineTo(p[0], p[1]);
				gp.closePath();
			}else{
				gp.lineTo(p[0], p[1]);
			}
		}
		Shape sp=af.createTransformedShape(gp);
		return sp;
	}

	protected  double sum(Cell a,Cell b,Cell c){
		return a.getH()+b.getH()+c.getH();
	}

	protected  void setArea(Cell cell,Map<Object,List<Area>> map,Object dir,AffineTransform af){
		Rectangle2D rec=new Rectangle2D.Double(cell.x[0],cell.y[0],cell.x.length+0.01,cell.y.length+0.01);
		Area sp=new Area(rec);
		if(map.containsKey(dir)){
			map.get(dir).add(sp);
		}else{
			ArrayList<Area> al=new ArrayList<>();
			al.add(sp);
			map.put(dir, al);
		}
	}

	public void inregrade(){
		for(Object o : map.keySet()){
			List<Area> a=map.get(o);
			List<Area> b=intrect(a);
			a.clear();
			a.addAll(b);
		}
	}

	protected List<Area> intrect(List<Area> li){
		List<Area> tmp=new ArrayList<>();
		for(Area a : li){
			boolean flg=true;
			for(Area b : tmp){
				if(b.intersects(a.getBounds2D())){
					b.add(a);
					flg=false;
					break;
				}
			}
			if(flg)tmp.add(a);
		}
		return tmp;
	}

	protected class Cell{
		int[] x;
		int[] y;
		double val;

		double getH(){
			double n=0;
			double v=0;
			for(int i=0;i<x.length;i++){
				for(int j=0;j<y.length;j++){
					if(Double.isNaN(dem[x[i]][y[j]])||dem[x[i]][y[j]]<=0)continue;
					n++;
					v +=dem[x[i]][y[j]];
				}
			}
			if(n==0){
				return 0;

			}else{
				return v/n;
			}
		};

		double getMinH(){
			double min=Double.MAX_VALUE;
			for(int i=0;i<x.length;i++){
				for(int j=0;j<y.length;j++){
					if(Double.isNaN(dem[x[i]][y[j]])||dem[x[i]][y[j]]<=0)continue;
					min=Math.min(min, dem[x[i]][y[j]]);
				}
			}
			if(min==Double.MAX_VALUE){
				return 0;
			}else{
				return min;
			}
		}

		double getMaxH(){
			double max=-Double.MAX_VALUE;
			for(int i=0;i<x.length;i++){
				for(int j=0;j<y.length;j++){
					if(Double.isNaN(dem[x[i]][y[j]])||dem[x[i]][y[j]]<=0)continue;
					max=Math.max(max, dem[x[i]][y[j]]);
				}
			}
			if(max==Double.MAX_VALUE){
				return 0;
			}else{
				return max;
			}
		}

		void setRGB(BufferedImage img,int rgb){
			for(int i=0;i<x.length;i++){
				for(int j=0;j<y.length;j++){
					img.setRGB(x[i], y[j], rgb);
				}
			}
		}

		void setValue(double[][] data){
			for(int i=0;i<x.length;i++){
				for(int j=0;j<y.length;j++){
					data[x[i]][y[j]]=val;
				}
			}
		}

		public String toString(){
			StringBuffer buf=new StringBuffer();
			for(int i=0;i<x.length;i++){
				for(int j=0;j<y.length;j++){
					buf.append("["+x[i]+","+y[j]+"],");
				}
				buf.append("\n");
			}
			return buf.toString();
		}

		double dist(Cell cell,double dx,double dy){
			int x1=x[x.length/2];
			int y1=y[x.length/2];
			int x2=cell.x[cell.x.length/2];
			int y2=cell.y[cell.y.length/2];
			double xx=(x1-x2)*dx;
			double yy=(y1-y2)*dy;
			return Math.sqrt(xx*xx+yy*yy);
		}

		Point2D getCenter(){
			double xx=0;
			double yy=0;
			for(int i=0;i<x.length;i++)xx +=x[i];
			for(int i=0;i<y.length;i++)yy +=y[i];
			xx=xx/x.length;
			yy=yy/y.length;
			return new Point2D.Double(xx,yy);
		}
	}
}
