package net.termat.geo.satellite;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.locationtech.jts.io.ParseException;

import net.termat.geo.web.WebTile;

public class WebTileReader {
	private String url;
	private int zoom;
	private double resolution;
	private WebTile tile;
	
	public WebTileReader(String url,int zoom,double resolution) {
		this.url=url;
		this.zoom=zoom;
		this.resolution=resolution;
		tile=new WebTile(this.url,this.zoom,this.resolution);
	}
	
	public void build(int planNo,Rectangle2D rect,File out) throws IOException {
		tile.create(planNo, rect);
		String ext=ext(out);
		ImageIO.write(tile.getImage(), ext, out);
		AffineTransform af=new AffineTransform(
				resolution,0,0,-resolution,rect.getX(),rect.getY()+rect.getHeight());
		String name=out.getAbsolutePath();
		if(ext.equals("jpg")){
			name=name.replace(".jpg", ".jgw");
		}else {
			int id=name.lastIndexOf(".");
			name=name.substring(0, id);
			name=name+".pgw";
		}
		BandUtil.writeTransform(af, new File(name));
	}
	
	private String ext(File o) {
		String name=o.getName();
		int id=name.lastIndexOf(".");
		return name.substring(id+1,name.length()).toLowerCase();
	}
	
	public static void main(String[] args) throws ParseException, IOException {
		File f=new File("E:\\業務\\20220217丹波篠山市\\GIS\\国勢調査\\h27ka28221.shp");
		
		File dir=new File("E:\\業務\\20220217丹波篠山市");
		String name="兵庫県数値地図DEM1m.png";
		
		VectorReader vr=VectorReader.createReader(f);
		String[] ll=MeshUtil.getBasicMeshList(5, vr.getBounds(), MeshUtil.BasicMeshType.S2500);
		Rectangle2D rect=null;
		for(String str : ll) {
			if(rect==null) {
				rect=MeshUtil.getBasicMeshBounds(str);
			}else {
				rect.add(MeshUtil.getBasicMeshBounds(str));
			}
		}
		String url="https://gio.pref.hyogo.lg.jp/tile/dsm/{z}/{y}/{x}.png";
		WebTileReader app=new WebTileReader(url,16,1.0);
		app.build(5, rect, new File(dir.getAbsolutePath()+"/"+name));
	}
}
