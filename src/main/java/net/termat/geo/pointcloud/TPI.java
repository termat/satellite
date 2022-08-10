package net.termat.geo.pointcloud;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.termat.components.gradient.Gradient;
import net.termat.components.gradient.GradientFactory;
import net.termat.components.gradient.Range;
import net.termat.geo.satellite.BandReader;

public class TPI extends AbstractTerrainRaster{

	private double[][] tpi;
	private double stdv;
	

	public TPI(BandReader br,int channel) {
		super(br,channel);
	}
	
	public TPI(BufferedImage dem,AffineTransform af){
		super(dem,af);
	}
	
	public TPI(double[][] npy,AffineTransform af){
		super(npy,af);
	}

	@Override
	public void create(int size){
		int ww=dem.length;
		int hh=dem[0].length;
		cell=new Cell[ww/size][hh/size];
		int x=0;
		int y=0;
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				cell[i][j]=new Cell();
				cell[i][j].x=new int[size];
				cell[i][j].y=new int[size];
				for(int m=0;m<size;m++){
					cell[i][j].y[m]=y;
					y=(y+1)%hh;
				}
				for(int m=0;m<size;m++){
					cell[i][j].x[m]=x+m;
				}
				if(y==0)x=x+size;
			}
		}
		tpi=new double[cell.length][cell[0].length];
		List<Double> tmp=new ArrayList<Double>();
		for(int i=1;i<cell.length-1;i++){
			for(int j=1;j<cell[i].length-1;j++){
				double mm=getMean(new Cell[]{
					cell[i-1][j-1],cell[i][j-1],cell[i+1][j-1],
					cell[i-1][j],cell[i][j],cell[i+1][j],
					cell[i-1][j+1],cell[i][j+1],cell[i+1][j+1],
				});
				tpi[i][j]=cell[i][j].getH()-mm;
				if(Double.isNaN(tpi[i][j]))continue;
				tmp.add(tpi[i][j]);
			}
		}
		stdv=Math.abs(calStd(tmp));
	}
	
	private double calStd(List<Double> li) {
		double sum=0;
		double vars=0;
		for(double d : li)sum +=d;
		double ave = sum/li.size();
		for(double d : li)vars +=(d-ave)*(d-ave);
		return Math.sqrt(vars/li.size());
	}

	public BufferedImage getImage() {
		Gradient grad=GradientFactory.createGradient(new Color[]{Color.BLACK,Color.WHITE});
		Range range=new Range(-stdv*2,stdv*2);
		BufferedImage img=new BufferedImage(dem.length,dem[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				int rgb=grad.getColorByInt(range.getValue(tpi[i][j]));
				cell[i][j].setRGB(img,rgb);
			}
		}
		return img;
	}
	
	public double[][] getNPY(){
		double[][] ret=new double[dem[0].length][dem.length];
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				for(int xx : cell[i][j].x) {
					for(int yy : cell[i][j].y) {
						ret[yy][xx]=tpi[i][j];
					}
				}
			}
		}
		return ret;
	}

	private double getMean(Cell[] c){
		double[] tmp=new double[c.length];
		for(int i=0;i<tmp.length;i++){
			tmp[i]=c[i].getH();
		}
		Arrays.sort(tmp);
		int n=tmp.length/2;
		return tmp[n];
	}
}
