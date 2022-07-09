package net.termat.geo.vectortile;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.mapbox.geojson.Feature;
import com.wdtinc.mapbox_vector_tile.adapt.jts.MvtReader;
import com.wdtinc.mapbox_vector_tile.adapt.jts.TagKeyValueMapConverter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsLayer;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsMvt;

import net.termat.geo.satellite.MeshUtil;
import net.termat.geo.satellite.VecUtil;
import net.termat.geo.satellite.VectorReader;

public class MVTUtil {
	
	public static Map<String,List<Geometry>> mvtToJson(File f,int zoom,int x,int y,Gson gson,GeometryFactory fac) throws IOException {
		Rectangle2D r=MeshUtil.getTileBounds(zoom,x,y);
		ByteArrayInputStream bis=new ByteArrayInputStream(getProto(f));
		GeometryFactory geomFactory = new GeometryFactory();
		JtsMvt mvt = MvtReader.loadMvt(bis,geomFactory,new TagKeyValueMapConverter(),MvtReader.RING_CLASSIFIER_V1);
		Map<String,List<Geometry>> ret=new HashMap<>();
		Map<String,JtsLayer> ll=mvt.getLayersByName();
		for(String key : ll.keySet()) {
			System.out.println(key);
			JtsLayer lay=ll.get(key);
			int extent=lay.getExtent();
			double xx=r.getX();
			double yy=r.getY()+r.getHeight();
			double ww=r.getWidth()/extent;
			double hh=r.getHeight()/extent;
			List<Geometry> fs=new ArrayList<>();
			for(Geometry g : lay.getGeometries()) {
				JsonElement p=gson.toJsonTree(g.getUserData());
				g=trans(g,xx,yy,ww,hh,fac);
				g.setUserData(p);
				fs.add(g);
			}
			ret.put(key, fs);
		}
		return ret;
	}
	
	public static Map<String,List<Geometry>> mvtToJson(byte[] bytes,int zoom,int x,int y,Gson gson,GeometryFactory fac) throws IOException {
		Rectangle2D r=MeshUtil.getTileBounds(zoom,x,y);
		ByteArrayInputStream bis=new ByteArrayInputStream(bytes);
		GeometryFactory geomFactory = new GeometryFactory();
		JtsMvt mvt = MvtReader.loadMvt(bis,geomFactory,new TagKeyValueMapConverter(),MvtReader.RING_CLASSIFIER_V1);
		Map<String,List<Geometry>> ret=new HashMap<>();
		Map<String,JtsLayer> ll=mvt.getLayersByName();
		for(String key : ll.keySet()) {
			System.out.println(key);
			JtsLayer lay=ll.get(key);
			int extent=lay.getExtent();
			double xx=r.getX();
			double yy=r.getY()+r.getHeight();
			double ww=r.getWidth()/extent;
			double hh=r.getHeight()/extent;
			List<Geometry> fs=new ArrayList<>();
			for(Geometry g : lay.getGeometries()) {
				JsonElement p=gson.toJsonTree(g.getUserData());
				g=trans(g,xx,yy,ww,hh,fac);
				g.setUserData(p);
				fs.add(g);
			}
			ret.put(key, fs);
		}
		return ret;
	}

	public static Map<String,List<Feature>> mvtToFeatures(File f,int zoom,int x,int y,Gson gson,GeometryFactory fac) throws IOException {
		Rectangle2D r=MeshUtil.getTileBounds(zoom,x,y);
		ByteArrayInputStream bis=new ByteArrayInputStream(getProto(f));
		GeometryFactory geomFactory = new GeometryFactory();
		JtsMvt mvt = MvtReader.loadMvt(bis,geomFactory,new TagKeyValueMapConverter(),MvtReader.RING_CLASSIFIER_V1);
		Map<String,List<Feature>> ret=new HashMap<>();
		Map<String,JtsLayer> ll=mvt.getLayersByName();
		for(String key : ll.keySet()) {
			System.out.println(key);
			JtsLayer lay=ll.get(key);
			int extent=lay.getExtent();
			double xx=r.getX();
			double yy=r.getY()+r.getHeight();
			double ww=r.getWidth()/extent;
			double hh=r.getHeight()/extent;
			List<Feature> fs=new ArrayList<>();
			for(Geometry g : lay.getGeometries()) {
				JsonElement p=gson.toJsonTree(g.getUserData());
				g=trans(g,xx,yy,ww,hh,fac);
				fs.add(Feature.fromGeometry(VecUtil.toMapbox(g), p.getAsJsonObject()));
			}
			ret.put(key, fs);
		}
		return ret;
	}
	
	public static Map<String,List<Feature>> mvtToFeatures(byte[] bytes,int zoom,int x,int y,Gson gson,GeometryFactory fac) throws IOException {
		Rectangle2D r=MeshUtil.getTileBounds(zoom,x,y);
		ByteArrayInputStream bis=new ByteArrayInputStream(bytes);
		GeometryFactory geomFactory = new GeometryFactory();
		JtsMvt mvt = MvtReader.loadMvt(bis,geomFactory,new TagKeyValueMapConverter(),MvtReader.RING_CLASSIFIER_V1);
		Map<String,List<Feature>> ret=new HashMap<>();
		Map<String,JtsLayer> ll=mvt.getLayersByName();
		for(String key : ll.keySet()) {
			System.out.println(key);
			JtsLayer lay=ll.get(key);
			int extent=lay.getExtent();
			double xx=r.getX();
			double yy=r.getY()+r.getHeight();
			double ww=r.getWidth()/extent;
			double hh=r.getHeight()/extent;
			List<Feature> fs=new ArrayList<>();
			for(Geometry g : lay.getGeometries()) {
				JsonElement p=gson.toJsonTree(g.getUserData());
				g=trans(g,xx,yy,ww,hh,fac);
				fs.add(Feature.fromGeometry(VecUtil.toMapbox(g), p.getAsJsonObject()));
			}
			ret.put(key, fs);
		}
		return ret;
	}

	public static byte[] jsonToMvt(File f,String layerName,int zoom,int x,int y,GeometryFactory fac) throws IOException, ParseException {
		VectorReader vr=VectorReader.createReader(f);
		MVTBuilder mvt=new MVTBuilder(vr);
		return mvt.createMVT(zoom, new java.awt.Point(x,y), fac, layerName);
	}
	
	public static void write(File out,byte[] bytes) throws IOException {
        Path path = out.toPath();
        Files.write(path, bytes);
	}
	
	/*
	public static byte[] jsonToMvt(Map<String,List<Geometry>> gm,int zoom,int x,int y,GeometryFactory fac) throws IOException {
		MvtLayerParams layerParams = new MvtLayerParams();
		Envelope env=getTileBounds(x,y,zoom);
//		Envelope env =getTileBoundsExt(x,y,zoom);
		IGeometryFilter acceptAllGeomFilter=new IGeometryFilter() {
			@Override
			public boolean accept(Geometry geometry) {
				return true;
			}
		};
		final VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();
		final MvtLayerProps layerProps = new MvtLayerProps();
		IUserDataConverter userDataConverter=new IUserDataConverter() {
			@Override
			public void addTags(Object userData, MvtLayerProps layerProps, Builder featureBuilder) {
				JsonObject o=(JsonObject)userData;
				for(String s : o.keySet()) {
					String val=o.get(s).getAsString();
					try {
						 featureBuilder.addTags(layerProps.addValue(Double.parseDouble(val)));
					}catch(Exception e) {
						 featureBuilder.addTags(layerProps.addValue(val));
					}
				}
			}
		};
		for(String key : gm.keySet()) {
			List<Geometry> g=gm.get(key);
			TileGeomResult tileGeom = JtsAdapter.createTileGeom(g, env, fac, layerParams, acceptAllGeomFilter);
			final List<VectorTile.Tile.Feature> features = JtsAdapterReverse.toFeatures(tileGeom.mvtGeoms, layerProps, userDataConverter);
			final VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(key, layerParams);
			layerBuilder.addAllFeatures(features);
			MvtLayerBuild.writeProps(layerBuilder, layerProps);
			final VectorTile.Tile.Layer layer = layerBuilder.build();
			tileBuilder.addLayers(layer);
		}
		Tile mvt = tileBuilder.build();
		return mvt.toByteArray();
	}
	*/
	
	public static JtsMvt toJtsMvt(byte[] b,GeometryFactory fac) throws IOException {
		ByteArrayInputStream bis=new ByteArrayInputStream(b);
		return MvtReader.loadMvt(bis,fac,new TagKeyValueMapConverter(),MvtReader.RING_CLASSIFIER_V1);
	}
	
	public static List<Feature> getMapboxFeatures(List<Geometry> geo){
		List<Feature> ret=new ArrayList<>();
		for(Geometry g : geo) {
			ret.add(Feature.fromGeometry(VecUtil.toMapbox(g),(JsonObject)g.getUserData()));
		}
		return ret;
	}
	
	public static byte[] getProto(File f) throws IOException {
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
