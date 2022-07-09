package net.termat.geo.mapdb;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import com.j256.ormlite.field.DatabaseField;

public class MapIndex {
	public enum DataType{FLOAT,IMAGE,PBF};

	@DatabaseField(generatedId=true)
	public long id;

	@DatabaseField
	public long key;
	
	@DatabaseField
	public String src;
	
	@DatabaseField
	public String area;
	
	@DatabaseField
	public String band;
	
	@DatabaseField
	public long date;
	
	@DatabaseField
	public int width;
	
	@DatabaseField
	public int height;
	
	@DatabaseField
	public double east;
	
	@DatabaseField
	public double west;
	
	@DatabaseField
	public double north;
	
	@DatabaseField
	public double south;
	
	@DatabaseField
	public DataType type;
	
    @DatabaseField
    public String transform;
	
	public Rectangle2D getBounds(){
		AffineTransform af=getTransform();
		return af.createTransformedShape(new Rectangle2D.Double(0,0,width,height)).getBounds2D();
	}

	public AffineTransform getTransform(){
		String[] ss=transform.split(",");
		double[] ret=new double[ss.length];
		for(int i=0;i<ret.length;i++){
			ret[i]=Double.parseDouble(ss[i]);
		}
		return new AffineTransform(ret);
	}

	public void setTransform(AffineTransform a){
		transform=Double.toString(a.getScaleX())+",0,0,"+Double.toString(a.getScaleY())+",";
		transform=transform+Double.toString(a.getTranslateX())+","+Double.toString(a.getTranslateY());
	}
}
