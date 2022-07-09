package net.termat.geo.grid;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;

import com.google.gson.JsonObject;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.GeoCoord;

import net.termat.geo.satellite.MeshUtil;
import net.termat.geo.satellite.VectorReader;
import net.termat.geo.vectortile.MVTBuilder;

public class H3Builder {
	private H3Core h3;
	private GeometryFactory gf;
	
	public H3Builder() throws IOException {
		h3 = H3Core.newInstance();
		gf=new GeometryFactory();
	}
	
	public void buildMVT(Rectangle2D recWGS84,int minZoom,int maxZoom,File outDir) throws IOException, ParseException {
		List<Long> hex=null;
		for(int z=maxZoom;z>=minZoom;z--) {
			System.out.println(z);
			if(hex==null) {
				hex=getFillGrid(recWGS84,z-2);
			}else {
				hex=getParent(hex,z-2);
			}
			List<Geometry> geom=new ArrayList<>();
			List<JsonObject> obj=new ArrayList<>();
			for(Long l : hex) {
				Geometry gg=getPolygon(l);
				JsonObject o=new JsonObject();
				o.addProperty("h3", l);
				geom.add(gg);
				obj.add(o);
			}
			VectorReader vr=VectorReader.createReader(4326,geom,obj);
			MVTBuilder mvt=new MVTBuilder(vr);
			mvt.createMVT(z, outDir);
		}
	}
	
	public void buildMVT2(Rectangle2D recWGS84,int minZoom,int maxZoom,File outDir,int zoomMargin,String layerName) throws IOException, ParseException {
		List<Long> hex=null;
		GeometryFactory gf=new GeometryFactory();
		for(int z=maxZoom;z>=minZoom;z--) {
			List<Point> ps=MeshUtil.getTileList(recWGS84, z);
			for(Point p : ps) {
				Rectangle2D r=MeshUtil.getTileBounds(z, p.x, p.y);
				double mx=r.getWidth()/20;
				double my=r.getHeight()/20;
				r=new Rectangle2D.Double(r.getX()-mx,r.getY()-my,r.getWidth()+2*mx,r.getHeight()+2*my);
				hex=getFillGrid(r,z-zoomMargin);
				List<Geometry> geom=new ArrayList<>();
				List<JsonObject> obj=new ArrayList<>();
				for(Long l : hex) {
					Geometry gg=getPolygon(l);
					JsonObject o=new JsonObject();
					o.addProperty("h3", l);
					geom.add(gg);
					obj.add(o);
				}
				VectorReader vr=VectorReader.createReader(4326,geom,obj);
				MVTBuilder mvt=new MVTBuilder(vr);
				mvt.createMVT(z,p, outDir,gf,layerName);
			}
		}
	}
	
	public void writeJson(String src,File f) throws IOException {
		BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f),"UTF-8"));
		bw.write(src);
		bw.close();
	}
	
	private List<GeoCoord> getGeoCoords(Rectangle2D r){
		List<GeoCoord> ret=new ArrayList<>();
		ret.add(new GeoCoord(r.getY(),r.getX()));
		ret.add(new GeoCoord(r.getY()+r.getHeight(),r.getX()));
		ret.add(new GeoCoord(r.getY()+r.getHeight(),r.getX()+r.getWidth()));
		ret.add(new GeoCoord(r.getY(),r.getX()+r.getWidth()));
		ret.add(new GeoCoord(r.getY(),r.getX()));
		return ret;
	}
	
	public VectorReader createH3VectorReader(Rectangle2D r,int zoom,boolean neigh) throws ParseException {
		List<Long> ll=getFillGrid(r,zoom);
		List<Geometry> geom=new ArrayList<>();
		List<JsonObject> prop=new ArrayList<>();
		for(Long l : ll) {
			geom.add(getPolygon(l));
			JsonObject o=new JsonObject();
			o.addProperty("z", zoom);
			o.addProperty("index", l);
			if(neigh) {
				List<Long> ns=getNeighbor(l);
				int ii=0;
				for(Long n : ns) {
					String name="n"+Integer.toString(ii++);
					o.addProperty(name, n);
				}
			}
			prop.add(o);
		}
		return VectorReader.createReader(4326, geom, prop);
	}
	
	public VectorReader createH3VectorReader(List<Long> hex,int zoom,boolean neigh) throws ParseException {
		List<Geometry> geom=new ArrayList<>();
		List<JsonObject> prop=new ArrayList<>();
		for(Long l : hex) {
			geom.add(getPolygon(l));
			JsonObject o=new JsonObject();
			o.addProperty("z", zoom);
			o.addProperty("index", l);
			if(neigh) {
				List<Long> ns=getNeighbor(l);
				int ii=0;
				for(Long n : ns) {
					String name="n"+Integer.toString(ii++);
					o.addProperty(name, n);
				}
			}
			prop.add(o);
		}
		return VectorReader.createReader(4326, geom, prop);
	}
	
	private List<GeoCoord> getGeoCoords(LineString l){
		Coordinate[] c=l.getCoordinates();
		List<GeoCoord> ret=new ArrayList<>();
		for(int i=0;i<c.length;i++) {
			ret.add(new GeoCoord(c[i].getY(),c[i].getX()));
		}
		return ret;
	}
	
	public List<Long> getChildren(List<Long> hex,int childZoom){
		List<Long> ret=new ArrayList<>();
		for(Long ll : hex) {
//			List<Long> tmp=h3.cellToChildren(ll, childZoom);
			List<Long> tmp=h3.h3ToChildren(ll, childZoom);
			ret.addAll(tmp);
		}
		return ret;
	}
	
	public List<Long> getParent(List<Long> hex,int parentZoom){
		Set<Long> ss=new HashSet<>();
		for(Long ll : hex) {
//			Long l=h3.cellToParent(ll, parentZoom);
			Long l=h3.h3ToParent(ll, parentZoom);
			ss.add(l);
		}
		List<Long> ret=new ArrayList<>();
		ret.addAll(ss);
		return ret;
	}
	
	public List<Long> getFillGrid(Rectangle2D r,int zoom){
		return h3.polyfill(getGeoCoords(r),new ArrayList<List<GeoCoord>>() , zoom);
//		return h3.polygonToCells(getGeoCoords(r),new ArrayList<List<GeoCoord>>() , zoom);
	}
	
	public List<Long> getFillGrid(Polygon p,int zoom){
		List<GeoCoord> b=getGeoCoords(p.getExteriorRing());
		List<List<GeoCoord>> h=new ArrayList<List<GeoCoord>>();
		for(int i=0;i<p.getNumInteriorRing();i++) {
			List<GeoCoord> ii=getGeoCoords(p.getInteriorRingN(i));
			h.add(ii);
		}
		return h3.polyfill(b,h, zoom);
//		return h3.polygonToCells(b,h, zoom);
	}
	
	public List<Long> getFillGrid(MultiPolygon p,int zoom){
		List<Long> ret=new ArrayList<>();
		for(int i=0;i<p.getNumGeometries();i++) {
			Geometry g=p.getGeometryN(i);
			if(g instanceof Polygon) {
				ret.addAll(getFillGrid((Polygon)g,zoom));
			}
		}
		return ret;
	}
	
	private Polygon getPolygon(long id) {
//		List<GeoCoord> li=h3.cellToBoundary(id);
		List<GeoCoord> li=h3.h3ToGeoBoundary(id);
		int n=li.size();
		Coordinate[] c=new Coordinate[n+1];
		for(int i=0;i<c.length;i++) {
			GeoCoord ll=li.get(i%n);
			c[i]=new Coordinate(ll.lng,ll.lat);
		}
		return gf.createPolygon(c);
	}
	
	private List<Long> getNeighbor(Long hex) {
		return h3.kRing(hex, 1);
//		return h3.gridDisk(hex, 6);
	}
	
}
