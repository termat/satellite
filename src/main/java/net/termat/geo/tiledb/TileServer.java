package net.termat.geo.tiledb;

import static spark.Spark.get;

import java.awt.geom.Point2D;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import spark.ModelAndView;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

public class TileServer {
	private Map<String,TileDB> tiles;
	private Map<String,TileProperty> props;
	private List<Tiles> images;
	private List<Tiles> dem;
	private List<Tiles> vectors;
	private List<Map<String,String>> geojson;
	private Point2D pos;

	public static void runServer() {
		try {
			new TileServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private TileServer() throws ClassNotFoundException, SQLException {
		Optional<String> optionalPort = Optional.ofNullable(System.getenv("PORT"));
		optionalPort.ifPresent( p -> {
			int port = Integer.parseInt(p);
			Spark.port(port);
		});
		Spark.staticFileLocation("/public");
		new CorsFilter().apply();
		tiles=new HashMap<>();
		props=new HashMap<>();
		File dir=new File("db");
		for(File f : dir.listFiles()) {
			String name=f.getName();
			TileDB tdb=TileDB.open(f);
			tiles.put(name, tdb);
			props.put(name,tdb.getProperty());
		}
		images=new ArrayList<>();
		dem=new ArrayList<>();
		vectors=new ArrayList<>();
		geojson=getGeojsonList();
		Point2D tmp=null;
		for(File f : reverse(dir.listFiles())) {
			System.out.println(f.getAbsolutePath());
			if(!f.getName().endsWith(".db"))continue;
			TileDB db;
			try {
				db=TileDB.open(f);
				if(tmp==null)tmp=db.getCenter();
				TileProperty tc=db.getProperty();
				if(tc.content.equals("image")) {
					Tiles cp=new Tiles();
					cp.url=f.getName()+"/{z}/{x}/{y}";
					cp.attr=tc.copyright;
					cp.min=tc.minZoom;
					cp.max=tc.maxZoom;
					cp.name=tc.name;
					String name=f.getName();
					if(name.contains("DEM")||name.contains("DSM")) {
						cp.id="000"+Integer.toString(dem.size());
						cp.id=cp.id.substring(cp.id.length()-2);
						dem.add(cp);
					}else {
						cp.id="000"+Integer.toString(images.size());
						cp.id=cp.id.substring(cp.id.length()-2);
						images.add(cp);
					}
				}else if(tc.content.equals("application")) {
					Tiles cp=new Tiles();
					cp.url=f.getName()+"/{z}/{x}/{y}";
					cp.attr=tc.copyright;
					cp.min=tc.minZoom;
					cp.max=tc.maxZoom;
					cp.name=tc.name;
					cp.id="000"+Integer.toString(vectors.size());
					cp.id=cp.id.substring(cp.id.length()-2);
					vectors.add(cp);
				}
			} catch ( SQLException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			pos=tmp;
		}
		
		get("/:type/:z/:x/:y", (request, response) -> {
	    	try{
				String type=request.params("type");
				Integer z=Integer.parseInt(request.params("z"));
				Integer x=Integer.parseInt(request.params("x"));
				Integer y=Integer.parseInt(request.params("y"));
				TileDB db=tiles.get(type);
				if(db==null) {
					response.status(400);
					return null;
				}
				TileProperty tp=props.get(type);
				byte[] bb=db.getTileBytes(z, x, y);
				if(bb!=null){
					response.status(200);
					response.header("Content-Type", tp.content+"/"+tp.type);
					return bb;
				}else{
					response.status(400);
					return null;
				}
	    	}catch(Exception e){
	        	response.status(400);
	    		return null;
	    	}
		});

		get("/map2d",(request, response) -> {
			Map<String, Object> model = new HashMap<>();
			model.put("captions", images);
			model.put("vectors", vectors);
			model.put("jsons", geojson);
			model.put("centerX", pos.getX());
			model.put("centerY", pos.getY());
			return new ModelAndView(model, "map2d.mustache");
			},new MustacheTemplateEngine());

		get("/map2d2",(request, response) -> {
			Map<String, Object> model = new HashMap<>();
			model.put("captions", images);
			model.put("vectors", vectors);
			model.put("jsons", geojson);
			model.put("centerX", pos.getX());
			model.put("centerY", pos.getY());
			return new ModelAndView(model, "map2d2.mustache");
			},new MustacheTemplateEngine());
	
		get("/map3d",(request, response) -> {
			Map<String, Object> model = new HashMap<>();
			model.put("captions", images);
			model.put("dem", dem);
			model.put("vectors", vectors);
			model.put("jsons", geojson);
			model.put("centerX", pos.getX());
			model.put("centerY", pos.getY());
			return new ModelAndView(model, "map3d.mustache");
			},new MustacheTemplateEngine());
	}
	
	private static List<Map<String,String>> getGeojsonList(){
		List<Map<String,String>> geojson=new ArrayList<>();
		File ff=new File("");
		ff=new File(ff.getAbsolutePath()+"/public/geojson");
		for(File f : ff.listFiles()) {
			Map<String,String> m=new HashMap<>();
			m.put("name", f.getName());
			geojson.add(m);
			System.out.println(f.getName());
		}
		return geojson;
	}
	
	@SuppressWarnings("unused")
	private static class Tiles{
		String id;
		String url;
		String name;
		String attr;
		int min;
		int max;
	}
	
	private static List<File> reverse(File[] ff){
		List<File> ret=new ArrayList<>();
		for(File f : ff)ret.add(0, f);
		return ret;
	}
}
