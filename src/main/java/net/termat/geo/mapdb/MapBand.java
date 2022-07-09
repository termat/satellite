package net.termat.geo.mapdb;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

public class MapBand {
	@DatabaseField(generatedId=true)
	public long id;

	@DatabaseField
	public long key;

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    public byte[] band;
    
	public void setBand(float[][] f) {
		float[] p=new float[f.length*f[0].length];
		int it=0;
		for(int i=0;i<f.length;i++) {
			for(int j=0;j<f[0].length;j++) {
				p[it++]=f[i][j];
			}
		}
		this.band=compress(floatsToBytes(p));
	}
	
	public void setBand(BufferedImage img) throws IOException {
		this.band=compress(bi2Bytes(img,"png"));
	}
	
	public float[][] getBandAsFloat(int w,int h){
		float[][] ret=new float[w][h];
		float[] p=bytesToFloats(melt(band));
		int it=0;
		for(int i=0;i<ret.length;i++) {
			for(int j=0;j<ret[0].length;j++) {
				ret[i][j]=p[it++];
			}
		}
		return ret;
	}
	
	public BufferedImage getBandAsImage() throws IOException {
		return bytes2Bi(melt(band));
	}
	
	public static byte[] floatsToBytes(float[] floats) {
		byte bytes[] = new byte[Float.BYTES * floats.length];
		ByteBuffer.wrap(bytes).asFloatBuffer().put(floats);
		return bytes;
	}
	
	public static float[] bytesToFloats(byte[] bytes) {
		if (bytes.length % Float.BYTES != 0)
			throw new RuntimeException("Illegal length");
		float floats[] = new float[bytes.length / Float.BYTES];
		ByteBuffer.wrap(bytes).asFloatBuffer().get(floats);
		return floats;
	}
    
	/**
	 * BufferedImageをbyte[]に変換
	 * @param img BufferedImage
	 * @param ext 画像の拡張子
	 * @return byte[]
	 * @throws IOException
	 */
	private static byte[] bi2Bytes(BufferedImage img,String ext)throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write( img, ext, baos );
		baos.flush();
		return baos.toByteArray();
	}

	/**
	 * byte[]をBufferedImageに変換
	 * @param raw byte[]
	 * @return BufferedImage
	 * @throws IOException
	 */
	private static BufferedImage bytes2Bi(byte[] raw)throws IOException{
		ByteArrayInputStream bais = new ByteArrayInputStream(raw);
		BufferedImage img=ImageIO.read(bais);
		return img;
	}
	
	private static byte[] compress(byte[] bi){
		try {
			ByteArrayOutputStream compressBaos = new ByteArrayOutputStream();
	        try (OutputStream gzip = new GZIPOutputStream(compressBaos)) {
	            gzip.write(bi);
	        }
	        return compressBaos.toByteArray();
		}catch(IOException e) {
			return bi;
		}
	}
	
	private static byte[] melt(byte[] bi){
		try {
			ByteArrayOutputStream decompressBaos = new ByteArrayOutputStream();
			try (InputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bi))) {
				int b;
	            while ((b = gzip.read()) != -1) {
	                decompressBaos.write(b);
	            }
	        }
			return decompressBaos.toByteArray();
		}catch(IOException e) {
			return bi;
		}
	}
}
