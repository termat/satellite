package net.termat.geo.satellite;

import java.awt.geom.Area;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.io.ParseException;

import com.google.gson.JsonObject;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;

public class TestRinsou {

	public static void main(String[] args) throws ParseException {
		File out=new File("G:\\三重県\\三重県リスト\\201_津市_林相区分.geojson");
		File f1=new File("G:\\三重県\\三重県リスト\\計画図\\201_津市.geojson");
		File f2=new File("G:\\三重県\\航空レーザ測量成果\\令和元年度第１号\\003_林相区分データ\\01_林相区分データ.shp");
		VectorReader vr1=VectorReader.createReader(f1);
		VectorReader vr2=VectorReader.createReader(f2);
		List<Feature> list=new ArrayList<>();
		List<Area> area1=vr1.getShapeList();
		List<Area> area2=vr2.getShapeList();
		int num=vr1.size();
		for(int i=0;i<num;i++) {
			Area a1=area1.get(i);
			List<Integer> it=new ArrayList<>();
			for(int j=0;j<area2.size();j++) {
				if(a1.intersects(area2.get(j).getBounds2D()))it.add(j);
			}
			JsonObject prop=vr1.getProperty(i);
			list.addAll(createFeature(a1,prop,it,area2,vr2));
			if(i%1000==0)System.out.println(i+" / "+num);
		}
		try {
			BufferedWriter bw=new BufferedWriter(new FileWriter(out));
			FeatureCollection fc=FeatureCollection.fromFeatures(list);
			bw.write(fc.toJson());
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static List<Feature> createFeature(Area a1,JsonObject p1,List<Integer> it,List<Area> sp,VectorReader vr){
		List<Feature> ret=new ArrayList<>();
		for(Integer ii : it) {
			Area ta=VecUtil.clip(a1,sp.get(ii));
			if(ta.isEmpty())continue;
			Geometry g=VecUtil.toMapbox(VecUtil.toGeometry(ta));
			JsonObject cp=p1.deepCopy();
			JsonObject tmp=vr.getProperty(ii);
			cp.addProperty("SPEC",tmp.get("種別").getAsNumber());
			ret.add(Feature.fromGeometry(g, cp));
		}
		return ret;
	}
	
}
