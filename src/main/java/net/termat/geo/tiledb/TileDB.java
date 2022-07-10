package net.termat.geo.tiledb;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import net.termat.geo.satellite.MeshUtil;

public class TileDB {
	private ConnectionSource connectionSource = null;
	private Dao<Tile,Long> tile;
	private Dao<TileProperty,Long> prop;
	private Connection connection;
	
	public static TileDB open(File f) throws ClassNotFoundException, SQLException {
		TileDB db=new TileDB();
		db.connectDB(f.getAbsolutePath());
		db.connection = SQLHelper.establishConnection(f);
		return db;
	}
	
	private void connectDB(String dbName) throws SQLException, ClassNotFoundException{
		if(!dbName.endsWith(".db"))dbName=dbName+".db";
		Class.forName("org.sqlite.JDBC");
		connectionSource = new JdbcConnectionSource("jdbc:sqlite:"+dbName);
		tile= DaoManager.createDao(connectionSource, Tile.class);
		TableUtils.createTableIfNotExists(connectionSource, Tile.class);
		prop= DaoManager.createDao(connectionSource, TileProperty.class);
		TableUtils.createTableIfNotExists(connectionSource, TileProperty.class);
		init();
	}
	
	private void init() {
		try {
			if(prop.queryForAll().size()==0) {
				TileProperty p=new TileProperty();
				prop.create(p);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void add(Tile t) throws SQLException {
		tile.create(t);
	}
	
	public void update(Tile t) throws SQLException {
		tile.update(t);
	}
	
	public void add(List<Tile> tl) throws SQLException {
		tile.create(tl);
	}
	
	public Tile getTile(int z,int x,int y) throws SQLException {
		QueryBuilder<Tile, Long> query=tile.queryBuilder();
		query.where().eq("z", z).and().eq("x", x).and().eq("y", y);
		List<Tile> li=tile.query(query.prepare());
		if(li.size()==0) {
			return null;
		}else {
			return li.get(0);
		}
	}
	
	public List<Tile> getTileAtZoom(int z) throws SQLException {
		QueryBuilder<Tile, Long> query=tile.queryBuilder();
		query.where().eq("z", z);
		return tile.query(query.prepare());
	}
	
    public byte[] getTileBytes(int zoom, int x, int y) throws IOException{
    	String sql = String.format("SELECT bytes FROM tile WHERE z = %d AND x = %d AND y = %d", zoom, x, y);
		InputStream tileDataInputStream = null;
    	try {
			ResultSet resultSet = SQLHelper.executeQuery(connection, sql);
			if(resultSet.isClosed()) {
				return null;
			}else {
				tileDataInputStream = resultSet.getBinaryStream("bytes");
				byte[] ret=tileDataInputStream.readAllBytes();
	            return ret;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}finally {
			tileDataInputStream.close();
		}
    }
	
	public List<Tile> getAll() throws SQLException{
		return tile.queryForAll();
	}
	
	public TileProperty getProperty() throws SQLException {
		return prop.queryForAll().get(0);
	}

	public void setProperty(TileProperty p) throws SQLException {
		TileProperty t=prop.queryForAll().get(0);
		t.maxZoom=p.maxZoom;
		t.minZoom=p.minZoom;
		t.content=p.content;
		t.type=p.type;
		t.copyright=p.copyright;
		t.name=p.name;
		t.bounds=p.bounds;
		prop.update(t);
	}
	
	public Rectangle2D getBounds() throws SQLException {
		TileProperty tp=getProperty();
//		return tp.getBounds();
		QueryBuilder<Tile, Long> query=tile.queryBuilder();
		query.where().eq("z", tp.minZoom);
		List<Tile> li=tile.query(query.prepare());
		Rectangle2D ret=null;
		for(Tile t : li) {
			Rectangle2D r=MeshUtil.getTileBounds(t.z, t.x, t.y);
			if(ret==null) {
				ret=r;
			}else {
				ret.add(r);
			}
		}
		return ret;
	}
	
	public Point2D getCenter() throws SQLException {
		Rectangle2D r=getBounds();
		double x=r.getCenterX();
		double y=r.getCenterY();
		return new Point2D.Double(x, y);
	}
	
}
