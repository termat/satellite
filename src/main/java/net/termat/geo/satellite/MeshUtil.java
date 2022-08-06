package net.termat.geo.satellite;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Polygon;

public class MeshUtil {

	public enum BasicMeshType{S50000,S5000,S2500,S1000,S500};
	public enum StdMeshType{FIRST,SECOND,THRED};
	
	private static String[] ALP=new String[] {"a","b","c","d","e","f","g","h","i","j","k",
			"l","m","n","o","p","q","r","s","t"};
	private static final double L=85.05112877980659;
	

	public static Point2D lonlatToPixel(int zoom,double lon,double lat){
		double x=(Math.pow(2, zoom+7)*(lon/180.0+1.0));
		double y=((Math.pow(2, zoom+7)/Math.PI)*(-atanh(Math.sin(Math.toRadians(lat)))+atanh(Math.sin(Math.toRadians(L)))));
		return new Point2D.Double(x,y);
	}
	
	public static Point lonlatToTile(int zoom,double lon,double lat){
		Point2D pixel=lonlatToPixel(zoom,lon,lat);
		Point tile=new Point((int)Math.floor(pixel.getX()/256.0),(int)Math.floor(pixel.getY()/256.0));
		return tile;
	}
	
	public static List<Point> getTileList(Rectangle2D rect,int zoom){
		Point p1=lonlatToTile(zoom,rect.getX(),rect.getY());
		Point p2=lonlatToTile(zoom,rect.getX()+rect.getWidth(),rect.getY()+rect.getHeight());
		int xmin=(int)Math.min(p1.getX(), p2.getX());
		int xmax=(int)Math.max(p1.getX(), p2.getX());
		int ymin=(int)Math.min(p1.getY(), p2.getY());
		int ymax=(int)Math.max(p1.getY(), p2.getY());
		List<Point> ret=new ArrayList<>();
		for(int i=xmin;i<=xmax;i++) {
			for(int j=ymin;j<=ymax;j++) {
				ret.add(new Point(i,j));
			}
		}
		return ret;
	}
	
	public static Rectangle2D getTileBounds(int zoom,int tileX,int tileY) {
		double px=tileX*256;
		double py=tileY*256;
		double lon1=180*(px/Math.pow(2, zoom+7)-1);
		double lat1=(180.0/Math.PI)*Math.asin(Math.tanh(-(Math.PI/Math.pow(2,zoom+7))*py+atanh(Math.sin(Math.PI/180*L))));
		double lon2=180*((px+256)/Math.pow(2, zoom+7)-1);
		double lat2=(180.0/Math.PI)*Math.asin(Math.tanh(-(Math.PI/Math.pow(2,zoom+7))*(py+256)+atanh(Math.sin(Math.PI/180*L))));
		double xmin=Math.min(lon1,lon2);
		double xmax=Math.max(lon1, lon2);
		double ymin=Math.min(lat1,lat2);
		double ymax=Math.max(lat1, lat2);
		return new Rectangle2D.Double(xmin,ymin,xmax-xmin,ymax-ymin);
	}

	private static double atanh(double v){
		return 0.5*Math.log((1.0+v)/(1.0-v));
	}
	
	public static String rect2Geojson(Rectangle2D r) {
		Polygon p=createPolygon(r);
		Feature ff=Feature.fromGeometry(p);
		return FeatureCollection.fromFeature(ff).toJson();
	}
	
	public static String createGeoJson(String[] str) {
		List<Feature> fs=new ArrayList<>();
		if(isStdMesh(str[0])) {
			for(String ss : str) {
				Rectangle2D r=MeshUtil.getStdMeshBounds(ss);
				Polygon p=createPolygon(r);
				JsonObject o=new JsonObject();
				o.addProperty("name", ss);
				fs.add(Feature.fromGeometry(p, o));
			}
		}else {
			for(String ss : str) {
				Rectangle2D r=MeshUtil.getBasicMeshBounds(ss);
				Polygon p=createPolygon(r);
				JsonObject o=new JsonObject();
				o.addProperty("name", ss);
				fs.add(Feature.fromGeometry(p, o));
			}
		}
		FeatureCollection fc=FeatureCollection.fromFeatures(fs);
		return fc.toJson();
	};
	
	private static Polygon createPolygon(Rectangle2D r) {
		List<com.mapbox.geojson.Point> p1=new ArrayList<>();
		p1.add(com.mapbox.geojson.Point.fromLngLat(r.getX(), r.getY()));
		p1.add(com.mapbox.geojson.Point.fromLngLat(r.getX()+r.getWidth(), r.getY()));
		p1.add(com.mapbox.geojson.Point.fromLngLat(r.getX()+r.getWidth(), r.getY()+r.getHeight()));
		p1.add(com.mapbox.geojson.Point.fromLngLat(r.getX(), r.getY()+r.getHeight()));
		p1.add(com.mapbox.geojson.Point.fromLngLat(r.getX(), r.getY()));
		List<List<com.mapbox.geojson.Point>> ll=new ArrayList<>();
		ll.add(p1);
		return Polygon.fromLngLats(ll);
	}
	
	private static boolean isStdMesh(String str) {
		for(int i=0;i<str.length();i++) {
			if(Character.isDigit(str.charAt(i))){
				continue;
			}else {
				return false;
			}
		}
		return true;
	}
	
	public static String[] getStdMeshList(Rectangle2D rect,StdMeshType type){
		double lat=0;
		double lon=0;
		switch(type){
			case FIRST:
				lat=5.0/60.0;
				lon=0.125;
				break;
			case SECOND:
				lat=5.0/60.0;
				lon=0.125;
				break;
			case THRED:
				lat=30.0/3600.0;
				lon=45.0/3600.0;
				break;
		}
		Set<String> ret=new HashSet<>();
		for(double x=rect.getX();x<rect.getX()+rect.getWidth();x=x+lon) {
			for(double y=rect.getY();y<rect.getY()+rect.getHeight();y=y+lat) {
				switch(type){
					case FIRST:
						ret.add(getStdMeshName1st(x,y));
						break;
					case SECOND:
						ret.add(getStdMeshName2nd(x,y));
						break;
					case THRED:
						ret.add(getStdMeshName3rd(x,y));
						break;
				}
			}
		}
		String[] ss=ret.toArray(new String[ret.size()]);
		Arrays.sort(ss);
		return ss;
	}
	
	public static Rectangle2D getStdMeshBounds(String name) {
		int n=name.length();
		if(n==4) {
			return getStdMeshBounds1st(name);
		}else if(n==6) {
			return getStdMeshBounds2nd(name);
		}else if(n==8){
			return getStdMeshBounds3rd(name);
		}else {
			return null;
		}
	}
	
	private static String getStdMeshName1st(double lon,double lat) {
		int la=(int)Math.floor(lat*1.5);
		int lo=(int)Math.floor(lon-100);
		return Integer.toString(la)+Integer.toString(lo);
	}
	
	private static String getStdMeshName2nd(double lon,double lat) {
		double ty1=lat*60/40;
		double tx1=lon-100;
		int la1=(int)Math.floor(ty1);
		int lo1=(int)Math.floor(tx1);
		double ty2=(ty1-Math.floor(ty1))*40/5;
		double tx2=(tx1-Math.floor(tx1))*60/7.5;
		int la2=(int)Math.floor(ty2);
		int lo2=(int)Math.floor(tx2);
		return Integer.toString(la1)+Integer.toString(lo1)+Integer.toString(la2)+Integer.toString(lo2);
	}
	
	private static String getStdMeshName3rd(double lon,double lat) {
		double ty1=lat*60/40;
		double tx1=lon-100;
		int la1=(int)Math.floor(ty1);
		int lo1=(int)Math.floor(tx1);
		double ty2=(ty1-Math.floor(ty1))*40/5;
		double tx2=(tx1-Math.floor(tx1))*60/7.5;
		int la2=(int)Math.floor(ty2);
		int lo2=(int)Math.floor(tx2);
		double ty3=(ty2-Math.floor(ty2))*5;
		double tx3=(tx2-Math.floor(tx2))*7.5;
		int la3=(int)Math.floor(ty3*60/30);
		int lo3=(int)Math.floor(tx3*60/45);
		return Integer.toString(la1)+Integer.toString(lo1)+Integer.toString(la2)+Integer.toString(lo2)
			+Integer.toString(la3)+Integer.toString(lo3);
	}
	
	public static String[] getBasicMeshList(int coord,Rectangle2D rect,BasicMeshType type){
		double lat=0;
		double lon=0;
		switch(type){
			case S50000:
				lat=30000;
				lon=40000;
				break;
			case S5000:
				lat=3000;
				lon=4000;
				break;
			case S2500:
				lat=1500;
				lon=2000;
				break;
			case S1000:
				lat=600;
				lon=800;
				break;
			case S500:
				lat=300;
				lon=400;
				break;
		}
		Set<String> ret=new HashSet<>();
		double xw=Math.ceil(rect.getWidth()/lon)*lon;
		double yw=Math.ceil(rect.getHeight()/lat)*lat;
		for(double x=rect.getX();x<=rect.getX()+xw;x=x+lon) {
			for(double y=rect.getY();y<=rect.getY()+yw;y=y+lat) {
				String zone=getBasicMeshNameByXY(coord,x,y,type);
				if(zone!=null) {
					Rectangle2D r=MeshUtil.getBasicMeshBounds(zone);
					if(rect.intersects(r))ret.add(zone.toUpperCase());
				}
			}
		}
		String[] ss=ret.toArray(new String[ret.size()]);
		Arrays.sort(ss);
		return ss;
	}
	
	public static Rectangle2D getBasicMeshBounds(String name) {
		int n=name.length();
		double[] p1=getCoord50000(name);
		if(n==4)return new Rectangle2D.Double(p1[0],p1[1]-30000,40000,30000);
		double[] p2=getCoord5000(name);
		if(n==6)return new Rectangle2D.Double(p1[0]+p2[0],p1[1]-p2[1]-3000,4000,3000);
		if(n==7) {
			double[] p3=getCoord2500(name);
			return new Rectangle2D.Double(p1[0]+p2[0]+p3[0],p1[1]-p2[1]-1500-p3[1],2000,1500);
		}else if(n==8){
			if(!Character.isDigit(name.charAt(7))) {
				double[] p4=getCoord1000(name);
				return new Rectangle2D.Double(p1[0]+p2[0]+p4[0],p1[1]-p2[1]-600-p4[1],800,600);
			}else {
				double[] p4=getCoord500(name);
				return new Rectangle2D.Double(p1[0]+p2[0]+p4[0],p1[1]-p2[1]-300-p4[1],400,300);
			}
		}
		return null;
	}
	
	public static String getBasicMeshNameByXY(int coord,double x,double y,BasicMeshType type) {
		String top=Integer.toString(coord);
		if(top.length()==1)top="0"+top;
		int[] st=getName50000(x,y);
		if(st[1]<0||st[1]>=ALP.length)return null;
		if(st[0]<0||st[0]>=ALP.length)return null;
		if(type==BasicMeshType.S50000)return top+ALP[st[1]].toUpperCase()+ALP[st[0]].toUpperCase();
		String name=top+ALP[st[1]].toUpperCase()+ALP[st[0]].toUpperCase();
		Rectangle2D r=getBasicMeshBounds(name);
		int s2x=0;
		int s2y=0;
		double ox=r.getX();
		double oy=r.getY();
		double ww=r.getWidth()/10;
		double hh=r.getHeight()/10;
		for(int i=0;i<10;i++) {
			for(int j=0;j<10;j++) {
				double wx=i*ww+ox;
				double wy=j*hh+oy;
				if(x>=wx&&x<wx+ww&&y>=wy&&y<wy+hh) {
					s2x=i;
					s2y=9-j;
					break;
				}
			}
		}
		String name2=name+Integer.toString(s2y)+Integer.toString(s2x);
		if(type==BasicMeshType.S5000)return name2;
		r=getBasicMeshBounds(name2);
		if(type==BasicMeshType.S2500) {
			ww=r.getWidth()/2;
			hh=r.getHeight()/2;
			if(x>=r.getX()&&x<r.getX()+ww) {
				if(y>=r.getY()&&y<r.getY()+hh) {
					return name2+"3";
				}else {
					return name2+"1";
				}
			}else {
				if(y>=r.getY()&&y<r.getY()+hh) {
					return name2+"4";
				}else {
					return name2+"2";
				}
			}
		}else {
			ww=r.getWidth()/5;
			hh=r.getHeight()/5;
			for(int i=0;i<5;i++) {
				for(int j=0;j<5;j++) {
					double x1=r.getX();
					double x2=x1+(i+1)*ww;
					double y1=r.getY();
					double y2=y1+(j+1)*hh;
					if(x>=x1&&x<x2&&y>=y1&&y<y2) {
						return name2+Integer.toString(4-j)+ALP[i];
					}
				}
			}
			return null;
		}
	}
	
	
	private static double[] getCoord50000(String name) {
		String ss=name.substring(2,4).toLowerCase();
		String s1=ss.substring(0,1);
		String s2=ss.substring(1,2);
		int xx=0;
		int yy=0;
		for(int i=0;i<ALP.length;i++) {
			if(s1.equals(ALP[i])) {
				yy=10-i;
				break;
			}
		}
		for(int i=0;i<ALP.length;i++) {
			if(s2.equals(ALP[i])) {
				xx=i-4;
				break;
			}
		}
		return new double[] {xx*40000,yy*30000};
	}
	

	private static int getBasicMeshNumber(String name) {
		String ss=name.substring(0,2);
		if(ss.startsWith("0"))ss=ss.substring(1, 2);
		return Integer.parseInt(ss);
	}
	
	private static Rectangle2D getStdMeshBounds1st(String name) {
		String la=name.substring(0, 2);
		String lo=name.substring(2, 4);
		double lat=Double.parseDouble(la);
		lat=lat/1.5;
		double lon=100+Double.parseDouble(lo);
		return new Rectangle2D.Double(lon,lat,1.0,40.0/60.0);
	}
	
	private static Rectangle2D getStdMeshBounds2nd(String name) {
		String la1=name.substring(0, 2);
		String lo1=name.substring(2, 4);
		double lat=Double.parseDouble(la1);
		lat=lat/1.5;
		double lon=100+Double.parseDouble(lo1);
		String la2=name.substring(4, 5);
		String lo2=name.substring(5, 6);
		lat=lat+(Double.parseDouble(la2)*2/3)/8;
		lon=lon+Double.parseDouble(lo2)/8;
		return new Rectangle2D.Double(lon,lat,7.5/60.0,5.0/60.0);
	}
	
	private static Rectangle2D getStdMeshBounds3rd(String name) {
		String la1=name.substring(0, 2);
		String lo1=name.substring(2, 4);
		double lat=Double.parseDouble(la1);
		lat=lat/1.5;
		double lon=100+Double.parseDouble(lo1);
		String la2=name.substring(4, 5);
		String lo2=name.substring(5, 6);
		lat=lat+(Double.parseDouble(la2)*2/3)/8;
		lon=lon+Double.parseDouble(lo2)/8;
		String la3=name.substring(6, 7);
		String lo3=name.substring(7, 8);
		lat=lat+(Double.parseDouble(la3)*2/3)/8/10;
		lon=lon+5/8+(Double.parseDouble(lo3)/8/10);
		return new Rectangle2D.Double(lon,lat,45.0/3600.0,30.0/3600.0);
	}
	
	private static int[] getName50000(double x,double y) {
		int yp=10-(int)Math.ceil(y/30000);
		int xp=(int)Math.floor(x/40000)+4;
		return new int[] {xp,yp};
	}
	
	
	private static double[] getCoord5000(String name) {
		String val=name.substring(4,6);
		int yy=Integer.parseInt(val.substring(0, 1));
		int xx=Integer.parseInt(val.substring(1, 2));
		return new double[] {xx*4000,yy*3000};
	}
	
	private static double[] getCoord2500(String name) {
		String val=name.substring(6,7);
		int yy=Integer.parseInt(val.substring(0, 1));
		switch(yy) {
			case 1:
				return new double[] {0,0};
			case 2:
				return new double[] {2000,0};
			case 3:
				return new double[] {0,1500};
			default:
				return new double[] {2000,1500};
		}
	}
	
	private static double[] getCoord1000(String name) {
		int yy=Integer.parseInt(name.substring(6,7));
		String mm=name.substring(7,8).toLowerCase();
		if(mm.equals("a")) {
			return new double[] {0,600*yy};
		}else if(mm.equals("b")) {
			return new double[] {800,600*yy};
		}else if(mm.equals("c")) {
			return new double[] {1600,600*yy};
		}else if(mm.equals("d")) {
			return new double[] {2400,600*yy};
		}else {
			return null;
		}
	}

	private static double[] getCoord500(String name)  {
		int yy=Integer.parseInt(name.substring(6,7));
		int xx=Integer.parseInt(name.substring(7,8));
		double py=yy*300;
		double px=xx*400;
		return new double[] {px,py};
	}
	
	public static void main(String[] args) {
		String name="07DD55";
		System.out.println(getBasicMeshNumber(name));
		Rectangle2D rect=getBasicMeshBounds(name);
		double cx=rect.getCenterX();
		double cy=rect.getCenterY();
		System.out.println(getBasicMeshNameByXY(getBasicMeshNumber(name),cx,cy,BasicMeshType.S5000));
		System.out.println("---");
		double lon=139.71475;
		double lat=35.7007777;
		System.out.println(getStdMeshName1st(lon,lat));
		System.out.println(getStdMeshName2nd(lon,lat));
		String nm=getStdMeshName3rd(lon,lat);
		System.out.println(nm);
		
		System.out.println(getStdMeshBounds1st(nm));
		System.out.println(getStdMeshBounds2nd(nm));
		System.out.println(getStdMeshBounds3rd(nm));
		
		double x=-50000;
		double y=-140700;
		System.out.println(getBasicMeshNameByXY(5,x,y,MeshUtil.BasicMeshType.S5000));
		
	}
}
