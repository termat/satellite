package net.termat.geo.pointcloud;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import net.termat.geo.satellite.BandReader;

public class Slope extends AbstractTerrainRaster{

	public Slope(BandReader br,int channel) {
		super(br,channel);
	}
	
	public Slope(BufferedImage png, AffineTransform af) {
		super(png, af);
	}
	
	public Slope(double[][] npy, AffineTransform af) {
		super(npy, af);
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
				cell[i][j].val=deg;
			}
		}
	}

	private double getSlopeVal(double[][] p,double dx,double dy){
		double sx=(p[0][0]+p[1][0]+p[2][0]-(p[0][2]+p[1][2]+p[2][2]))/(6*dx);
		double sy=(p[0][0]+p[0][1]+p[0][2]-(p[2][0]+p[2][1]+p[2][2]))/(6*dy);
		return Math.sqrt(sx*sx+sy*sy);
	}
}
