package net.termat.geo.tiledb;

import java.awt.geom.Rectangle2D;

import com.j256.ormlite.field.DatabaseField;

public class TileProperty {
	@DatabaseField(generatedId=true)
	public long id;

	@DatabaseField
	public int minZoom;

	@DatabaseField
	public int maxZoom;

	@DatabaseField
	public String content;
	
	@DatabaseField
	public String type;
	
	@DatabaseField
	public String copyright;
	
	@DatabaseField
	public String name;
	
	@DatabaseField
	public String bounds;
	
	public void setBounds(Rectangle2D r) {
		bounds=r.getX()+","+r.getY()+","+r.getWidth()+","+r.getHeight();
	}
	
	public Rectangle2D getBounds() {
		String[] ss=bounds.split(",");
		return new Rectangle2D.Double(
				Double.parseDouble(ss[0]),Double.parseDouble(ss[1]),
				Double.parseDouble(ss[2]),Double.parseDouble(ss[3]));
	}
}
