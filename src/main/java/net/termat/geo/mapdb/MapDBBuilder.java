package net.termat.geo.mapdb;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import net.termat.components.solver.Solver;
import net.termat.components.solver.SolverTable;
import net.termat.geo.satellite.BandReader;
import net.termat.geo.satellite.ImageReader;
import net.termat.geo.satellite.MeshUtil;
import net.termat.geo.util.PCUtil;

public class MapDBBuilder {
	private static Map<Integer,Integer> coord=getCoordSys();
	private MapDB db;
	private SolverTable table;
	private int epsg;
	private MeshUtil.BasicMeshType meshType=MeshUtil.BasicMeshType.S2500;
	private double resolution=10.0;
	
	public MapDBBuilder(MapDB db,int epsg) {
		this.db=db;
		this.epsg=epsg;
		table=new SolverTable();
	}
	
	public void setMeshType(MeshUtil.BasicMeshType type) {
		this.meshType=type;
	}
	
	public double getResolution() {
		return resolution;
	}

	public void setResolution(double resolution) {
		this.resolution = resolution;
	}

	public MeshUtil.BasicMeshType getMeshType() {
		return meshType;
	}

	private static Map<Integer,Integer> getCoordSys(){
		Map<Integer,Integer> ret=new HashMap<>();
		for(int i=1;i<=19;i++) {
			ret.put(6668+i, i);
		}
		return ret;
	}
	
	public void addSource(File f,int col,String src,String band,long date) {
		MapSolver sol=new MapSolver(f,col,src,band,date);
		table.addSolver(sol, true);
	}
	
	private class MapSolver extends Solver{
		private File f;
		private String src;
		private String band;
		private int col;
		private long date;
		private BandReader sr=null;
		private ImageReader ir=null;
		private Rectangle2D rect;
		private String[] areas;
		
		
		public MapSolver(File f,int col,String src,String band,long date) {
			this.f=f;
			this.col=col;
			this.src=src;
			this.band=band;
			this.date=date;
			this.completed=0;
			this.iter=1;
		}
		
		private void preprocess(){
			String name=f.getName().toLowerCase();
			if(name.endsWith(".tif")||name.endsWith(".jp2")||name.endsWith(".tiff")) {
				sr=BandReader.createReader(f);
				if(sr.getEPSG()!=epsg) {
					sr=sr.createProjectionData(epsg);
				}
				rect=sr.getBounds();
				areas=MeshUtil.getBasicMeshList(coord.get(sr.getEPSG()), rect, meshType);
				this.iter=areas.length;
			}else if(name.endsWith(".png")||name.endsWith(".jpg")) {
				try {
					BufferedImage img = ImageIO.read(f);
					File fa=new File(f.getAbsolutePath().replace(".png", ".pgw").replace(".jpg", ".jgw").replace(".PNG", ".PGW").replace(".JPG", ".JGW"));
					AffineTransform af=PCUtil.loadTransform(fa);
					ir=ImageReader.createReader(epsg,img,af,band);
					rect=ir.getBounds();
					areas=MeshUtil.getBasicMeshList(coord.get(ir.getEPSG()), rect, meshType);
					this.iter=areas.length;
				}catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		@Override
		public void calc() {
			if(completed==0) {
				preprocess();
			}
			String area=areas[completed];
			if(sr!=null) {
				Rectangle2D tmp=MeshUtil.getBasicMeshBounds(area);
				BandReader sr2=sr.createSubImage(tmp,resolution);
				try {
					db.add(sr2,col, src,area,band, date);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}else if(ir!=null) {
				Rectangle2D tmp=MeshUtil.getBasicMeshBounds(area);
				try {
					ImageReader ir2=ir.createSubImage(tmp,resolution);
					db.add(ir2, src,area,band,date);
				} catch (SQLException | IOException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void canceled() {
			this.completed=this.iter;
		}

		@Override
		public void completed() {}

		@Override
		public String getName() {
			return f.getName();
		}
	}
	
}
