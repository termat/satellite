package net.termat.geo.pointcloud;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tinfour.common.IIncrementalTin;
import org.tinfour.common.Vertex;
import org.tinfour.contour.Contour;
import org.tinfour.contour.ContourBuilderForTin;
import org.tinfour.interpolation.IVertexValuator;
import org.tinfour.standard.IncrementalTin;

import com.google.gson.Gson;

import net.termat.geo.util.PCUtil;

public class ContourLine implements GeojsonData{
	private List<Vertex> points;
	private IIncrementalTin tin;
	private double[] zData;
	private Map<String,Object> root;
	protected Rectangle2D bounds;

	public ContourLine(BufferedImage dem,AffineTransform af,int step){
		points=new ArrayList<Vertex>();
		int id=0;
		double min=Double.MAX_VALUE;
		double max=-Double.MAX_VALUE;
		for(int i=0;i<dem.getWidth();i=i+step){
			for(int j=0;j<dem.getHeight();j=j+step){
				double z=PCUtil.getZ(dem.getRGB(i, j));
				if(Double.isNaN(z)||Double.isInfinite(z))continue;
				Point2D px=af.transform(new Point2D.Double(i,j), new Point2D.Double());
				Vertex v=new Vertex(px.getX(),px.getY(),z,id++);
				points.add(v);
				min=Math.min(min, z);
				max=Math.max(max, z);
			}
		}
		min=Math.floor(min/step)*step;
		max=Math.ceil(max/step)*step;
		List<Double> tmp=new ArrayList<Double>();
		for(double z=min;z<=max;z=z+step){
			tmp.add(z);
		}
		zData=new double[tmp.size()];
		for(int i=0;i<zData.length;i++){
			zData[i]=tmp.get(i);
		}
		bounds=af.createTransformedShape(new Rectangle2D.Double(0,0,dem.getWidth(),dem.getHeight())).getBounds2D();
	}

	@Override
	public void setName(String name) {
		root.put("name",name);
	}

	public void setCoordSys(int coodId){
		Gson gson=new Gson();
		String crs="{ 'type': 'name', 'properties': { 'name': 'urn:ogc:def:crs:EPSG::"+Integer.toString(coodId+6668)+"'}}";
		root.put("crs",gson.fromJson(crs, Map.class));
	}
	
	public void createContour(){
		tin=new IncrementalTin();
		tin.add(points, null);
		IVertexValuator vv=new IVertexValuator(){
			@Override
			public double value(Vertex arg0) {
				return arg0.getZ();
			}
		};
		ContourBuilderForTin builder=new ContourBuilderForTin(tin,vv,zData,false);
		List<Contour> contour=builder.getContours();
		root=new HashMap<String,Object>();
		root.put("type","FeatureCollection");
		List<Map<String,Object>> feas=new ArrayList<Map<String,Object>>();
		root.put("features",feas);
		for(Contour c :contour){
			if(c.isEmpty())continue;
			String zz=c.toString().split(",")[2];
			zz=zz.replace("z=", "");
			zz=zz.replace(" ", "");
			double z=Double.parseDouble(zz);
			double[] xy=c.getCoordinates();
			Map<String,Object> f=new HashMap<String,Object>();
			f.put("type", "Feature");
			Map<String,Object> p=new HashMap<String,Object>();
			p.put("z", z);
			f.put("properties", p);
			Map<String,Object> g=new HashMap<String,Object>();
			f.put("geometry", g);
			g.put("type","LineString");
			List<double[]> coord=new ArrayList<double[]>();
			for(int i=1;i<xy.length;i=i+2){
				if(Double.isNaN(xy[i-1])||Double.isInfinite(xy[i-1]))break;
				coord.add(new double[]{xy[i-1],xy[i]});
			}
			g.put("coordinates", coord);
			feas.add(f);
		}
	}

	public String getGeojson(){
		Gson gson=new Gson();
		return gson.toJson(root);
	}
	
	public Rectangle2D getBounds() {
		return bounds;
	}
}
