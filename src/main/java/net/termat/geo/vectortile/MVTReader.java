package net.termat.geo.vectortile;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.protobuf.ByteString;
import com.mapbox.geojson.FeatureCollection;
import com.wdtinc.mapbox_vector_tile.adapt.jts.MvtReader;
import com.wdtinc.mapbox_vector_tile.adapt.jts.TagKeyValueMapConverter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsLayer;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsMvt;

import net.termat.geo.satellite.MeshUtil;
import net.termat.geo.satellite.VecUtil;

public class MVTReader {
	
	public static List<String> getMvtLayerName(byte[] b) throws IOException{
		ByteArrayInputStream bis=new ByteArrayInputStream(b);
		GeometryFactory geomFactory = new GeometryFactory();
		JtsMvt mvt = MvtReader.loadMvt(bis,geomFactory,new TagKeyValueMapConverter(),MvtReader.RING_CLASSIFIER_V1);
		List<String> ret=new ArrayList<>();
		ret.addAll(mvt.getLayersByName().keySet());
		return ret;
	}
	
	public static List<String> getMvtLayerName(File f) throws IOException{
		return getMvtLayerName(getProto(f));
	}
	
	public static FeatureCollection mvtToJson(File f,int zoom,int x,int y,String name) throws IOException{
		return mvtToJson(getProto(f),zoom,x,y,name);
	}
	
	public static FeatureCollection mvtToJson(byte[] b,int zoom,int x,int y,String name) throws IOException{
		Rectangle2D r=MeshUtil.getTileBounds(zoom,x,y);
		ByteArrayInputStream bis=new ByteArrayInputStream(b);
		GeometryFactory geomFactory = new GeometryFactory();
		JtsMvt mvt = MvtReader.loadMvt(bis,geomFactory,new TagKeyValueMapConverter(),MvtReader.RING_CLASSIFIER_V1);
		JtsLayer ll=mvt.getLayer(name);
		return createLayer(ll,r,geomFactory,new Gson());
	}
	
	private static FeatureCollection createLayer(JtsLayer ll,Rectangle2D r,GeometryFactory fac,Gson gson) {
		int extent=ll.getExtent();
		double xx=r.getX();
		double yy=r.getY()+r.getHeight();
		double ww=r.getWidth()/extent;
		double hh=r.getHeight()/extent;
		List<com.mapbox.geojson.Feature> fs=new ArrayList<>();
		for(Geometry g : ll.getGeometries()) {
			JsonElement p=gson.toJsonTree(g.getUserData());
			g=trans(g,xx,yy,ww,hh,fac);
			fs.add(com.mapbox.geojson.Feature.fromGeometry(VecUtil.toMapbox(g), p.getAsJsonObject()));
		}
		return FeatureCollection.fromFeatures(fs);
	}
	
	private static byte[] getProto(File f) throws IOException {
		ByteString bs=ByteString.readFrom(new FileInputStream(f));
		return bs.toByteArray();
	}
	
	private static Geometry trans(Geometry g,double x,double y,double w,double h,GeometryFactory fac) {
		if(g instanceof Polygon) {
			Polygon p=(Polygon)g;
			Coordinate[] cc=p.getExteriorRing().getCoordinates();
			cc=transPoints(cc,x,y,w,h);
			LinearRing ol=fac.createLinearRing(cc);
			LinearRing[] il=new LinearRing[p.getNumInteriorRing()];
			for(int i=0;i<p.getNumInteriorRing();i++) {
				cc=p.getInteriorRingN(i).getCoordinates();
				cc=transPoints(cc,x,y,w,h);
				il[i]=fac.createLinearRing(cc);
			}
			if(il.length==0) {
				return fac.createPolygon(ol);
			}else {
				return fac.createPolygon(ol, il);
			}
		}else if(g instanceof Point) {
			Point p=(Point)g;
			return fac.createPoint(transPoint(p.getCoordinate(),x,y,w,h));
		}else if(g instanceof LineString) {
			LineString ls=(LineString)g;
			Coordinate[] cc=ls.getCoordinates();
			cc=transPoints(cc,x,y,w,h);
			return fac.createLineString(cc);
		}
		return g;
	}
	
	private static Coordinate[] transPoints(Coordinate[] org,double x,double y,double w,double h) {
		Coordinate[] ret=new Coordinate[org.length];
		for(int i=0;i<ret.length;i++) {
			ret[i]=transPoint(org[i],x,y,w,h);
		}
		return ret;
	}
	
	private static Coordinate transPoint(Coordinate org,double x,double y,double w,double h) {
		double xx=x+w*org.getX();
		double yy=y-h*org.getY();
		return new Coordinate(xx,yy);
	}
}
