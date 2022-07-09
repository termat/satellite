package net.termat.geo.pointcloud;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import net.termat.geo.util.LineSimpliyfy;
import net.termat.geo.util.PCUtil;

public class ContourLine2 implements GeojsonData{
	private double[][] data;
	private double[] x;
	private double[] y;
	private double max,min;
	private Map<Double,List<Line2D>> map;
	private Double[] zList;
	private Map<String,Object> root;
	private AffineTransform af;
	private int limit=5;
	private Rectangle2D rect;
	
	public ContourLine2(BufferedImage png,AffineTransform af,int step){
		this.af=af;
		data=new double[png.getWidth()][png.getHeight()];
		for(int i=0;i<data.length;i++) {
			for(int j=0;j<data[i].length;j++) {
				data[i][j]=PCUtil.getZ(png.getRGB(i, j));
			}
		}
		max=-Double.MAX_VALUE;
		min=Double.MAX_VALUE;
		x=new double[data.length];
		y=new double[data[0].length];
		for(int i=0;i<data.length;i++){
			x[i]=i;
			for(int j=0;j<data[i].length;j++){
				if(i==0)y[j]=j;
				if(Double.isNaN(data[i][j]))continue;
				max=Math.max(max, data[i][j]);
				min=Math.min(min, data[i][j]);
			}
		}
		min=Math.ceil(min/step)*step;
		max=Math.floor(max/step)*step;
		List<Double> tmp=new ArrayList<Double>();
		for(double i=min;i<=max;i=i+step){
			tmp.add(i);
		}
		zList=tmp.toArray(new Double[tmp.size()]);
		map=new HashMap<Double,List<Line2D>>();
		rect=af.createTransformedShape(new Rectangle2D.Double(0,0,png.getWidth(),png.getHeight())).getBounds2D();
	}

	public void setName(String name){
		root.put("name", name);
	}

	public void setCoordSys(int coodId){
		Gson gson=new Gson();
		String crs="{ 'type': 'name', 'properties': { 'name': 'urn:ogc:def:crs:EPSG::"+Integer.toString(coodId+6668)+"'}}";
		root.put("crs",gson.fromJson(crs, Map.class));
	}
	
	@Override
	public String getGeojson() {
		Gson gson=new Gson();
		return gson.toJson(root);
	}

	public void create(){
		drawContour(data,0,data.length-1,0,data[0].length-1,x,y,zList.length,zList);
		root=new HashMap<String,Object>();
		root.put("type","FeatureCollection");
		List<Map<String,Object>> feas=new ArrayList<Map<String,Object>>();
		root.put("features",feas);
		for(int i=0;i<zList.length;i++){
			List<Line2D> ll=map.get(zList[i]);
			List<List<Point2D>> val=connectLine(ll.toArray(new Line2D[ll.size()]));
			Map<String,Object> f=new HashMap<String,Object>();
			f.put("type", "Feature");
			Map<String,Object> p=new HashMap<String,Object>();
			p.put("z", zList[i]);
			f.put("properties", p);
			Map<String,Object> g=new HashMap<String,Object>();
			f.put("geometry", g);
			g.put("type","MultiLineString");
			List<List<double[]>> coord=new ArrayList<List<double[]>>();
			for(List<Point2D> pt : val){
				if(pt.size()<limit)continue;
				coord.add(p2Array(pt));
			}
			g.put("coordinates", coord);
			feas.add(f);
		}
	}

	private List<double[]> p2Array(List<Point2D> ll){
		LineSimpliyfy sl=new LineSimpliyfy();
		ll=sl.simplify(ll, 1.0, true);
		List<double[]> ret=new ArrayList<double[]>();
		for(Point2D p :ll){
			Point2D pt=af.transform(p, new Point2D.Double());
			ret.add(new double[]{pt.getX(),pt.getY()});
		}
		return ret;
	}

	private List<List<Point2D>> connectLine(Line2D[] ll){
		List<List<Point2D>> ret=new ArrayList<List<Point2D>>();
		for(int i=0;i<ll.length;i++){
			if(ll[i]==null)continue;
			List<Point2D> tmp=new LinkedList<Point2D>();
			tmp.add(ll[i].getP1());
			tmp.add(ll[i].getP2());
			ll[i]=null;
			for(int j=i+1;j<ll.length;j++){
				if(ll[j]==null)continue;
				Point2D p1=ll[j].getP1();
				Point2D p2=ll[j].getP2();
				if(tmp.get(0).equals(p1)){
					tmp.add(0, p2);
				}else if(tmp.get(0).equals(p2)){
					tmp.add(0,p1);
				}else if(tmp.get(tmp.size()-1).equals(p1)){
					tmp.add(p2);
				}else if(tmp.get(tmp.size()-1).equals(p2)){
					tmp.add(p1);
				}
			}
			ret.add(tmp);
		}
		List<List<Point2D>> ret2=new ArrayList<List<Point2D>>();
		for(int i=0;i<ret.size();i++){
			List<Point2D> t1=ret.get(i);
			for(int j=i+1;j<ret.size();j++){
				List<Point2D> t2=ret.get(j);
				if(t1.get(0).equals(t2.get(0))){
					t2.remove(0);
					while(t2.size()>0){
						Point2D o=t2.remove(0);
						t1.add(0,o);
					}
					ret.remove(t2);
				}else if(t1.get(0).equals(t2.get(t2.size()-1))){
					List<Point2D> tp=new LinkedList<Point2D>();
					tp.addAll(t2);
					t1.remove(0);
					tp.addAll(t1);
					t1=tp;
					ret.remove(t2);
				}else if(t1.get(t1.size()-1).equals(t2.get(0))){
					t2.remove(0);
					t1.addAll(t2);
					ret.remove(t2);
				}else if(t1.get(t1.size()-1).equals(t2.get(t2.size()-1))){
					t2.remove(t2.size()-1);
					while(t2.size()>0){
						t1.add(t2.remove(t2.size()-1));
					}
					ret.remove(t2);
				}
			}
			ret2.add(t1);
		}
		return ret2;
	}

	/* Conrec */
	private void drawContour(double [][] d, int ilb, int iub, int jlb, int jub, double [] x, double [] y, int nc, Double[] z) {
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
									Line2D l=new Line2D.Float((int)x1, (int)y1, (int)x2, (int)y2);
									if(map.containsKey(z[k])){
										map.get(z[k]).add(l);
									}else{
										List<Line2D> ll=new ArrayList<Line2D>();
										ll.add(l);
										map.put(z[k],ll);
									}
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

	@Override
	public Rectangle2D getBounds() {
		return rect;
	}
}
