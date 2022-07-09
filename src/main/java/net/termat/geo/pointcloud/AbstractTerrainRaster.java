package net.termat.geo.pointcloud;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.termat.components.gradient.Gradient;
import net.termat.components.gradient.Range;
import net.termat.geo.util.PCUtil;

public abstract class AbstractTerrainRaster {
	protected AffineTransform af;
	protected double[][] dem;
	protected Cell[][] cell;

	public AbstractTerrainRaster(BufferedImage png,AffineTransform af){
		dem=new double[png.getWidth()][png.getHeight()];
		this.af=af;
		for(int i=0;i<dem.length;i++){
			for(int j=0;j<dem[i].length;j++){
				dem[i][j]=PCUtil.getZ(png.getRGB(i, j));
			}
		}
	}

	public AbstractTerrainRaster(double[][] npy,AffineTransform af){
		dem=PCUtil.npyToArray(npy);
		this.af=af;
	}
	
	abstract public void create(int size);

	protected void createCell(int size){
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
	}

	public double[] getMinMaxVal(){
		double min=Double.MAX_VALUE;
		double max=-Double.MAX_VALUE;
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				if(Double.isNaN(cell[i][j].val))continue;
				min=Math.min(min, cell[i][j].val);
				max=Math.max(max, cell[i][j].val);
			}
		}
		return new double[]{min,max};
	}

	public BufferedImage createImage(Gradient grad,Range range){
		BufferedImage ret=new BufferedImage(dem.length,dem[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				int col=grad.getColorByInt(range.getValue(cell[i][j].val));
				cell[i][j].setRGB(ret, col);
			}
		}
		return ret;
	}

	public void outputImage(File path,Gradient grad,Range range) throws IOException{
		BufferedImage bi=createImage(grad,range);
		if(path.getName().toLowerCase().endsWith(".png")){
			ImageIO.write(bi, "png", path);
		}else{
			ImageIO.write(bi, "jpg", path);
		}
	}

	public void outputTransform(File path) throws IOException{
		PCUtil.writeTransform(af, path);
	}

	public double[][] getData(){
		double[][] src=new double[dem.length][dem[0].length];
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				cell[i][j].setValue(src);
			}
		}
		return src;
	}
	
	public double[][] getNPY(){
		double[][] src=new double[dem.length][dem[0].length];
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				cell[i][j].setValue(src);
			}
		}
		double[][] npy=new double[dem[0].length][dem.length];
		for(int i=0;i<src.length;i++){
			for(int j=0;j<src[i].length;j++){
				npy[j][i]=src[i][j];
			}
		}
		return npy;
	}
	
	public void outputNpy(File path){
		double[][] npy=getNPY();
		PCUtil.writeNpy(npy, path);
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
