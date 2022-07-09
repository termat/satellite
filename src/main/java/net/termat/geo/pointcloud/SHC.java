package net.termat.geo.pointcloud;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import net.termat.components.gradient.Gradient;
import net.termat.components.gradient.GradientFactory;
import net.termat.components.gradient.Range;
import net.termat.geo.util.PCUtil;

public class SHC{
	private AffineTransform af;
	private double[][] dem;
	private double[][] curve;
	private double[][] std;
	private Cell[][] cell;
	private double dist=100;
	private double min,max;

	public SHC(BufferedImage png,AffineTransform a){
		dem=new double[png.getWidth()][png.getHeight()];
		this.af=a;
		for(int i=0;i<dem.length;i++){
			for(int j=0;j<dem[i].length;j++){
				dem[i][j]=PCUtil.getZ(png.getRGB(i, j));
			}
		}
	}

	
	public double getDist() {
		return dist;
	}

	public void setDist(double dist) {
		this.dist = dist;
	}

	private void calcCurve(int size,double dx,double dy){
		int ww=dem.length;
		int hh=dem[0].length;
		cell=new Cell[ww/size][hh/size];
		curve=new double[cell.length][cell[0].length];
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
		for(int i=1;i<cell.length-1;i++){
			for(int j=1;j<cell[i].length-1;j++){
				double[][] px=getPixelDataCell(i,j,cell);
				curve[i][j]=getCurveVal(px,dx,dy);
			}
		}
	}

	public double[][] getNPYStd(){
		double[][] ret=new double[dem[0].length][dem.length];
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				for(int xx : cell[i][j].x) {
					for(int yy : cell[i][j].y) {
						ret[yy][xx]=std[i][j];
					}
				}
			}
		}
		return ret;
	}

	private void createSHC(double dx,double dy,double dist){
		std=new double[cell.length][cell[0].length];
		min=Double.MAX_VALUE;
		max=-Double.MAX_VALUE;
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				std[i][j]=calcCellStd(cell[i][j],dx,dy,dist);
				if(!Double.isNaN(std[i][j]))min=Math.min(min,std[i][j]);
				if(!Double.isNaN(std[i][j]))max=Math.max(max,std[i][j]);
			}
		}
	}

	private double calcCellStd(Cell c,double dx,double dy,double dist){
			List<Double> list=new ArrayList<Double>();
			for(int i=0;i<cell.length;i++){
				for(int j=0;j<cell[i].length;j++){
					if(c.dist(cell[i][j], dx, dy)<=dist){
						list.add(curve[i][j]);
					}
				}
			}
			return std(list);
	}

	private double std(List<Double> ll){
		double n=ll.size();
		double ave=ave(ll);
		double ss=0;
		for(Double d : ll){
			ss +=Math.pow(d-ave,2);
		}
		ss=ss/n;
		return Math.sqrt(ss);
	}

	private double ave(List<Double> ll){
		double n=ll.size();
		double s=0;
		for(Double d : ll){
			s +=d;
		}
		return s/n;
	}

	private static double getCurveVal(double[][] p,double dx,double dy){
		double ex1=p[1][0]+p[1][2]-2*p[1][1];
		double ex2=p[0][1]+p[2][1]-2*p[1][1];
		return (ex1+ex2)/(dx*dy)*100;
	}

	protected double[][] getPixelDataCell(int x,int y,Cell[][] cell){
		double[][] ret=new double[3][3];
		for(int i=0;i<3;i++){
			for(int j=0;j<3;j++){
				ret[i][j]=cell[x+i-1][y+j-1].getH();
			}
		}
		return ret;
	}
	
	public void create(int size) {
		double d=af.getScaleX()*size;
		calcCurve(size,d,d);
		createSHC(af.getScaleX(), af.getScaleX(), dist);
	}
	
	public BufferedImage getImage() {
		Gradient grad=GradientFactory.createDefaultGradient();
		Range range=new Range(min,max);
		range.setStep(10);
		return createImage(grad,range);
	}
	
	public BufferedImage createImage(Gradient grad,Range range){
		BufferedImage ret=new BufferedImage(dem.length,dem[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				int col=grad.getColorByInt(range.getValue(std[i][j]));
				cell[i][j].setRGB(ret, col);
			}
		}
		return ret;
	}

	protected class Cell{
		int[] x;
		int[] y;
		double val=Double.NaN;

		double getH(){
			double n=0;
			double v=0;
			for(int i=0;i<x.length;i++){
				for(int j=0;j<y.length;j++){
					if(Double.isNaN(dem[x[i]][y[j]])||dem[x[i]][y[j]]<=0)continue;
					n++;
					v +=dem[x[i]][y[j]];
				}
			}
			if(n==0){
				return 0;

			}else{
				return v/n;
			}
		};

		double getMinH(){
			double min=Double.MAX_VALUE;
			for(int i=0;i<x.length;i++){
				for(int j=0;j<y.length;j++){
					if(Double.isNaN(dem[x[i]][y[j]])||dem[x[i]][y[j]]<=0)continue;
					min=Math.min(min, dem[x[i]][y[j]]);
				}
			}
			if(min==Double.MAX_VALUE){
				return 0;
			}else{
				return min;
			}
		}

		double getMaxH(){
			double max=-Double.MAX_VALUE;
			for(int i=0;i<x.length;i++){
				for(int j=0;j<y.length;j++){
					if(Double.isNaN(dem[x[i]][y[j]])||dem[x[i]][y[j]]<=0)continue;
					max=Math.max(max, dem[x[i]][y[j]]);
				}
			}
			if(max==Double.MAX_VALUE){
				return 0;
			}else{
				return max;
			}
		}

		void setRGB(BufferedImage img,int rgb){
			for(int i=0;i<x.length;i++){
				for(int j=0;j<y.length;j++){
					img.setRGB(x[i], y[j], rgb);
				}
			}
		}

		void setValue(double[][] data){
			for(int i=0;i<x.length;i++){
				for(int j=0;j<y.length;j++){
					data[x[i]][y[j]]=val;
				}
			}
		}

		public String toString(){
			StringBuffer buf=new StringBuffer();
			for(int i=0;i<x.length;i++){
				for(int j=0;j<y.length;j++){
					buf.append("["+x[i]+","+y[j]+"],");
				}
				buf.append("\n");
			}
			return buf.toString();
		}

		double dist(Cell cell,double dx,double dy){
			int x1=x[x.length/2];
			int y1=y[x.length/2];
			int x2=cell.x[cell.x.length/2];
			int y2=cell.y[cell.y.length/2];
			double xx=(x1-x2)*dx;
			double yy=(y1-y2)*dy;
			return Math.sqrt(xx*xx+yy*yy);
		}

		Point2D getCenter(){
			double xx=0;
			double yy=0;
			for(int i=0;i<x.length;i++)xx +=x[i];
			for(int i=0;i<y.length;i++)yy +=y[i];
			xx=xx/x.length;
			yy=yy/y.length;
			return new Point2D.Double(xx,yy);
		}
	}
}
