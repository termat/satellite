package net.termat.geo.pointcloud;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import net.termat.components.gradient.Gradient;
import net.termat.components.gradient.Range;

public class GroundOpenning extends AbstractTerrainRaster {
	private double[][] above;
	private double[][] downward;
	private double radius=20;
	private int cell_width;
	private int cell_height;
	private double dx,dy;

	public GroundOpenning(BufferedImage png, AffineTransform af) {
		super(png, af);
	}
	
	public GroundOpenning(double[][] npy, AffineTransform af) {
		super(npy, af);
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	@Override
	public void create(int size) {
		createCell(size);
		cell_width=cell.length;
		cell_height=cell[0].length;
		above=new double[cell.length][cell[0].length];
		downward=new double[cell.length][cell[0].length];
		dx=Math.abs(af.getScaleX())*size;
		dy=Math.abs(af.getScaleY())*size;
		calc(radius);
	}

	public void calc(double radius){
		for(int i=0;i<cell.length;i++){
			if(i%50==0)System.out.println(i);
			for(int j=0;j<cell[i].length;j++){
				List<Point[]> pos=searchCells(i,j,radius);
				double[][] minmax=getMinMaxAtan(pos,cell[i][j]);
				double ave[]=ave(minmax);
				above[i][j]=ave[0];
				downward[i][j]=ave[1];
			}
		}
	}

	public double[] minmaxAbove(){
		return minmax(above);
	}

	public double[] minmaxDown(){
		return minmax(downward);
	}

	public double[][] getNPYAbove() {
		double[][] ret=new double[dem[0].length][dem.length];
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				for(int xx : cell[i][j].x) {
					for(int yy : cell[i][j].y) {
						ret[yy][xx]=above[i][j];
					}
				}
			}
		}
		return ret;
	}
	
	public double[][] getNPYDoownward() {
		double[][] ret=new double[dem[0].length][dem.length];
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				for(int xx : cell[i][j].x) {
					for(int yy : cell[i][j].y) {
						ret[yy][xx]=downward[i][j];
					}
				}
			}
		}
		return ret;
	}
	
	public BufferedImage createImageAbove(Gradient grad,Range range){
		return outImage(dem,cell,above,grad,range);
	}

	public BufferedImage createImageDownward(Gradient grad,Range range){
		return outImage(dem,cell,downward,grad,range);
	}

	private static BufferedImage outImage(double[][] dem,Cell[][] cell,double[][] data,Gradient grad,Range range){
		BufferedImage img=new BufferedImage(dem.length,dem[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<data.length;i++){
			for(int j=0;j<data[i].length;j++){
				int  c=grad.getColorByInt(range.getValue(data[i][j]));
				cell[i][j].setRGB(img, c);
			}
		}
		return img;
	}

	private static double[] minmax(double[][] val){
		double min=Double.MAX_VALUE;
		double max=-Double.MAX_VALUE;
		for(int i=0;i<val.length;i++){
			for(int j=0;j<val[i].length;j++){
				min=Math.min(min, val[i][j]);
				max=Math.max(max, val[i][j]);

			}
		}
		return new double[]{min,max};
	}

	private List<Point[]> searchCells(int x,int y,double radius){
		int ix=(int)Math.ceil(radius/dx);
		int iy=(int)Math.ceil(radius/dy);
		List<Point[]> list=new ArrayList<Point[]>();
		List<Point> tmp=new ArrayList<Point>();
		//W
		for(int i=x-ix;i<x;i++){
			if(i<0)continue;
			tmp.add(new Point(i,y));
		}
		list.add(tmp.toArray(new Point[tmp.size()]));
		tmp.clear();
		//E
		for(int i=x+1;i<=x+ix&&i<cell_width;i++){
			tmp.add(new Point(i,y));
		}
		list.add(tmp.toArray(new Point[tmp.size()]));
		tmp.clear();
		//N
		for(int i=y-iy;i<y;i++){
			if(i<0)continue;
			tmp.add(new Point(x,i));
		}
		list.add(tmp.toArray(new Point[tmp.size()]));
		tmp.clear();
		//S
		for(int i=y+1;i<=y+iy&&i<cell_height;i++){
			tmp.add(new Point(x,i));
		}
		list.add(tmp.toArray(new Point[tmp.size()]));
		tmp.clear();
		//NW
		for(int i=x-ix;i<x;i++){
			if(i<0)continue;
			for(int j=y-iy;j<y;j++){
				if(j<0)continue;
				tmp.add(new Point(i,j));
			}
		}
		list.add(tmp.toArray(new Point[tmp.size()]));
		tmp.clear();
		//NE
		for(int i=x+1;i<=x+ix&&i<cell_width;i++){
			for(int j=y-iy;j<y;j++){
				if(j<0)continue;
				tmp.add(new Point(i,j));
			}
		}
		list.add(tmp.toArray(new Point[tmp.size()]));
		tmp.clear();
		//SW
		for(int i=x-ix;i<x;i++){
			if(i<0)continue;
			for(int j=y+1;j<=y+iy&&j<cell_height;j++){
				tmp.add(new Point(i,j));
			}
		}
		list.add(tmp.toArray(new Point[tmp.size()]));
		tmp.clear();
		//SE
		for(int i=x+1;i<=x+ix&&i<cell_width;i++){
			if(i<0)continue;
			for(int j=y+1;j<=y+iy&&j<cell_height;j++){
				tmp.add(new Point(i,j));
			}
		}
		list.add(tmp.toArray(new Point[tmp.size()]));
		return list;
	}

	private double[][] getMinMaxAtan(List<Point[]> list,Cell c){
		List<double[]> tmp=new ArrayList<double[]>();
		for(Point[] ps : list){
			if(ps.length==0){
				tmp.add(new double[]{0.0,0.0});
			}else{
				double max=-Double.MAX_VALUE;
				double min=Double.MAX_VALUE;
				for(int i=0;i<ps.length;i++){
					Point p=ps[i];
					Cell cx=cell[p.x][p.y];
					double dist=cx.dist(c, dx, dy);
					if(dist<0.1)continue;
					double hh=cx.getH()-c.getH();
					double val=Math.atan(hh/dist);
					max=Math.max(max, val);
					min=Math.min(min, val);
				}
				double uv=2.0/Math.PI-max;
				double dv=2.0/Math.PI+min;
				tmp.add(new double[]{uv,dv});
			}

		}
		return tmp.toArray(new double[tmp.size()][]);
	}

	private double[] ave(double[][] val){
		double p1=0.0;
		double p2=0.0;
		double len=0.0;
		for(double[] v : val){
			p1 +=v[0];
			p2 +=v[1];
			len++;
		}
		if(len==0){
			return new double[]{0.0,0.0};
		}else{
			return new double[]{p1/len,p2/len};
		}
	}
}
