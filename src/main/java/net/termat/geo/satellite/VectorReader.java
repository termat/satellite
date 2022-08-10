package net.termat.geo.satellite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.gdal.gdal.gdal;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Driver;
import org.gdal.ogr.Feature;
import org.gdal.ogr.FeatureDefn;
import org.gdal.ogr.FieldDefn;
import org.gdal.ogr.GeomTransformer;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osrConstants;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mapbox.geojson.FeatureCollection;

import net.termat.geo.util.PCUtil;

public class VectorReader {
	private List<Geometry> geom;
	private List<JsonObject> prop;
	private int epsg;
	
	private VectorReader() {
		ogr.RegisterAll();
		geom=new ArrayList<>();
		prop=new ArrayList<>();
	}
	
	public static VectorReader createReader(File f) throws ParseException {
		VectorReader ret=new VectorReader();
		ret.readFile(f);
		return ret;
	}
	
	public static VectorReader createReader(int epsg,List<Geometry> g,List<JsonObject> o) throws ParseException {
		VectorReader ret=new VectorReader();
		ret.geom.addAll(g);
		ret.prop.addAll(o);
		ret.epsg=epsg;
		return ret;
	}
	
	public static VectorReader createReader(int epsg,File json) throws ParseException, IOException {
		VectorReader ret=new VectorReader();
		FeatureCollection fc=VecUtil.loadFeatureCollection(json);
		List<com.mapbox.geojson.Feature> ff=fc.features();
		GeoJsonReader gr=new GeoJsonReader();
		for(com.mapbox.geojson.Feature g :ff) {
			ret.geom.add(gr.read(g.geometry().toJson()));
			ret.prop.add(g.properties());
		}
		ret.epsg=epsg;
		return ret;
	}
	
	public static VectorReader createReader(int epsg,String json) throws ParseException {
		VectorReader ret=new VectorReader();
		FeatureCollection fc=FeatureCollection.fromJson(json);
		List<com.mapbox.geojson.Feature> ff=fc.features();
		GeoJsonReader gr=new GeoJsonReader();
		for(com.mapbox.geojson.Feature g :ff) {
			ret.geom.add(gr.read(g.geometry().toJson()));
			ret.prop.add(g.properties());
		}
		ret.epsg=epsg;
		return ret;
	}
	
	public void addFeature(VectorReader vr) throws ParseException {
		if(epsg!=vr.epsg)vr=vr.createProjectionData(epsg);
		this.geom.addAll(vr.geom);
		this.prop.addAll(vr.prop);
	}
	
	public VectorReader createProjectionData(int target_epsg) throws ParseException {
		if(epsg==target_epsg) {
			VectorReader ret=new VectorReader();
			ret.epsg=epsg;
			for(JsonObject o : prop) {
				ret.prop.add(o.deepCopy());
			}
			for(Geometry g : geom) {
				ret.geom.add(g.copy());
			}
			return ret;
		}else {
			SpatialReference target=BandUtil.createSpatialReference(target_epsg);
			target.SetAxisMappingStrategy(osrConstants.OAMS_TRADITIONAL_GIS_ORDER);
			SpatialReference src=BandUtil.createSpatialReference(epsg);
			src.SetAxisMappingStrategy(osrConstants.OAMS_TRADITIONAL_GIS_ORDER);
			CoordinateTransformation ct=BandUtil.getCoordinateTransformation(src,target);
			GeomTransformer gtrans=new GeomTransformer(ct);
			List<org.gdal.ogr.Geometry> li=new ArrayList<>();
			for(Geometry g : geom) {
				org.gdal.ogr.Geometry gp=VecUtil.toOGR(g);
				gp=gp.Transform(gtrans);
				li.add(gp);
			}
			GeoJsonReader gr=new GeoJsonReader();
			List<Geometry> ret=new ArrayList<>();
			for(org.gdal.ogr.Geometry g : li) {
				ret.add(gr.read(g.ExportToJson()));
			}
			return createReader(target_epsg,ret,prop);
		}
	}
	
	private void readFile(File f) throws ParseException {
		GeoJsonReader gr=new GeoJsonReader();
		DataSource data=ogr.Open(f.getAbsolutePath());
		Layer lay=data.GetLayer(0);
		SpatialReference srs=lay.GetSpatialRef();
		srs.SetAxisMappingStrategy(osrConstants.OAMS_TRADITIONAL_GIS_ORDER);
		epsg=Integer.parseInt(srs.GetAttrValue("AUTHORITY",1));
		long n=lay.GetFeatureCount();
		for(long i=0;i<n;i++) {
			Feature ff=lay.GetFeature(i);
			if(ff.GetGeometryRef()==null)continue;
			geom.add(gr.read(ff.GetGeometryRef().ExportToJson()));
			JsonObject obj=new JsonObject();
			int n2=ff.GetFieldCount();
			for(int j=0;j<n2;j++) {
				int type=ff.GetFieldType(j);
				switch(type) {
				case ogr.OFSTFloat32:
				case ogr.OFTReal:
					obj.addProperty(ff.GetFieldDefnRef(j).GetName(), ff.GetFieldAsDouble(j));
					break;
				case ogr.OFTInteger:
				case ogr.OFTInteger64:
					obj.addProperty(ff.GetFieldDefnRef(j).GetName(), ff.GetFieldAsInteger(j));
					break;
				case ogr.OFTString:
					obj.addProperty(ff.GetFieldDefnRef(j).GetName(), ff.GetFieldAsString(j));
					break;
				default:
				}
			}
			prop.add(obj);
		}
	}
	
	public int size() {
		return geom.size();
	}
	
	public int getEPSG() {
		return epsg;
	}
	
	public void addVector(VectorReader vr) {
		this.geom.addAll(vr.geom);
		this.prop.addAll(vr.prop);
	}
	
	public VectorReader getSubVector(Rectangle2D r) {
		VectorReader vr=new VectorReader();
		vr.epsg=this.epsg;
		int n=geom.size();
		for(int i=0;i<n;i++) {
			Geometry gp=geom.get(i);
			Shape sp=VecUtil.toShape(gp);
			if(sp.intersects(r)) {
				vr.geom.add(gp);
				vr.prop.add(prop.get(i));
			}
		}
		return vr;
	}
	
	public List<VectorReader> getSubVectors(List<Rectangle2D> rs){
		List<VectorReader> ret=new ArrayList<>();
		List<Geometry> gg=new ArrayList<>();
		List<JsonObject> pp=new ArrayList<>();
		gg.addAll(geom);
		pp.addAll(prop);
		for(Rectangle2D r : rs) {
			VectorReader vr=new VectorReader();
			vr.epsg=this.epsg;
			int n=gg.size();
			for(int i=0;i<n;i++) {
				Geometry gp=gg.get(i);
				Shape sp=VecUtil.toShape(gp);
				if(sp.intersects(r)) {
					vr.geom.add(gp);
					vr.prop.add(pp.get(i));
				}
			}
			gg.removeAll(vr.geom);
			pp.removeAll(vr.prop);
			ret.add(vr);
		}
		return ret;
	}
	
	public Rectangle2D getBounds() {
		Rectangle2D ret=null;
		for(Geometry g : geom) {
			Coordinate[] cc=g.getCoordinates();
			for(Coordinate c : cc) {
				if(ret==null) {
					ret=new Rectangle2D.Double(c.x,c.y,0,0);
				}else {
					ret.add(c.x,c.y);
				}
			}
		}
		return ret;
	}
	
	public Geometry getGeometry(int i) {
		return geom.get(i);
	}
	
	public List<Geometry> getGeometrys() {
		return geom;
	}
	
	public JsonObject getProperty(int i) {
		return prop.get(i);
	}
	
	public List<JsonObject> getPropertys() {
		return prop;
	}
	
	public List<Area> getShapeList(){
		List<Area> ret=new ArrayList<>();
		for(Geometry g : geom) {
			ret.add(VecUtil.toShape(g));
		}
		return ret;
	}
	
	public List<com.mapbox.geojson.Feature> getFeaturesMapbox(){
		List<com.mapbox.geojson.Feature> ret=new ArrayList<>();
		int n=geom.size();
		for(int i=0;i<n;i++) {
			com.mapbox.geojson.Geometry g=VecUtil.toMapbox(geom.get(i));
			JsonObject p=prop.get(i);
			ret.add(com.mapbox.geojson.Feature.fromGeometry(g, p));
		}
		return ret;
	}

	public void writeShp(File f) {
		gdal.SetConfigOption("SHAPE_ENCODING", "UTF-8");
		Driver driver = ogr.GetDriverByName("ESRI Shapefile");
		DataSource  data= driver.CreateDataSource(f.getAbsolutePath());
		SpatialReference srs=BandUtil.createSpatialReference(epsg);
		srs.SetAxisMappingStrategy(osrConstants.OAMS_TRADITIONAL_GIS_ORDER);
		int type=0;
		Geometry g=geom.get(0);
		if (g instanceof Point) {
			type=ogr.wkbPoint;
		} else if (g instanceof LineString) {
			type=ogr.wkbLineString;
		} else if (g instanceof Polygon) {
			type=ogr.wkbPolygon;
		} else if (g instanceof MultiPoint) {
			type=ogr.wkbMultiPoint;
		} else if (g instanceof MultiLineString) {
			type=ogr.wkbMultiLineString;
		} else if (geom instanceof MultiPolygon) {
		  type=ogr.wkbMultiPolygon;
		} 
		Layer layer =data.CreateLayer("data",srs,type);
		for(Entry<String,JsonElement> e : prop.get(0).entrySet()) {
			JsonElement je=e.getValue();
			JsonPrimitive jp=je.getAsJsonPrimitive();
			if(jp.isString()) {
				 FieldDefn oFieldName = new FieldDefn(e.getKey(), ogr.OFTString);
				 oFieldName.SetWidth(jp.getAsString().length()*2);
				 layer.CreateField(oFieldName);
			}else if(jp.isNumber()) {
				Number nn=jp.getAsNumber();
				if(nn instanceof Integer) {
					 FieldDefn oFieldName = new FieldDefn(e.getKey(), ogr.OFTInteger);
					 oFieldName.SetWidth(8);
					 layer.CreateField(oFieldName);
				}else {
					 FieldDefn oFieldName = new FieldDefn(e.getKey(), ogr.OFTReal);
					 oFieldName.SetWidth(8);
					 layer.CreateField(oFieldName);
				}
			}
		}
		int n=geom.size();
		FeatureDefn oDefn = layer.GetLayerDefn();
		for(int i=0;i<n;i++) {
			JsonObject o=prop.get(i);
			Feature ff = new Feature(oDefn);
			for(Entry<String,JsonElement> e : o.entrySet()) {
				JsonElement je=e.getValue();
				JsonPrimitive jp=je.getAsJsonPrimitive();
				if(jp.isString()) {
					ff.SetField(e.getKey(), e.getValue().getAsString());
				}else if(jp.isNumber()) {
					Number nn=jp.getAsNumber();
					if(nn instanceof Integer) {
						ff.SetField(e.getKey(), e.getValue().getAsInt());
					}else {
						ff.SetField(e.getKey(), e.getValue().getAsDouble());
					}
				}
			}
			org.gdal.ogr.Geometry ge=VecUtil.toOGR(geom.get(i));
			ff.SetGeometry(ge);
			layer.CreateFeature(ff);
		}
		data.SyncToDisk();
	}
	
	public void writeShp(File f,String enc) throws UnsupportedEncodingException {
		gdal.SetConfigOption("SHAPE_ENCODING", enc);
		Driver driver = ogr.GetDriverByName("ESRI Shapefile");
		DataSource  data= driver.CreateDataSource(f.getAbsolutePath());
		SpatialReference srs=BandUtil.createSpatialReference(epsg);
		srs.SetAxisMappingStrategy(osrConstants.OAMS_TRADITIONAL_GIS_ORDER);
		int type=0;
		Geometry g=geom.get(0);
		if (g instanceof Point) {
			type=ogr.wkbPoint;
		} else if (g instanceof LineString) {
			type=ogr.wkbLineString;
		} else if (g instanceof Polygon) {
			type=ogr.wkbPolygon;
		} else if (g instanceof MultiPoint) {
			type=ogr.wkbMultiPoint;
		} else if (g instanceof MultiLineString) {
			type=ogr.wkbMultiLineString;
		} else if (geom instanceof MultiPolygon) {
		  type=ogr.wkbMultiPolygon;
		} 
		Layer layer =data.CreateLayer("data",srs,type);
		for(Entry<String,JsonElement> e : prop.get(0).entrySet()) {
			JsonElement je=e.getValue();
			JsonPrimitive jp=je.getAsJsonPrimitive();
			if(jp.isString()) {
				 FieldDefn oFieldName = new FieldDefn(e.getKey(), ogr.OFTString);
				 oFieldName.SetWidth(jp.getAsString().length()*2);
				 layer.CreateField(oFieldName);
			}else if(jp.isNumber()) {
				Number nn=jp.getAsNumber();
				if(nn instanceof Integer) {
					 FieldDefn oFieldName = new FieldDefn(e.getKey(), ogr.OFTInteger);
					 oFieldName.SetWidth(8);
					 layer.CreateField(oFieldName);
				}else {
					 FieldDefn oFieldName = new FieldDefn(e.getKey(), ogr.OFTReal);
					 oFieldName.SetWidth(16);
					 layer.CreateField(oFieldName);
				}
			}
		}
		int n=geom.size();
		FeatureDefn oDefn = layer.GetLayerDefn();
		for(int i=0;i<n;i++) {
			JsonObject o=prop.get(i);
			Feature ff = new Feature(oDefn);
			for(Entry<String,JsonElement> e : o.entrySet()) {
				JsonElement je=e.getValue();
				JsonPrimitive jp=je.getAsJsonPrimitive();
				if(jp.isString()) {
					ff.SetField(e.getKey(), e.getValue().getAsString());
				}else if(jp.isNumber()) {
					Number nn=jp.getAsNumber();
					if(nn instanceof Integer) {
						ff.SetField(e.getKey(), e.getValue().getAsInt());
					}else {
						ff.SetField(e.getKey(), e.getValue().getAsDouble());
					}
				}
			}
			org.gdal.ogr.Geometry ge=VecUtil.toOGR(geom.get(i));
			ff.SetGeometry(ge);
			layer.CreateFeature(ff);
		}
		data.SyncToDisk();
	}
	
	public void writeJson(File f) throws IOException {
		BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f),"UTF-8"));
		FeatureCollection fc=FeatureCollection.fromFeatures(getFeaturesMapbox());
		bw.write(fc.toJson());
		bw.close();
	}
	
	public void createMaskImage(BandReader br,File png) throws NoninvertibleTransformException, IOException {
		float[][] p=br.getBand(0);
		AffineTransform af=br.getTransform();
		BufferedImage img=new BufferedImage(p.length,p[0].length,BufferedImage.TYPE_INT_RGB);
		Graphics2D g=img.createGraphics();
		g.setTransform(af.createInverse());
		g.setBackground(Color.BLACK);
		g.setColor(Color.WHITE);
		g.clearRect(0, 0, img.getWidth(), img.getHeight());
		for(Area a : getShapeList()) {
			g.fill(a);
		}
		ImageIO.write(img, "png", png);
		PCUtil.writeTransform(af, new File(png.getAbsolutePath().replace(".png", ".pgw").replace(".PNG", ".pgw")));
	}
	
	public void createMaskImage(ImageReader br,File png) throws NoninvertibleTransformException, IOException {
		BufferedImage b=br.getBand();
		AffineTransform af=br.getTransform();
		BufferedImage img=new BufferedImage(b.getWidth(),b.getHeight(),BufferedImage.TYPE_INT_RGB);
		Graphics2D g=img.createGraphics();
		g.setTransform(af.createInverse());
		g.setBackground(Color.BLACK);
		g.setColor(Color.WHITE);
		g.clearRect(0, 0, img.getWidth(), img.getHeight());
		for(Area a : getShapeList()) {
			g.fill(a);
		}
		ImageIO.write(img, "png", png);
		PCUtil.writeTransform(af, new File(png.getAbsolutePath().replace(".png", ".pgw").replace(".PNG", ".pgw")));
	}
	
	public VectorReader createBuffer(double dist) throws ParseException {
		List<Geometry> li=new ArrayList<>();
		List<JsonObject> jo=new ArrayList<>();
		for(int i=0;i<geom.size();i++) {
			Geometry g=geom.get(i);
			org.gdal.ogr.Geometry g2=VecUtil.toOGR(g);
			org.gdal.ogr.Geometry g3=g2.Buffer(dist);
			if(g3.Area()>0) {
				li.add(VecUtil.toJTS(g3));
				jo.add(getProperty(i));
			}
		}
		return createReader(epsg,li,jo);
	}

}

