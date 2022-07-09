package net.termat.geo.mapdb;

import com.j256.ormlite.field.DatabaseField;

public class MapProperty {

	@DatabaseField(generatedId=true)
	public long id;
	
	@DatabaseField
	public int epsg;
	
}
