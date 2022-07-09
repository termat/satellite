package net.termat.geo.mapdb;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.gdal.osr.CoordinateTransformation;

import com.google.gson.JsonObject;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import net.termat.geo.satellite.BandReader;
import net.termat.geo.satellite.BandUtil;
import net.termat.geo.satellite.ImageReader;
import net.termat.geo.satellite.MeshUtil;

public class MapDB {
	private ConnectionSource connectionSource = null;
	private  Dao<MapIndex,Long> indexDao;
	private  Dao<MapBand,Long> bandDao;
	private  Dao<MapProperty,Long> propDao;

	public static MapDB connect(File f) throws ClassNotFoundException, SQLException {
		MapDB db=new MapDB();
		db.connectDB(f.getAbsolutePath(),true);
		return db;
	}

	private void connectDB(String dbName,boolean create) throws SQLException, ClassNotFoundException{
		if(!dbName.endsWith(".db"))dbName=dbName+".db";
		Class.forName("org.sqlite.JDBC");
		connectionSource = new JdbcConnectionSource("jdbc:sqlite:"+dbName);
		indexDao= DaoManager.createDao(connectionSource, MapIndex.class);
		if(create)TableUtils.createTableIfNotExists(connectionSource, MapIndex.class);
		bandDao= DaoManager.createDao(connectionSource, MapBand.class);
		if(create)TableUtils.createTableIfNotExists(connectionSource, MapBand.class);
		propDao= DaoManager.createDao(connectionSource, MapProperty.class);
		if(create)TableUtils.createTableIfNotExists(connectionSource, MapProperty.class);
		if(nullEPSG()) {
			MapProperty me=new MapProperty();
			me.id=1l;
			me.epsg=4326;
			propDao.create(me);
		}
	}

	private boolean nullEPSG() throws SQLException {
		List<MapProperty> li=propDao.queryForAll();
		return li.size()==0;
	}
	
	public void setEPSG(int epsg) throws SQLException {
		MapProperty me=propDao.queryForId(1l);
		me.epsg=epsg;
		propDao.update(me);
	}
	
	public int getEPSG() throws SQLException {
		MapProperty me=propDao.queryForId(1l);
		return me.epsg;
	}
	
	public void add(BandReader sr,int col,String src,String area,String band,long date) throws SQLException {
		MapIndex index=getIndex(src,area,band,date);
		if(index==null) {
			index=new MapIndex();
			float[][] val=sr.getBand(col);
			index.width=val.length;
			index.height=val[0].length;
			index.src=src;
			index.area=area;
			index.band=band;
			index.date=date-date%(60*60*24*1000);
			index.key=System.currentTimeMillis();
			index.setTransform(sr.getTransform());
			Rectangle2D r=index.getBounds();
			index.west=r.getX();
			index.east=r.getX()+r.getWidth();
			index.north=r.getY();
			index.south=r.getY()+r.getHeight();
			index.type=MapIndex.DataType.FLOAT;
			MapBand b=new MapBand();
			b.key=index.key;
			b.setBand(val);
			indexDao.create(index);
			bandDao.create(b);
		}else {
			MapBand b=getBand(index);
			updateFloat(index, b, sr.getBand(col));
		}
	}
	
	public void add(ImageReader ir,String src,String area,String band,long date) throws IOException, SQLException {
		MapIndex index=getIndex(src,area,band,date);
		if(index==null) {
			index=new MapIndex();
			index.area=area;
			BufferedImage val=ir.getBand();
			index.width=val.getWidth();
			index.height=val.getHeight();
			index.src=src;
			index.band=band;
			index.date=date-date%(60*60*24*1000);
			index.key=System.currentTimeMillis();
			index.setTransform(ir.getTransform());
			Rectangle2D r=index.getBounds();
			index.west=r.getX();
			index.east=r.getX()+r.getWidth();
			index.north=r.getY();
			index.south=r.getY()+r.getHeight();
			index.type=MapIndex.DataType.IMAGE;
			MapBand b=new MapBand();
			b.key=index.key;
			b.setBand(val);
			indexDao.create(index);
			bandDao.create(b);
		}else {
			MapBand b=getBand(index);
			updateImage(index, b, ir.getBand());
		}
	}
	
	public List<String> getSourceList() throws SQLException{
		List<String> ret=new ArrayList<>();
		QueryBuilder<MapIndex, Long> query=indexDao.queryBuilder();
		query.groupBy("src").selectColumns("src");
		List<MapIndex> li=indexDao.query(query.prepare());
		for(MapIndex m : li) {
			ret.add(m.src);
		}
		return ret;
	}
	
	public List<String> getAreaList() throws SQLException{
		List<String> ret=new ArrayList<>();
		QueryBuilder<MapIndex, Long> query=indexDao.queryBuilder();
		query.groupBy("area").selectColumns("area");
		List<MapIndex> li=indexDao.query(query.prepare());
		for(MapIndex m : li) {
			ret.add(m.area);
		}
		return ret;
	}
	
	public String getAreaJson() throws SQLException {
		List<String> li=getAreaList();
		List<Feature> ff=new ArrayList<>();
		for(String str : li) {
			Rectangle2D rec=MeshUtil.getBasicMeshBounds(str);
			List<Point> p=new ArrayList<>();
			p.add(Point.fromLngLat(rec.getX(), rec.getY()));
			p.add(Point.fromLngLat(rec.getX()+rec.getWidth(), rec.getY()));
			p.add(Point.fromLngLat(rec.getX()+rec.getWidth(), rec.getY()+rec.getHeight()));
			p.add(Point.fromLngLat(rec.getX(), rec.getY()+rec.getHeight()));
			p.add(Point.fromLngLat(rec.getX(), rec.getY()));
			List<List<Point>> pl=new ArrayList<>();
			pl.add(p);
			Geometry geom=Polygon.fromLngLats(pl);
			JsonObject obj=new JsonObject();
			obj.addProperty("name", str);
			Feature fe=Feature.fromGeometry(geom, obj);
			ff.add(fe);
		}
		return FeatureCollection.fromFeatures(ff).toJson();
	}
	
	public List<String> getBandList() throws SQLException{
		List<String> ret=new ArrayList<>();
		QueryBuilder<MapIndex, Long> query=indexDao.queryBuilder();
		query.groupBy("band").selectColumns("band");
		List<MapIndex> li=indexDao.query(query.prepare());
		for(MapIndex m : li) {
			ret.add(m.band);
		}
		return ret;
	}
	
	public List<Long> getDateList() throws SQLException{
		List<Long> ret=new ArrayList<>();
		QueryBuilder<MapIndex, Long> query=indexDao.queryBuilder();
		query.groupBy("date").selectColumns("date");
		List<MapIndex> li=indexDao.query(query.prepare());
		for(MapIndex m : li) {
			ret.add(m.date);
		}
		return ret;
	}
	
	private MapBand getBand(MapIndex index) throws SQLException {
		QueryBuilder<MapBand, Long> query=bandDao.queryBuilder();
		query.where().eq("key", index.key);
		List<MapBand> li=bandDao.query(query.prepare());
		if(li.size()==0) {
			return null;
		}else {
			return li.get(0);
		}
	}
	
	private void updateFloat(MapIndex id,MapBand mb,float[][] d) throws SQLException {
		float[][] src=mb.getBandAsFloat(id.width, id.height);
		for(int i=0;i<src.length;i++) {
			for(int j=0;j<src[0].length;j++) {
				if(src[i][j]==0)src[i][j]=d[i][j];
			}
		}
		mb.setBand(src);
		bandDao.update(mb);
	}
	
	private void updateImage(MapIndex id,MapBand mb,BufferedImage b) throws SQLException, IOException {
		BufferedImage src=mb.getBandAsImage();
		for(int i=0;i<id.width;i++) {
			for(int j=0;j<id.height;j++) {
				if(src.getRGB(i, j)==0)src.setRGB(i, j, b.getRGB(i,j));
			}
		}
		mb.setBand(src);
		bandDao.update(mb);
	}
	
	public List<MapIndex> getIndexesSrc(String src) throws SQLException {
		QueryBuilder<MapIndex, Long> query=indexDao.queryBuilder();
		query.where().eq("src", src);
		return indexDao.query(query.prepare());
	}
	
	public MapIndex getIndex(String src,String area,String band,long date) throws SQLException {
		QueryBuilder<MapIndex, Long> query=indexDao.queryBuilder();
		query.where().eq("src", src).and().eq("area", area).and().eq("band", band).and().eq("date", date);
		List<MapIndex> list=indexDao.query(query.prepare());
		if(list.size()==0) {
			return null;
		}else {
			return list.get(0);
		}
	}
	
	public List<MapIndex> getIndexes(String src,String band,long date,double x,double y) throws SQLException{
		QueryBuilder<MapIndex, Long> query=indexDao.queryBuilder();
		query.where().eq("src", src).and().eq("band", band).and().eq("date", date).and().le("west", x).and().ge("east", x).and().ge("south", y).and().le("north", y);
		return indexDao.query(query.prepare());
	}
	
	public List<MapIndex> getIndexes(String src,String band,long date,Rectangle2D rect) throws SQLException{
		QueryBuilder<MapIndex, Long> query=indexDao.queryBuilder();
		query.where().eq("src", src).and().eq("band", band).and().eq("date", date).and().le("west", rect.getX()+rect.getWidth()).and().ge("east", rect.getX()).and()
			.ge("south", rect.getY()).and().le("north", rect.getY()+rect.getHeight());
		return indexDao.query(query.prepare());
	}
	
	public float[][] getValueAsFloat(MapIndex index) throws SQLException{
		return getBand(index).getBandAsFloat(index.width, index.height);
	}
	
	public BandReader getImageAsFloat(String src,String band,long date,Rectangle2D rect,double resolution) throws SQLException {
		int sizeX=(int)Math.round(rect.getWidth()/resolution);
		int sizeY=(int)Math.round(rect.getHeight()/resolution);
		AffineTransform af=new AffineTransform(new double[] {
				resolution,0,0,-resolution,rect.getX(),rect.getY()+rect.getHeight()});
		float[][] data=new float[sizeX][sizeY];
		Point2D[][] pt=new Point2D[sizeX][sizeY];
		for(int i=0;i<sizeX;i++) {
			for(int j=0;j<sizeY;j++) {
				pt[i][j]=af.transform(new Point2D.Double(i, j), new Point2D.Double());
			}
		}
		List<MapIndex> li=getIndexes(src,band,date,rect);
		for(MapIndex mi : li) {
			if(mi.type!=MapIndex.DataType.FLOAT)continue;
			float[][] val=getBand(mi).getBandAsFloat(mi.width, mi.height);
			try {
				AffineTransform at=mi.getTransform().createInverse();
				for(int i=0;i<sizeX;i++) {
					for(int j=0;j<sizeY;j++) {
						Point2D p=at.transform(pt[i][j], new Point2D.Double());
						int px=(int)Math.round(p.getX());
						int py=(int)Math.round(p.getY());
						if(px>=0&&px<mi.width&&py>=0&&py<mi.height) {
							data[i][j]=val[px][py];
						}
					}
				}
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
		}
		return BandReader.createReader(getEPSG(), af, data);
	}
	
	public ImageReader getImageAsImage(String src,String band,long date,Rectangle2D rect,double resolution) throws SQLException, IOException {
		int sizeX=(int)Math.round(rect.getWidth()/resolution);
		int sizeY=(int)Math.round(rect.getHeight()/resolution);
		AffineTransform af=new AffineTransform(new double[] {
				resolution,0,0,-resolution,rect.getX(),rect.getY()+rect.getHeight()});
		BufferedImage data=new BufferedImage(sizeX,sizeY,BufferedImage.TYPE_INT_RGB);
		Point2D[][] pt=new Point2D[sizeX][sizeY];
		for(int i=0;i<sizeX;i++) {
			for(int j=0;j<sizeY;j++) {
				pt[i][j]=af.transform(new Point2D.Double(i, j), new Point2D.Double());
			}
		}
		List<MapIndex> li=getIndexes(src,band,date,rect);
		for(MapIndex mi : li) {
			if(mi.type!=MapIndex.DataType.IMAGE)continue;
			BufferedImage val=getBand(mi).getBandAsImage();
			try {
				AffineTransform at=mi.getTransform().createInverse();
				for(int i=0;i<sizeX;i++) {
					for(int j=0;j<sizeY;j++) {
						Point2D p=at.transform(pt[i][j], new Point2D.Double());
						int px=(int)Math.round(p.getX());
						int py=(int)Math.round(p.getY());
						if(px>=0&&px<mi.width&&py>=0&&py<mi.height) {
							data.setRGB(i, j, val.getRGB(px, py));
						}
					}
				}
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
		}
		return ImageReader.createReader(getEPSG(), data, af, band);
	}
	
	public BandReader getImageAsFloat(int epsgTarget,String src,String band,long date,Rectangle2D rect,double resolution) throws SQLException {
		int sizeX=(int)Math.round(rect.getWidth()/resolution);
		int sizeY=(int)Math.round(rect.getHeight()/resolution);
		AffineTransform af=new AffineTransform(new double[] {
				resolution,0,0,-resolution,rect.getX(),rect.getY()+rect.getHeight()});
		float[][] data=new float[sizeX][sizeY];
		Point2D[][] pt=new Point2D[sizeX][sizeY];
		
		CoordinateTransformation ct=BandUtil.getCoordinateTransformation(
				BandUtil.createSpatialReference(epsgTarget),
				BandUtil.createSpatialReference(getEPSG()));
		for(int i=0;i<sizeX;i++) {
			for(int j=0;j<sizeY;j++) {
				Point2D px=af.transform(new Point2D.Double(i, j), new Point2D.Double());
				double[] p=ct.TransformPoint(px.getX(), px.getY());
				pt[i][j]=new Point2D.Double(p[0], p[1]);
			}
		}
		List<MapIndex> li=getIndexes(src,band,date,rect);
		for(MapIndex mi : li) {
			if(mi.type!=MapIndex.DataType.FLOAT)continue;
			float[][] val=getBand(mi).getBandAsFloat(mi.width, mi.height);
			try {
				AffineTransform at=mi.getTransform().createInverse();
				for(int i=0;i<sizeX;i++) {
					for(int j=0;j<sizeY;j++) {
						Point2D p=at.transform(pt[i][j], new Point2D.Double());
						int px=(int)Math.round(p.getX());
						int py=(int)Math.round(p.getY());
						if(px>=0&&px<mi.width&&py>=0&&py<mi.height) {
							data[i][j]=val[px][py];
						}
					}
				}
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
		}
		return BandReader.createReader(getEPSG(), af, data);
	}
	
	public ImageReader getImageAsImage(int epsgTarget,String src,String band,long date,Rectangle2D rect,double resolution) throws SQLException, IOException {
		int sizeX=(int)Math.round(rect.getWidth()/resolution);
		int sizeY=(int)Math.round(rect.getHeight()/resolution);
		AffineTransform af=new AffineTransform(new double[] {
				resolution,0,0,-resolution,rect.getX(),rect.getY()+rect.getHeight()});
		BufferedImage data=new BufferedImage(sizeX,sizeY,BufferedImage.TYPE_INT_RGB);
		Point2D[][] pt=new Point2D[sizeX][sizeY];
		CoordinateTransformation ct=BandUtil.getCoordinateTransformation(
				BandUtil.createSpatialReference(epsgTarget),
				BandUtil.createSpatialReference(getEPSG()));
		for(int i=0;i<sizeX;i++) {
			for(int j=0;j<sizeY;j++) {
				Point2D px=af.transform(new Point2D.Double(i, j), new Point2D.Double());
				double[] p=ct.TransformPoint(px.getX(), px.getY());
				pt[i][j]=new Point2D.Double(p[0], p[1]);
			}
		}
		List<MapIndex> li=getIndexes(src,band,date,rect);
		for(MapIndex mi : li) {
			if(mi.type!=MapIndex.DataType.IMAGE)continue;
			BufferedImage val=getBand(mi).getBandAsImage();
			try {
				AffineTransform at=mi.getTransform().createInverse();
				for(int i=0;i<sizeX;i++) {
					for(int j=0;j<sizeY;j++) {
						Point2D p=at.transform(pt[i][j], new Point2D.Double());
						int px=(int)Math.round(p.getX());
						int py=(int)Math.round(p.getY());
						if(px>=0&&px<mi.width&&py>=0&&py<mi.height) {
							data.setRGB(i, j, val.getRGB(px, py));
						}
					}
				}
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
		}
		return ImageReader.createReader(getEPSG(), data, af, band);
	}
	
	public BufferedImage getTileAsImage(String src,String band,long date,int zoom,int x,int y) throws SQLException, IOException {
		int sizeX=256;
		int sizeY=256;
		Rectangle2D r=MeshUtil.getTileBounds(zoom, x, y);
		BufferedImage data=new BufferedImage(sizeX,sizeY,BufferedImage.TYPE_INT_RGB);
		AffineTransform af=new AffineTransform(new double[] {
				r.getWidth()/(double)sizeX,0,0,-r.getHeight()/(double)sizeY,r.getX(),r.getY()+r.getHeight()});
		CoordinateTransformation ct=BandUtil.getCoordinateTransformation(
				BandUtil.createSpatialReference(4326),
				BandUtil.createSpatialReference(getEPSG()));
		Point2D[][] pt=new Point2D[sizeX][sizeY];
		Rectangle2D r2=null;
		for(int i=0;i<sizeX;i++) {
			for(int j=0;j<sizeY;j++) {
				Point2D px=af.transform(new Point2D.Double(i, j), new Point2D.Double());
				double[] p=ct.TransformPoint(px.getX(), px.getY());
				pt[i][j]=new Point2D.Double(p[0], p[1]);
				if(r2==null) {
					r2=new Rectangle2D.Double(pt[i][j].getX(),pt[i][j].getY(),0,0);
				}else {
					r2.add(pt[i][j]);
				}
			}
		}
		List<MapIndex> li=getIndexes(src,band,date,r2);
		for(MapIndex mi : li) {
			if(mi.type!=MapIndex.DataType.IMAGE)continue;
			BufferedImage val=getBand(mi).getBandAsImage();
			try {
				AffineTransform at=mi.getTransform().createInverse();
				for(int i=0;i<sizeX;i++) {
					for(int j=0;j<sizeY;j++) {
						Point2D p=at.transform(pt[i][j], new Point2D.Double());
						int px=(int)Math.round(p.getX());
						int py=(int)Math.round(p.getY());
						if(px>=0&&px<mi.width&&py>=0&&py<mi.height) {
							data.setRGB(i, j, val.getRGB(px, py));
						}
					}
				}
			} catch (NoninvertibleTransformException e) {
				e.printStackTrace();
			}
		}
		return data;
	}
}
