package net.termat.geo.pointcloud;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import net.termat.components.gradient.Gradient;
import net.termat.components.gradient.GradientFactory;
import net.termat.components.gradient.Range;

public class SPI extends AbstractTerrainRaster{

	private double[][] spi;
	
	/**
	 *
	 * @param dem 標高PNG
	 */
	public SPI(BufferedImage dem,AffineTransform af){
		super(dem,af);
	}
	
	public SPI(double[][] npy,AffineTransform af){
		super(npy,af);
	}

	@Override
	public void create(int size){
		int ww=dem.length;
		int hh=dem[0].length;
		cell=new Cell[ww/size][hh/size];
		spi=new double[cell.length][cell[0].length];
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
				Cell[] tc=new Cell[]{
						cell[i-1][j-1],cell[i][j-1],cell[i+1][j-1],
						cell[i-1][j],cell[i][j],cell[i+1][j],
						cell[i-1][j+1],cell[i][j+1],cell[i+1][j+1],
					};
				double mm=getMin(tc);
				double n=numMin(tc,mm);
				double val=1.0/n;
				for(int k=-1;k<=1;k++){
					for(int l=-1;l<=1;l++){
						if(cell[i+k][j+l].getH()==mm)spi[i+k][j+l] +=val;
					}
				}
			}
		}
	}
	
	public BufferedImage getImage() {
		Gradient grad=GradientFactory.createGradient(new Color[]{Color.WHITE,Color.BLUE});
		Range range=new Range(0,16);
		BufferedImage img=new BufferedImage(dem.length,dem[0].length,BufferedImage.TYPE_INT_RGB);
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				int rgb=grad.getColorByInt(range.getValue(spi[i][j]));
				cell[i][j].setRGB(img,rgb);
			}
		}
		return img;
	}
	
	public double[][] getData(){
		double[][] src=new double[dem.length][dem[0].length];
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				for(int xx : cell[i][j].x) {
					for(int yy :cell[i][j].y) {
						src[xx][yy]=spi[i][j];
					}
				}
			}
		}
		return src;
	}
	
	public double[][] getNPY(){
		double[][] ret=new double[dem[0].length][dem.length];
		for(int i=0;i<cell.length;i++){
			for(int j=0;j<cell[i].length;j++){
				for(int xx : cell[i][j].x) {
					for(int yy : cell[i][j].y) {
						ret[yy][xx]=spi[i][j];
					}
				}
			}
		}
		return ret;
	}

	private double numMin(Cell[] c,double val){
		double n=0;
		for(int i=0;i<c.length;i++){
			if(val==c[i].getH())n++;
		}
		return n;
	}

	private double getMin(Cell[] c){
		double mm=10e10;
		for(int i=0;i<c.length;i++){
			mm=Math.min(mm, c[i].getH());
		}
		return mm;
	}
}
