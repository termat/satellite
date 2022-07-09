package net.termat.geo.vectortile;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

public class OrientationTransformer {
	
	public static Geometry transformCCW(final Geometry geometry) {
		if (geometry instanceof MultiPolygon) {
			MultiPolygon multiPolygon = (MultiPolygon) geometry;
			List<Polygon> polygons = new ArrayList<>();
			for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
				final Geometry polygon = multiPolygon.getGeometryN(i);
				polygons.add((Polygon) transformCCW(polygon));
			}
			return new GeometryFactory().createMultiPolygon(polygons.toArray(new Polygon[0]));
		} else if (geometry instanceof Polygon) {
			return transformCCW((Polygon) geometry);
		} else {
			return geometry;
		}
	}
	
	public static Geometry transformCW(final Geometry geometry) {
		if (geometry instanceof MultiPolygon) {
			MultiPolygon multiPolygon = (MultiPolygon) geometry;
			List<Polygon> polygons = new ArrayList<>();
			for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
				final Geometry polygon = multiPolygon.getGeometryN(i);
				polygons.add((Polygon) transformCW(polygon));
			}
			return new GeometryFactory().createMultiPolygon(polygons.toArray(new Polygon[0]));
		} else if (geometry instanceof Polygon) {
			return transformCW((Polygon) geometry);
		} else {
			return geometry;
		}
	}

	private static Coordinate[] reverse(Coordinate[] src) {
		Coordinate[] tmp=new Coordinate[src.length];
		int n=src.length;
		for(int k=0;k<n;k++) {
			tmp[k]=src[n-k-1];
		}
		return tmp;
	}
	
	private static Polygon transformCCW(Polygon polygon) {
		GeometryFactory gf=new GeometryFactory(polygon.getPrecisionModel(), polygon.getSRID());
		LinearRing ol=null;
		LineString outer=polygon.getExteriorRing();
		if(!Orientation.isCCW(outer.getCoordinates())) {
			Coordinate[] src=outer.getCoordinates();
			Coordinate[] tmp=reverse(src);
			ol=gf.createLinearRing(tmp);
		}
		List<LinearRing> inner=new ArrayList<>();
		for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
			LineString ll=polygon.getInteriorRingN(i);
			if(Orientation.isCCW(ll.getCoordinates())) {
				Coordinate[] src=ll.getCoordinates();
				Coordinate[] tmp=reverse(src);
				LinearRing il=gf.createLinearRing(tmp);
				inner.add(il);
			}else {
				LinearRing il=gf.createLinearRing(ll.getCoordinates());
				inner.add(il);
			}
        }
		if(inner.size()>0) {
			LinearRing[] ils=inner.toArray(new LinearRing[inner.size()]);
			return new GeometryFactory(polygon.getPrecisionModel(), polygon.getSRID()).createPolygon(ol, ils);
		}else {
			return new GeometryFactory(polygon.getPrecisionModel(), polygon.getSRID()).createPolygon(ol);
		}
    }
	
	private static Polygon transformCW(Polygon polygon) {
		GeometryFactory gf=new GeometryFactory(polygon.getPrecisionModel(), polygon.getSRID());
		LinearRing ol=null;
		LineString outer=polygon.getExteriorRing();
		if(Orientation.isCCW(outer.getCoordinates())) {
			Coordinate[] src=outer.getCoordinates();
			Coordinate[] tmp=reverse(src);
			ol=gf.createLinearRing(tmp);
		}
		List<LinearRing> inner=new ArrayList<>();
		for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
			LineString ll=polygon.getInteriorRingN(i);
			if(!Orientation.isCCW(ll.getCoordinates())) {
				Coordinate[] src=ll.getCoordinates();
				Coordinate[] tmp=reverse(src);
				LinearRing il=gf.createLinearRing(tmp);
				inner.add(il);
			}else {
				LinearRing il=gf.createLinearRing(ll.getCoordinates());
				inner.add(il);
			}
        }
		if(inner.size()>0) {
			LinearRing[] ils=inner.toArray(new LinearRing[inner.size()]);
			return new GeometryFactory(polygon.getPrecisionModel(), polygon.getSRID()).createPolygon(ol, ils);
		}else {
			return new GeometryFactory(polygon.getPrecisionModel(), polygon.getSRID()).createPolygon(ol);
		}
    }
}
