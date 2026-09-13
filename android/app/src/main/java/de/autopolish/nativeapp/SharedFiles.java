package de.autopolish.nativeapp;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

public class SharedFiles extends ContentProvider {
    static Uri uri(Context c,File file){return new Uri.Builder().scheme("content").authority(c.getPackageName()+".files").appendPath(file.getName()).build();}
    static File directory(Context c){File dir=new File(c.getCacheDir(),"shared");dir.mkdirs();return dir;}
    private File resolve(Uri u) throws FileNotFoundException {String name=u.getLastPathSegment();if(name==null||!name.matches("[A-Za-z0-9_.-]+"))throw new FileNotFoundException();try{File dir=directory(getContext()).getCanonicalFile();File f=new File(dir,name).getCanonicalFile();if(!f.getParentFile().equals(dir))throw new FileNotFoundException();return f;}catch(IOException e){throw new FileNotFoundException();}}
    public boolean onCreate(){return true;}
    public String getType(Uri u){return u.toString().endsWith(".pdf")?"application/pdf":"image/jpeg";}
    public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{return ParcelFileDescriptor.open(resolve(u),ParcelFileDescriptor.parseMode(mode));}
    public Cursor query(Uri u,String[] projection,String selection,String[] args,String sort){try{File f=resolve(u);String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor cursor=new MatrixCursor(cols);Object[] row=new Object[cols.length];for(int i=0;i<cols.length;i++)row[i]=cols[i].equals(OpenableColumns.SIZE)?f.length():cols[i].equals(OpenableColumns.DISPLAY_NAME)?f.getName():null;cursor.addRow(row);return cursor;}catch(Exception e){return null;}}
    public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
