package net.termat.geo.pointcloud;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import net.termat.geo.util.PCUtil;

public class ContourImage{
	private double[][] data;
	private double[] x;
	private double[] y;
	private BufferedImage img;
	private double max,min;
	private Color color;

	public ContourImage(BufferedImage png){
		data=PCUtil.getTable(png);
		max=0;
		x=new double[data.length];
		y=new double[data[0].length];
		for(int i=0;i<data.length;i++){
			x[i]=i;
			for(int j=0;j<data[i].length;j++){
				if(i==0)y[j]=j;
				if(Double.isNaN(data[i][j]))continue;
				max=Math.max(max, data[i][j]);
			}
		}
	}
	
	public ContourImage(double[][] npy){
		data=PCUtil.npyToArray(npy);
		max=0;
		max=0;
		x=new double[data.length];
		y=new double[data[0].length];
		for(int i=0;i<data.length;i++){
			x[i]=i;
			for(int j=0;j<data[i].length;j++){
				if(i==0)y[j]=j;
				if(Double.isNaN(data[i][j]))continue;
				max=Math.max(max, data[i][j]);
			}
		}
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void drawContour(int step,float lineWidth){
		List<Double> z=new ArrayList<Double>();
		double mm=min/step;
		for(int i=step*(int)mm;i<max;i=i+step){
			z.add((double)i);
		}
		Graphics2D g=img.createGraphics();
		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke(lineWidth));
		Double[] dd=z.toArray(new Double[z.size()]);
		drawContour(g,data,0,data.length-1,0,data[0].length-1,x,y,dd.length,dd);
	}

	public void setBaseImage(BufferedImage img){
		this.img=img;
	}

	public BufferedImage getImage(){
		return this.img;
	}
	private void drawContour(Graphics2D g,double [][] d, int ilb, int iub, int jlb, int jub, double [] x, double [] y, int nc, Double [] z) {
		double[] h=new double [5];
		int[] sh=new int[5];
		double[] xh=new double [5];
		double[] yh=new double [5];
		int m1;
		int m2;
		int m3;
		int case_value;
		double dmin;
		double dmax;
		double x1 = 0.0;
		double x2 = 0.0;
		double y1 = 0.0;
		double y2 = 0.0;
		int i,j,k,m;
		int[] im= {0,1,1,0};
		int[] jm= {0,0,1,1};
		int[][][] castab={
			{{0,0,8},{0,2,5},{7,6,9}},
			{{0,3,4},{1,3,1},{4,3,0}},
			{{9,6,7},{5,2,0},{8,0,0}}
		};
		for (j=(jub-1);j>=jlb;j--) {
			for (i=ilb;i<=iub-1;i++) {
				double temp1,temp2;
				temp1 = Math.min(d[i][j],d[i][j+1]);
				temp2 = Math.min(d[i+1][j],d[i+1][j+1]);
				dmin  = Math.min(temp1,temp2);
				temp1 = Math.max(d[i][j],d[i][j+1]);
				temp2 = Math.max(d[i+1][j],d[i+1][j+1]);
				dmax  = Math.max(temp1,temp2);
				if (dmax>=z[0]&&dmin<=z[nc-1]) {
					for (k=0;k<nc;k++) {
						if (z[k]>=dmin&&z[k]<=dmax) {
							for (m=4;m>=0;m--) {
								if (m>0) {
									h[m] = d[i+im[m-1]][j+jm[m-1]]-z[k];
									xh[m] = x[i+im[m-1]];
									yh[m] = y[j+jm[m-1]];
								} else {
									h[0] = 0.25*(h[1]+h[2]+h[3]+h[4]);
									xh[0]=0.5*(x[i]+x[i+1]);
									yh[0]=0.5*(y[j]+y[j+1]);
								}
								if (h[m]>0.0) {
									sh[m] = 1;
								} else if (h[m]<0.0) {
									sh[m] = -1;
								} else
									sh[m] = 0;
							}
							for (m=1;m<=4;m++) {
								m1 = m;
								m2 = 0;
								if (m!=4) {
									m3 = m+1;
								} else {
									m3 = 1;
								}
								case_value = castab[sh[m1]+1][sh[m2]+1][sh[m3]+1];
								if (case_value!=0) {
									switch (case_value) {
										case 1: // Line between vertices 1 and 2
											x1=xh[m1];
											y1=yh[m1];
											x2=xh[m2];
											y2=yh[m2];
											break;
										case 2: // Line between vertices 2 and 3
											x1=xh[m2];
											y1=yh[m2];
											x2=xh[m3];
											y2=yh[m3];
											break;
										case 3: // Line between vertices 3 and 1
											x1=xh[m3];
											y1=yh[m3];
											x2=xh[m1];
											y2=yh[m1];
											break;
										case 4: // Line between vertex 1 and side 2-3
											x1=xh[m1];
											y1=yh[m1];
											x2=xsect(h,xh,m2,m3);
											y2=ysect(h,yh,m2,m3);
											break;
										case 5: // Line between vertex 2 and side 3-1
											x1=xh[m2];
											y1=yh[m2];
											x2=xsect(h,xh,m3,m1);
											y2=ysect(h,yh,m3,m1);
											break;
										case 6: //  Line between vertex 3 and side 1-2
											x1=xh[m3];
											y1=yh[m3];
											x2=xsect(h,xh,m1,m2);
											y2=ysect(h,yh,m1,m2);
											break;
										case 7: // Line between sides 1-2 and 2-3
											x1=xsect(h,xh,m1,m2);
											y1=ysect(h,yh,m1,m2);
											x2=xsect(h,xh,m2,m3);
											y2=ysect(h,yh,m2,m3);
											break;
										case 8: // Line between sides 2-3 and 3-1
											x1=xsect(h,xh,m2,m3);
											y1=ysect(h,yh,m2,m3);
											x2=xsect(h,xh,m3,m1);
											y2=ysect(h,yh,m3,m1);
											break;
										case 9: // Line between sides 3-1 and 1-2
											x1=xsect(h,xh,m3,m1);
											y1=ysect(h,yh,m3,m1);
											x2=xsect(h,xh,m1,m2);
											y2=ysect(h,yh,m1,m2);
											break;
										default:
											break;
									}
									g.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
								}
							}
						}
					}
				}
			}
		}
	}

	private static double xsect(double[] h,double[] xh,int p1, int p2){
		return    (h[p2]*xh[p1]-h[p1]*xh[p2])/(h[p2]-h[p1]);
	}

	private static double ysect(double[] h,double[] yh,int p1, int p2){
		return (h[p2]*yh[p1]-h[p1]*yh[p2])/(h[p2]-h[p1]);
	}
}
