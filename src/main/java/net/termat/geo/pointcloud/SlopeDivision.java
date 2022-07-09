package net.termat.geo.pointcloud;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import net.termat.geo.util.PCUtil;

public class SlopeDivision extends AbstractTerrainVector{
	private Integer[] degree=new Integer[]{0,5,10,15,20,25,30,35,40,45,50,55,60,90};

	public SlopeDivision(BufferedImage png, AffineTransform af) {
		super(png, af);
	}

	@Override
	public void create(int size) {
		createCell(size);
		double dx=Math.abs(af.getScaleX());
		double dy=Math.abs(af.getScaleY());
		for(int i=1;i<cell.length-1;i++){
			for(int j=1;j<cell[i].length-1;j++){
				double[][] px=getPixelDataCell(i,j,cell);
				double sv=getSlopeVal(px,dx*size,dy*size);
				double deg=Math.toDegrees(Math.atan(sv));
				if(deg<5){
					setArea(cell[i][j],map, degree[0],af);
				}else if(deg<10){
					setArea(cell[i][j],map, degree[1],af);
				}else if(deg<15){
					setArea(cell[i][j],map, degree[2],af);
				}else if(deg<20){
					setArea(cell[i][j],map, degree[3],af);
				}else if(deg<25){
					setArea(cell[i][j],map, degree[4],af);
				}else if(deg<30){
					setArea(cell[i][j],map, degree[5],af);
				}else if(deg<35){
					setArea(cell[i][j],map, degree[6],af);
				}else if(deg<40){
					setArea(cell[i][j],map, degree[7],af);
				}else if(deg<45){
					setArea(cell[i][j],map, degree[8],af);
				}else if(deg<50){
					setArea(cell[i][j],map, degree[9],af);
				}else if(deg<55){
					setArea(cell[i][j],map, degree[10],af);
				}else if(deg<60){
					setArea(cell[i][j],map, degree[11],af);
				}else{
					setArea(cell[i][j],map, degree[12],af);
				}
			}
		}
	}

	@Override
	protected void createGeojson() {
		this.inregrade();
		for(Object degree: map.keySet()){
			List<Area> area=map.get(degree);
			for(Area a : area){
				List<Shape> sp=getShapes(a);
				for(Shape s : sp){
					Map<String,Object> prop=new HashMap<>();
					prop.put("degree", degree);
					addFeature(s,prop);
				}
			}
		}
	}

	private double getSlopeVal(double[][] p,double dx,double dy){
		double sx=(p[0][0]+p[1][0]+p[2][0]-(p[0][2]+p[1][2]+p[2][2]))/(6*dx);
		double sy=(p[0][0]+p[0][1]+p[0][2]-(p[2][0]+p[2][1]+p[2][2]))/(6*dy);
		return Math.sqrt(sx*sx+sy*sy);
	}

	public static void main(String[] args){
		File f=new File("dem.png");
		try {
			BufferedImage img=ImageIO.read(f);
			AffineTransform af=PCUtil.loadTransform(new File("test.pgw"));
			SlopeDivision app=new SlopeDivision(img,af);
			app.setCoordSys(8);
			app.create(20);
			app.outGeojson(new File("slope2.geojson"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
