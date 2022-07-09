package net.termat.geo.pointcloud;

import java.awt.geom.Rectangle2D;

public interface GeojsonData {
	public String getGeojson();
	public Rectangle2D getBounds();
	public void setCoordSys(int coodId);
	public void setName(String name);
}
