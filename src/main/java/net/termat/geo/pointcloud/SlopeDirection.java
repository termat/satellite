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

public class SlopeDirection extends AbstractTerrainVector{

	public SlopeDirection(BufferedImage png, AffineTransform af) {
		super(png, af);
	}

	@Override
	public void create(int size) {
		createCell(size);
		double dx=Math.abs(af.getScaleX());
		double dy=Math.abs(af.getScaleY());
		for(int i=1;i<cell.length-1;i++){
			for(int j=1;j<cell[i].length-1;j++){
				double hx=(sum(cell[i-1][j-1],cell[i-1][j],cell[i-1][j+1])-sum(cell[i+1][j-1],cell[i+1][j],cell[i+1][j+1]))/(3*dx*size);
				double hy=(sum(cell[i-1][j+1],cell[i][j+1],cell[i+1][j+1])-sum(cell[i-1][j-1],cell[i][j-1],cell[i+1][j-1]))/(3*dy*size);
				hx=Math.round(hx*10000)/10000;
				hy=Math.round(hy*10000)/10000;
				if(hx>0){
					if(hy>0){			//北東
					    setArea(cell[i][j],map,"北東",af);
					}else if(hy<0){	//南東
						setArea(cell[i][j],map,"南東",af);
					}else{				//東
						setArea(cell[i][j],map,"東",af);
					}
				}else if(hx<0){
					if(hy>0){			//北西
						setArea(cell[i][j],map,"北西",af);
					}else if(hy<0){	//南西
						setArea(cell[i][j],map,"南西",af);
					}else{				//西
						setArea(cell[i][j],map,"西",af);
					}
				}else{
					if(hy>0){			//北
						setArea(cell[i][j],map,"北",af);
					}else if(hy<0){	//南
						setArea(cell[i][j],map,"南",af);
					}else{				//平坦
						setArea(cell[i][j],map,"平坦",af);
					}
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
					prop.put("direction", degree);
					addFeature(s,prop);
				}
			}
		}


	}

	public static void main(String[] args){
		File f=new File("dem.png");
		try {
			BufferedImage img=ImageIO.read(f);
			AffineTransform af=PCUtil.loadTransform(new File("test.pgw"));
			SlopeDirection app=new SlopeDirection(img,af);
			app.setCoordSys(8);
			app.create(20);
			app.outGeojson(new File("test.geojson"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
