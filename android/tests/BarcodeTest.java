package de.autopolish.nativeapp;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
public class BarcodeTest {
 public static void main(String[] args)throws Exception{int checks=0;for(String code:new String[]{"RAD-9001","AP-94347","RAD-20260910-ABC123"}){BitMatrix m=new MultiFormatWriter().encode(code,BarcodeFormat.CODE_128,900,220);byte[] b=new byte[900*220];for(int y=0;y<220;y++)for(int x=0;x<900;x++)b[y*900+x]=(byte)(m.get(x,y)?0:255);if(!code.equals(BarcodeDecoder.decodeFrame(b,900,220)))throw new AssertionError(code);checks++;byte[] rotated=new byte[b.length];for(int y=0;y<220;y++)for(int x=0;x<900;x++)rotated[x*220+219-y]=b[y*900+x];if(!code.equals(BarcodeDecoder.decodeFrame(rotated,220,900)))throw new AssertionError("rotated "+code);checks++;}byte[] blank=new byte[300*300];java.util.Arrays.fill(blank,(byte)255);if(BarcodeDecoder.decodeFrame(blank,300,300)!=null)throw new AssertionError("false positive");System.out.println((checks+1)+" barcode tests passed");}
}
