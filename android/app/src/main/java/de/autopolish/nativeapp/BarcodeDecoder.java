package de.autopolish.nativeapp;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import java.util.*;
final class BarcodeDecoder {
    static String decodeFrame(byte[] bytes,int w,int h){MultiFormatReader reader=new MultiFormatReader();Map<DecodeHintType,Object> hints=new EnumMap<>(DecodeHintType.class);hints.put(DecodeHintType.TRY_HARDER,Boolean.TRUE);hints.put(DecodeHintType.POSSIBLE_FORMATS,Arrays.asList(BarcodeFormat.CODE_128,BarcodeFormat.CODE_39,BarcodeFormat.EAN_13,BarcodeFormat.EAN_8,BarcodeFormat.QR_CODE));for(int turn=0;turn<2;turn++){try{return reader.decode(new BinaryBitmap(new HybridBinarizer(new PlanarYUVLuminanceSource(bytes,w,h,0,0,w,h,false))),hints).getText();}catch(NotFoundException ignored){}finally{reader.reset();}byte[] rotated=new byte[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++)rotated[x*h+h-1-y]=bytes[y*w+x];bytes=rotated;int old=w;w=h;h=old;}return null;}
}
