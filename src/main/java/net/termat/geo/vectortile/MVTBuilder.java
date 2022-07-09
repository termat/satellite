package net.termat.geo.vectortile;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;

import com.google.gson.JsonObject;
import com.wdtinc.mapbox_vector_tile.VectorTile;
import com.wdtinc.mapbox_vector_tile.VectorTile.Tile;
import com.wdtinc.mapbox_vector_tile.VectorTile.Tile.Feature.Builder;
import com.wdtinc.mapbox_vector_tile.adapt.jts.IGeometryFilter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.IUserDataConverter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.JtsAdapter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.TileGeomResult;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerBuild;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerParams;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerProps;

import net.termat.geo.satellite.MeshUtil;
import net.termat.geo.satellite.VectorReader;

public class MVTBuilder {
	private List<Geometry> geom;
	private Rectangle2D bounds;
	
	public MVTBuilder(VectorReader vr) throws ParseException {
		if(vr.getEPSG()!=4326)vr.createProjectionData(4326);
		this.bounds=vr.getBounds();
		this.geom=new ArrayList<>();
		for(int i=0;i<vr.size();i++) {
			Geometry gg=vr.getGeometry(i);
			gg.setUserData(vr.getProperty(i));
			this.geom.add(gg);
		}
	}
	
	public byte[] createMVT(int zoom,Point p,GeometryFactory geomFactory,String layerName) throws IOException {
		MvtLayerParams layerParams = new MvtLayerParams();
		Envelope env=getTileBounds(p.x,p.y,zoom);
		IGeometryFilter acceptAllGeomFilter=new IGeometryFilter() {
			@Override
			public boolean accept(Geometry geometry) {
				return true;
			}
		};
//		String layerName="h3grid";
		TileGeomResult tileGeom = JtsAdapter.createTileGeom(geom, env, geomFactory, layerParams, acceptAllGeomFilter);
		final VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();
		final MvtLayerProps layerProps = new MvtLayerProps();
		IUserDataConverter userDataConverter=new IUserDataConverter() {
			@Override
			public void addTags(Object userData, MvtLayerProps layerProps, Builder featureBuilder) {
				JsonObject o=(JsonObject)userData;
				for(String s : o.keySet()) {
					if(layerProps.keyIndex(layerName)==null) {
						 featureBuilder.addTags(layerProps.addKey(s));
					}
					String val=o.get(s).getAsString();
					try {
						 featureBuilder.addTags(layerProps.addValue(Double.parseDouble(val)));
					}catch(Exception e) {
						 featureBuilder.addTags(layerProps.addValue(val));
					}
				}
			}
		};
		if(tileGeom.mvtGeoms.size()==0)return null;
		final List<VectorTile.Tile.Feature> features = JtsAdapterReverse.toFeatures(tileGeom.mvtGeoms, layerProps, userDataConverter);
		final VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(layerName, layerParams);
		layerBuilder.addAllFeatures(features);
		MvtLayerBuild.writeProps(layerBuilder, layerProps);
		final VectorTile.Tile.Layer layer = layerBuilder.build();
		tileBuilder.addLayers(layer);
		Tile mvt = tileBuilder.build();
		byte[] bytes=mvt.toByteArray();
		return bytes;
	}
	
	public void createMVT(int zoom,Point p,File dir,GeometryFactory geomFactory,String layerName) throws IOException {
		File out=new File(dir.getAbsolutePath()+"\\"+Integer.toString(zoom));
		if(!out.exists())out.mkdir();
		File out2=new File(out.getAbsolutePath()+"\\"+Integer.toString(p.x));
		if(!out2.exists())out2.mkdir();
		File out3=new File(out2.getAbsolutePath()+"\\"+Integer.toString(p.y)+".pbf");
		MvtLayerParams layerParams = new MvtLayerParams();
		Envelope env=getTileBounds(p.x,p.y,zoom);
		IGeometryFilter acceptAllGeomFilter=new IGeometryFilter() {
			@Override
			public boolean accept(Geometry geometry) {
				return true;
			}
		};
//		String layerName="h3grid";
		TileGeomResult tileGeom = JtsAdapter.createTileGeom(geom, env, geomFactory, layerParams, acceptAllGeomFilter);
		final VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();
		final MvtLayerProps layerProps = new MvtLayerProps();
		IUserDataConverter userDataConverter=new IUserDataConverter() {
			@Override
			public void addTags(Object userData, MvtLayerProps layerProps, Builder featureBuilder) {
				JsonObject o=(JsonObject)userData;
				for(String s : o.keySet()) {
					if(layerProps.keyIndex(layerName)==null) {
						 featureBuilder.addTags(layerProps.addKey(s));
					}
					String val=o.get(s).getAsString();
					try {
						 featureBuilder.addTags(layerProps.addValue(Double.parseDouble(val)));
					}catch(Exception e) {
						 featureBuilder.addTags(layerProps.addValue(val));
					}
				}
			}
		};
		if(tileGeom.mvtGeoms.size()==0)return;
		final List<VectorTile.Tile.Feature> features = JtsAdapterReverse.toFeatures(tileGeom.mvtGeoms, layerProps, userDataConverter);
		final VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(layerName, layerParams);
		layerBuilder.addAllFeatures(features);
		MvtLayerBuild.writeProps(layerBuilder, layerProps);
		final VectorTile.Tile.Layer layer = layerBuilder.build();
		tileBuilder.addLayers(layer);
		Tile mvt = tileBuilder.build();
		byte[] bytes=mvt.toByteArray();
        Path path = out3.toPath();
        Files.write(path, bytes);
	}
	
	public void createMVT(int zoom,File dir) throws IOException {
		GeometryFactory geomFactory = new GeometryFactory();
		List<Point> list=MeshUtil.getTileList(this.bounds, zoom);
		File out=new File(dir.getAbsolutePath()+"\\"+Integer.toString(zoom));
		if(!out.exists())out.mkdir();
		for(Point p : list) {
			File out2=new File(out.getAbsolutePath()+"\\"+Integer.toString(p.x));
			if(!out2.exists())out2.mkdir();
			File out3=new File(out2.getAbsolutePath()+"\\"+Integer.toString(p.y)+".pbf");
			MvtLayerParams layerParams = new MvtLayerParams();
			Envelope env=getTileBounds(p.x,p.y,zoom);
			IGeometryFilter acceptAllGeomFilter=new IGeometryFilter() {
				@Override
				public boolean accept(Geometry geometry) {
					return true;
				}
			};
			String layerName="bldg";
			TileGeomResult tileGeom = JtsAdapter.createTileGeom(geom, env, geomFactory, layerParams, acceptAllGeomFilter);
			final VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();
			final MvtLayerProps layerProps = new MvtLayerProps();
			IUserDataConverter userDataConverter=new IUserDataConverter() {
				@Override
				public void addTags(Object userData, MvtLayerProps layerProps, Builder featureBuilder) {
					JsonObject o=(JsonObject)userData;
					for(String s : o.keySet()) {
						if(layerProps.keyIndex(layerName)==null) {
							 featureBuilder.addTags(layerProps.addKey(s));
						}
						String val=o.get(s).getAsString();
						try {
							 featureBuilder.addTags(layerProps.addValue(Double.parseDouble(val)));
						}catch(Exception e) {
							 featureBuilder.addTags(layerProps.addValue(val));
						}
					}
				}
			};
			if(tileGeom.mvtGeoms.size()==0)continue;
			final List<VectorTile.Tile.Feature> features = JtsAdapterReverse.toFeatures(tileGeom.mvtGeoms, layerProps, userDataConverter);
			final VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder(layerName, layerParams);
			layerBuilder.addAllFeatures(features);
			MvtLayerBuild.writeProps(layerBuilder, layerProps);
			final VectorTile.Tile.Layer layer = layerBuilder.build();
			tileBuilder.addLayers(layer);
			Tile mvt = tileBuilder.build();
			byte[] bytes=mvt.toByteArray();
	        Path path = out3.toPath();
	        Files.write(path, bytes);
		}
	}
	
	public void createMVTs(int minZoom,int maxZoom,File out) {
		for(int i=minZoom;i<=maxZoom;i++) {
			try {
				this.createMVT(i, out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static Envelope getTileBounds(int x, int y, int zoom){
	    return new Envelope(getLong(x, zoom), getLong(x + 1, zoom), getLat(y, zoom), getLat(y + 1, zoom));
	}

	public static double getLong(int x, int zoom){
	    return ( x / Math.pow(2, zoom) * 360 - 180 );
	}

	public static double getLat(int y, int zoom){
	    double r2d = 180 / Math.PI;
	    double n = Math.PI - 2 * Math.PI * y / Math.pow(2, zoom);
	    return r2d * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n)));
	}
	
	public static void main(String[] args) {
		File in=new File("\\\\149-128\\e\\業務\\五條土木\\五條市建物.geojson");
		try {
			VectorReader vr=VectorReader.createReader(6674, in);
			MVTBuilder mvt=new MVTBuilder(vr);
			mvt.createMVTs(10,17,new File("E:\\業務\\衛星_丹波篠山市\\mvt\\pbf2"));
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
	}
}
