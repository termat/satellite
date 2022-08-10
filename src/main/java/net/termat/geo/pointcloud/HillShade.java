package net.termat.geo.pointcloud;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import net.termat.geo.satellite.BandReader;

public class HillShade extends AbstractTerrainRaster{
	private double zfactor=0.00001171;
	private double z_scale=100;
	private double azimush=-315;
	private double altitude=45;

	public HillShade(BandReader br,int channel) {
		super(br,channel);
	}
	
	public HillShade(BufferedImage png, AffineTransform af) {
		super(png, af);
	}
	
	public HillShade(double[][] npy, AffineTransform af) {
		super(npy, af);
	}

	public double getAzimush() {
		return azimush;
	}

	public void setAzimush(double azimush) {
		this.azimush = azimush;
	}

	public double getAltitude() {
		return altitude;
	}

	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}

	public double getZ_scale() {
		return z_scale;
	}

	public void setZ_scale(double z_scale) {
		this.z_scale = z_scale;
	}

	@Override
	public void create(int size) {
		createCell(size);
		double dx=Math.abs(af.getScaleX());
		double dy=Math.abs(af.getScaleY());
		for(int i=1;i<cell.length-1;i++){
			for(int j=1;j<cell[i].length-1;j++){
				double[][] px=getPixelDataCell(i,j,cell);
				int sv=getHillHsadeVal(px,dx,dy);
				if(sv<0)sv=0;if(sv>255)sv=255;
				cell[i][j].val=sv;;
			}
		}
	}

	public BufferedImage getImage() {
		double[][] data=this.getNPY();
		BufferedImage ret=new BufferedImage(data[0].length,data.length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<ret.getWidth();i++) {
			for(int j=0;j<ret.getHeight();j++) {
				double sv=data[j][i];
				if(sv<0)sv=0;if(sv>255)sv=255;
				ret.setRGB(i, j, new Color((int)sv,(int)sv,(int)sv).getRGB());
				
			}
		}
		return ret;
	}
	
	private int getHillHsadeVal(double[][] p,double dx,double dy){
		double zx=z_scale*(p[2][1]-p[0][1])/(2*dx);
		double zy=z_scale*(p[1][2]-p[1][0])/(2*dy);
		return hillshade(zx,zy,azimush,altitude);
	}

	private int hillshade(double zx,double zy,double azimush_deg,double altitude_deg){
		double slope_rad=Math.atan(500*zfactor*Math.sqrt(zx*zx+zy*zy));
		double aspect_rad=Math.atan2(-zx, zy);
		double azimush_rad=Math.toRadians(360-azimush_deg+90);
		double zenith_rad=Math.toRadians(90-altitude_deg);
		double shaded=Math.cos(zenith_rad)*Math.cos(slope_rad)+Math.sin(zenith_rad)*Math.sin(slope_rad)*Math.cos(azimush_rad-aspect_rad);
		return (int)(255*shaded);
	}

}
