package net.termat.geo.satellite;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.gdal.gdal.GCP;

public class GCPMapper {
	private Integer[] line;
	private Integer[] pixel;
	private double[][][][] cof;
	private Rectangle2D rect;
	
	public GCPMapper(Vector<GCP> vec) {
		Map<Integer,Map<Integer,double[]>> map=new HashMap<>();
		Set<Integer> li=new HashSet<>();
		Set<Integer> pi=new HashSet<>();
		for(GCP g : vec) {
			li.add((int)g.getGCPLine());
			pi.add((int)g.getGCPPixel());
			int ll=(int)g.getGCPLine();
			if(map.containsKey(ll)) {
				Map<Integer,double[]> tmp=map.get(ll);
				tmp.put((int)g.getGCPPixel(), new double[] {g.getGCPX(), g.getGCPY()});
			}else {
				Map<Integer,double[]> tmp=new HashMap<>();
				tmp.put((int)g.getGCPPixel(), new double[] {g.getGCPX(), g.getGCPY()});
				map.put(ll, tmp);
			}
			if(rect==null) {
				rect=new Rectangle2D.Double(g.getGCPX(),g.getGCPY(),0,0);
			}else {
				rect.add(new Point2D.Double(g.getGCPX(),g.getGCPY()));
			}
			
		}
		line=li.toArray(new Integer[li.size()]);
		pixel=pi.toArray(new Integer[pi.size()]); 
		Arrays.sort(line);
		Arrays.sort(pixel);
		cof=new double[line.length][pixel.length][][];
		for(int i=1;i<line.length;i++) {
			for(int j=1;j<pixel.length;j++) {
				int y1=line[i-1];
				int y2=line[i];
				int x1=pixel[j-1];
				int x2=pixel[j];
				double[][] pt=new double[][] {
					new double[] {x1,y1,x1*y1,1},
					new double[] {x2,y1,x2*y1,1},
					new double[] {x1,y2,x1*y2,1},
					new double[] {x2,y2,x2*y2,1}
				};
				double[] p1=map.get(y1).get(x1);
				double[] p2=map.get(y1).get(x2);
				double[] p3=map.get(y2).get(x1);
				double[] p4=map.get(y2).get(x2);
				double[] x=new double[] {p1[0],p2[0],p3[0],p4[0]};
				double[] y=new double[] {p1[1],p2[1],p3[1],p4[1]};
				double[] xc=solve(pt,x);
				double[] yc=solve(pt,y);
				cof[i][j]=new double[][] {xc,yc};
			}
		}
	}
	
	public Rectangle2D getBounds() {
		/*
		double[] p1=getXY(pixel[0],line[0]);
		double[] p2=getXY(pixel[0],line[line.length-1]);
		double[] p3=getXY(pixel[pixel.length-1],line[0]);
		double[] p4=getXY(pixel[pixel.length-1],line[line.length-1]);
		Rectangle2D rect=new Rectangle2D.Double(p1[0],p1[1],0,0);
		rect.add(p2[0], p2[1]);
		rect.add(p3[0], p3[1]);
		rect.add(p4[0], p4[1]);
		*/
		return rect;
	}
	
	public double[] getXY(int x,int y) {
		int i=1;
		for(;i<line.length;i++) {
			if(line[i-1]<=y&&line[i]>=y)break;
		}
		int j=1;
		for(;j<pixel.length;j++) {
			if(pixel[j-1]<=x&&pixel[j]>=x)break;
		}
		double[][] cc=cof[i][j];
		double px=cc[0][0]*x+cc[0][1]*y+cc[0][2]*x*y+cc[0][3];
		double py=cc[1][0]*x+cc[1][1]*y+cc[1][2]*x*y+cc[1][3];
		return new double[] {px,py};
	}

	private double[] solve(double[][] square,double[] val) {
		RealMatrix matrix = MatrixUtils.createRealMatrix(square);
		SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
		DecompositionSolver ds=svd.getSolver();
		RealVector vector = MatrixUtils.createRealVector(val);
		RealVector ret=ds.solve(vector);
		return new double[] {ret.getEntry(0),ret.getEntry(1),ret.getEntry(2),ret.getEntry(3)};
	}
}
