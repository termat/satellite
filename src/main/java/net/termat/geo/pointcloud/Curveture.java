package net.termat.geo.pointcloud;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import net.termat.geo.satellite.BandReader;


public class Curveture extends AbstractTerrainRaster{
	public enum Type{VERTICAL,HORIZONTAL}
	private Type type;

	public Curveture(BandReader br,int channel,Type type) {
		super(br,channel);
		this.type=type;
	}
	
	
	public Curveture(BufferedImage png,AffineTransform af,Type type) {
		super(png,af);
		this.type=type;
	}
	
	public Curveture(double[][] npy,AffineTransform af,Type type) {
		super(npy,af);
		this.type=type;
	}

	@Override
	public void create(int size) {
		createCell(size);
		double dx=Math.abs(af.getScaleX());
		double dy=Math.abs(af.getScaleY());
		for(int i=1;i<cell.length-1;i++){
			for(int j=1;j<cell[i].length-1;j++){
				double[][] px=getPixelDataCell(i,j,cell);
				cell[i][j].val=getCurveVal(px,dx*size,dy*size,type);
			}
		}
	}

	private static double getCurveVal(double[][] p,double dx,double dy,Type type){
		double scale=1.0;
		double z1=p[0][0];
		double z2=p[0][1];
		double z3=p[0][2];
		double z4=p[1][0];
		double z5=p[1][1];
		double z6=p[1][2];
		double z7=p[2][0];
		double z8=p[2][1];
		double z9=p[2][2];
		double a=(z1+z3+z4+z6+z7+z9)/(6*dx*dx)-(z2+z5+z8)/(3*dx*dx);
		double b=(z1+z2+z3+z7+z8+z9)/(6*dy*dy)-(z4+z5+z6)/(3*dy*dy);
		double c=(z3+z7-z1-z9)/(4*dx*dy);
		double d=(z3+z6+z9-z1-z4-z7)/(6*dx);
		double ee=(z1+z2+z3-z7-z8-z9)/(6*dy);
//		double f=(2*(z2+z4+z6+z8)-(z1+z3+z7+z9)+(5* z5))/9;
		if(type==Type.VERTICAL){
			double prfdenom = Math.round(Math.pow(10, 7)*((Math.sqrt(ee) + Math.sqrt(d))*Math.pow((1 + Math.sqrt(d) + Math.sqrt(ee)),1.5)))*Math.pow(10,-7);
			if(prfdenom==0){
				if((a>0&&b>0)||(a<0&&b<0)){
					return -(a+b)*scale;
				}else{
					return 0.0;
				}
			}else{
				double profile=scale*-2*(a*Math.sqrt(d)+b*Math.sqrt(ee)+c*d*ee)/prfdenom;	//垂直曲率
				return profile;
			}
		}else{
			double plndenom = Math.round(Math.pow(10,7)*Math.pow(Math.sqrt(ee) + Math.sqrt(d),1.5))*Math.pow(10,-7);
			if(plndenom==0){
				if((a>0&&b>0)||(a<0&&b<0)){
					return -(a+b)*scale;
				}else{
					return 0.0;
				}
			}else{
				double plan=scale*-2*(b*Math.sqrt(d)+a*Math.sqrt(ee)-c*d*ee)/plndenom;		//平面曲率
				return plan;
			}
		}
	}
}
