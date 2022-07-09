package net.termat.geo.pointcloud;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import net.termat.geo.util.PCFilter;

public class RidgeValleyAtMedian extends AbstractTerrainRaster{
	private int median_size=3;

	public RidgeValleyAtMedian(BufferedImage png, AffineTransform af) {
		super(png, af);
	}
	
	public RidgeValleyAtMedian(double[][] npy, AffineTransform af) {
		super(npy, af);
	}

	@Override
	public void create(int size) {
		createCell(size);
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				cell[i][j].val=cell[i][j].getH();
			}
		}
		double[][] val=new double[cell.length][cell[0].length];
		for(int i=0;i<val.length;i++){
			for(int j=0;j<val[i].length;j++){
				val[i][j]=cell[i][j].val;
			}
		}
		if(median_size==3){
			val=PCFilter.median3(val);
		}else{
			val=PCFilter.median5(val);
		}
		for(int i=0;i<val.length;i++){
			for(int j=0;j<val[i].length;j++){
				if (Double.isNaN(cell[i][j].val)) continue;
				double v1=Math.round(cell[i][j].val*10)/10;
				double v2=Math.round(val[i][j]*10)/10;
				double tp=v1-v2;
				if(tp>0){
					cell[i][j].val=1.0;
				}else if(tp<0){
					cell[i][j].val=-1.0;
				}else{
					cell[i][j].val=0.0;
				}
			}
		}
	}

	public int getMedian_size() {
		return median_size;
	}

	public void setMedian_size(int median_size) {
		this.median_size = median_size;
	}

}
