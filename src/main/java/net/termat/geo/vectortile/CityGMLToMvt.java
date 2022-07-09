package net.termat.geo.vectortile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.citygml4j.CityGMLContext;
import org.citygml4j.builder.jaxb.CityGMLBuilder;
import org.citygml4j.builder.jaxb.CityGMLBuilderException;
import org.citygml4j.model.citygml.CityGML;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.building.AbstractBoundarySurface;
import org.citygml4j.model.citygml.building.BoundarySurfaceProperty;
import org.citygml4j.model.citygml.building.Building;
import org.citygml4j.model.citygml.building.OuterFloorSurface;
import org.citygml4j.model.citygml.building.RoofSurface;
import org.citygml4j.model.citygml.core.AbstractCityObject;
import org.citygml4j.model.citygml.core.CityModel;
import org.citygml4j.model.citygml.core.CityObjectMember;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurface;
import org.citygml4j.model.gml.geometry.aggregates.MultiSurfaceProperty;
import org.citygml4j.model.gml.geometry.primitives.AbstractRingProperty;
import org.citygml4j.model.gml.geometry.primitives.DirectPositionList;
import org.citygml4j.model.gml.geometry.primitives.Exterior;
import org.citygml4j.model.gml.geometry.primitives.LinearRing;
import org.citygml4j.model.gml.geometry.primitives.Polygon;
import org.citygml4j.model.gml.geometry.primitives.SurfaceProperty;
import org.citygml4j.xml.io.CityGMLInputFactory;
import org.citygml4j.xml.io.reader.CityGMLReadException;
import org.citygml4j.xml.io.reader.CityGMLReader;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;

import com.google.gson.JsonObject;

import net.termat.geo.satellite.VectorReader;

public class CityGMLToMvt {
	
	public static void main(String[] args) {
		File gmldir=new File("K:\\mvt\\20202_matsumoto-shi_2020_citygml_3_op\\udx\\bldg");
//		File gmldir=new File("E:\\mvt\\31201_tottori-shi_2020_citygml_3_op\\udx\\bldg");
//		updateURSchemeURL(gmldir);
		File outdir=new File("K:\\mvt\\matsumoto\\pbf_matsumoto");
		try {
			buildMvtFromGmlBldg(gmldir,outdir,10,16,"H");
		} catch (ParseException e) {
			e.printStackTrace();
		}		
	}
	
	public static void updateURSchemeURL(File dir) {
		for(File f : dir.listFiles()) {
			if(f.isDirectory())continue;
			if(f.getName().endsWith(".gml")) {
				System.out.println(f.getName());
				StringBuffer buf=new StringBuffer();
				String line=null;
				try {
					BufferedReader br=new BufferedReader(new FileReader(f));
					while((line=br.readLine())!=null) {
						line=line.replaceAll("http://www.kantei.go.jp/jp/singi/", "https://www.chisou.go.jp/");
						line=line.replaceAll("/1.4", "/1.5");
						buf.append(line+"\n");
					}
					br.close();
					BufferedWriter bw=new BufferedWriter(new FileWriter(f));
					bw.write(buf.toString());
					bw.close();					
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}
		}
	}

	public static void buildMvtFromGmlBldg(File gmlDir,File outDir,int minZoom,int maxZoom,String propName) throws ParseException {
		VectorReader vr=null;
		for(File f : gmlDir.listFiles()) {
			if(f.isDirectory())continue;
			if(f.getName().endsWith(".gml")) {
				try {
					System.out.println(f.getName());
					VectorReader tmp=gmlToJsonBldg(f,propName);
					if(vr==null) {
						vr=tmp;
					}else {
						vr.addFeature(tmp);
					}
				} catch (CityGMLBuilderException | CityGMLReadException | ParseException e) {
					e.printStackTrace();
				}
			}
		}
		if(vr==null)return;
		MVTBuilder builder=new MVTBuilder(vr);
		builder.createMVTs(minZoom, maxZoom, outDir);
	}

	public static VectorReader gmlToJsonBldg(File f,String propName) throws CityGMLBuilderException, CityGMLReadException, ParseException{
		List<Geometry> geom=new ArrayList<>();
		List<JsonObject> json=new ArrayList<>();
		CityGMLContext ctx = CityGMLContext.getInstance();
		CityGMLBuilder builder = ctx.createCityGMLBuilder();
		CityGMLInputFactory in = builder.createCityGMLInputFactory();
		CityGMLReader reader = in.createCityGMLReader(f);
		while (reader.hasNext()) {
			CityGML citygml = reader.nextFeature();
			if (citygml.getCityGMLClass() == CityGMLClass.CITY_MODEL) {
				CityModel cityModel = (CityModel)citygml;
				for (CityObjectMember cityObjectMember : cityModel.getCityObjectMember()) {
					AbstractCityObject cityObject = cityObjectMember.getCityObject();
					if (cityObject.getCityGMLClass() == CityGMLClass.BUILDING){
						Building b=(Building)cityObject;
						if(b.getMeasuredHeight()!=null){
							createFeature(b,geom,json,propName);
						}
					}
				}
			}
		}
		reader.close();
		return VectorReader.createReader(4326, geom, json);
	}

	private static double getDem(MultiSurfaceProperty msp) {
    	MultiSurface ms=msp.getMultiSurface();
    	 List<SurfaceProperty> spl=ms.getSurfaceMember();
    	 SurfaceProperty ps=spl.get(0);
    	 Polygon pl=(Polygon)ps.getGeometry();		
        Exterior ex=(Exterior)pl.getExterior();
        LinearRing lr=(LinearRing)ex.getRing();
        DirectPositionList dpl=(DirectPositionList)lr.getPosList();
        List<Double> dl=dpl.toList3d();
        double dem=10000;
        for(int i=0;i<dl.size();i=i+3){
            Double d03=dl.get(i+2);
            dem=Math.min(dem, d03);
        }
        return dem;
	}
	
    private static org.locationtech.jts.geom.LinearRing getExterior(Polygon pp,GeometryFactory gf,double dem) {
        Exterior ex=(Exterior)pp.getExterior();
        LinearRing lr=(LinearRing)ex.getRing();
        DirectPositionList dpl=(DirectPositionList)lr.getPosList();
        List<Double> dl=dpl.toList3d();
        List<Coordinate> tmp=new ArrayList<>();
        for(int i=0;i<dl.size();i=i+3){
            Double d01=dl.get(i);
            Double d02=dl.get(i+1);
            Double d03=dl.get(i+2);
            tmp.add(new Coordinate(d02,d01,d03-dem));
        }
        return gf.createLinearRing(tmp.toArray(new Coordinate[tmp.size()]));
    }
    
    private static org.locationtech.jts.geom.LinearRing[] getInerior(Polygon pp,GeometryFactory gf,double dem) {
        List<AbstractRingProperty> ll=pp.getInterior();
        List<org.locationtech.jts.geom.LinearRing> ret=new ArrayList<>();
        for(AbstractRingProperty ex : ll) {
            LinearRing lr=(LinearRing)ex.getRing();
            DirectPositionList dpl=(DirectPositionList)lr.getPosList();
            List<Double> dl=dpl.toList3d();
            List<Coordinate> tmp=new ArrayList<>();
            for(int i=0;i<dl.size();i=i+3){
                Double d01=dl.get(i);
                Double d02=dl.get(i+1);
                Double d03=dl.get(i+2);
                tmp.add(new Coordinate(d02,d01,d03-dem));
            }
            ret.add(gf.createLinearRing(tmp.toArray(new Coordinate[tmp.size()])));
        }
        return ret.toArray(new org.locationtech.jts.geom.LinearRing[ret.size()]);
    }
    
    private static void createFeature(Building b,List<Geometry> geom,List<JsonObject> json,String propName){
    	List<BoundarySurfaceProperty> bs=b.getBoundedBySurface();
    	if(bs.size()==0) {
    		createFeatureLOD0(b,geom,json,propName);
    	}else {
    		createFeatureLOD2(b,geom,json,propName);
    	}
    }
   
    private static MultiSurfaceProperty getLOD0MultiSurfaceProperty(Building b) {
    	MultiSurfaceProperty msp=b.getLod0FootPrint();
    	if(msp!=null) {
    		return msp;
    	}else {
    		return b.getLod0RoofEdge();
    	}
    }
     
    private static List<org.locationtech.jts.geom.Polygon> createGeometry(MultiSurfaceProperty msp,double dem) {
    	GeometryFactory gf=new GeometryFactory();
    	MultiSurface ms=msp.getMultiSurface();
        List<SurfaceProperty> spl=ms.getSurfaceMember();
        List<org.locationtech.jts.geom.Polygon> ret=new ArrayList<>();
        for(SurfaceProperty s : spl) {
            org.locationtech.jts.geom.LinearRing lr01=getExterior((Polygon)s.getGeometry(),gf,dem);
            org.locationtech.jts.geom.LinearRing[] lr02=getInerior((Polygon)s.getGeometry(),gf,dem);
            org.locationtech.jts.geom.Polygon poly=null;
            if(lr02.length==0) {
                poly=gf.createPolygon(lr01);
            }else {
                poly=gf.createPolygon(lr01,lr02);
            }
            ret.add(poly);
        }
        return ret;
    }
    
    private static void createFeatureLOD0(Building b,List<Geometry> geom,List<JsonObject> json,String propName){
    	MultiSurfaceProperty msp=getLOD0MultiSurfaceProperty(b);
		List<org.locationtech.jts.geom.Polygon> ll=createGeometry(msp,0);
		for(org.locationtech.jts.geom.Polygon pl : ll) {
	        JsonObject obj=new JsonObject();
	        obj.addProperty(propName, b.getMeasuredHeight().getValue());
	        geom.add(pl);
	        json.add(obj);
		}
		/*
        List<AbstractGenericAttribute> ll=b.getGenericAttribute();
        for(AbstractGenericAttribute at : ll){
            if(at instanceof StringAttribute){
                StringAttribute st=(StringAttribute)at;
                obj.addProperty(st.getName(), st.getValue());
            }else if(at instanceof MeasureAttribute){
                MeasureAttribute st=(MeasureAttribute)at;
                obj.addProperty(st.getName(), st.getValue().getValue());
            }else if(at instanceof IntAttribute){
                IntAttribute st=(IntAttribute)at;
                obj.addProperty(st.getName(), st.getValue());
            }
        }
        */
    }
    
    private static void createFeatureLOD2(Building b,List<Geometry> geom,List<JsonObject> json,String propName){
    	MultiSurfaceProperty mm=b.getLod0FootPrint();
    	double dem=0.0;
    	if(mm!=null) {
    		dem=getDem(mm);
    	}else {
    		dem=getDem(b.getLod0RoofEdge())-b.getMeasuredHeight().getValue();
    	}
    	List<BoundarySurfaceProperty> bs=b.getBoundedBySurface();
    	for(BoundarySurfaceProperty st : bs) {
    		AbstractBoundarySurface as=st.getBoundarySurface();
    		if(as instanceof RoofSurface) {
    			RoofSurface rs=(RoofSurface)as;
    			MultiSurfaceProperty msp=rs.getLod2MultiSurface();
    			List<org.locationtech.jts.geom.Polygon> ll=createGeometry(msp,dem);
    			for(org.locationtech.jts.geom.Polygon pl : ll) {
        	        JsonObject obj=new JsonObject();
        	        obj.addProperty(propName, getVal(pl.getExteriorRing()));
        	        geom.add(pl);
        	        json.add(obj);
    			}
    		/*
    		}else if(as instanceof OuterCeilingSurface) {
    			OuterCeilingSurface rs=(OuterCeilingSurface)as;
    			MultiSurfaceProperty msp=rs.getLod2MultiSurface();
    			List<org.locationtech.jts.geom.Polygon> ll=createGeometry(msp,dem);
    			for(org.locationtech.jts.geom.Polygon pl : ll) {
        	        JsonObject obj=new JsonObject();
        	        obj.addProperty(propName, getVal(pl.getExteriorRing()));
        	        geom.add(pl);
        	        json.add(obj);
    			}
    		*/
    		}else if(as instanceof OuterFloorSurface) {
    			OuterFloorSurface rs=(OuterFloorSurface)as;
    			MultiSurfaceProperty msp=rs.getLod2MultiSurface();
    			List<org.locationtech.jts.geom.Polygon> ll=createGeometry(msp,dem);
    			for(org.locationtech.jts.geom.Polygon pl : ll) {
        	        JsonObject obj=new JsonObject();
        	        obj.addProperty(propName, getVal(pl.getExteriorRing()));
        	        geom.add(pl);
        	        json.add(obj);
    			}
    		}
    	}
    }
    private static double getVal(LineString ls) {
    	double ave=0.0;
    	Coordinate[] c=ls.getCoordinates();
    	if(c.length==0)return 0.0;
    	for(Coordinate p : c) {
    		ave +=p.getZ();
    	}
    	return ave/c.length;
    }
}
