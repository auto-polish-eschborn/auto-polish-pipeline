package de.autopolish.nativeapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Resolution independent controls; no font-dependent emoji glyphs. */
final class AppIcon extends View {
    private final String kind;
    private final Paint p = new Paint(3);
    private int color;
    AppIcon(Context c, String kind, int color) { super(c); this.kind=kind; this.color=color; setContentDescription(kind); }
    void color(int c) { color=c; invalidate(); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        c.save(); c.translate(getWidth()/2f, getHeight()/2f); float s=Math.min(getWidth(),getHeight())/30f; c.scale(s,s);
        p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
        if(kind.equals("barcode")){for(int x=-9;x<=9;x+=3)c.drawLine(x,-7,x,7,p);c.drawLine(-12,-11,-6,-11,p);c.drawLine(6,11,12,11,p);c.drawLine(-12,-11,-12,-6,p);c.drawLine(12,6,12,11,p);}
        else if (kind.equals("bell")) {
            Path path=new Path(); path.moveTo(-8,6); path.lineTo(-6,3); path.lineTo(-6,-3); path.cubicTo(-6,-11,6,-11,6,-3); path.lineTo(6,3); path.lineTo(8,6); path.close(); c.drawPath(path,p); c.drawArc(-2,6,2,10,0,180,false,p);
        } else if (kind.equals("settings")) {
            c.drawCircle(0,0,7,p); c.drawCircle(0,0,3,p);
            for(int i=0;i<8;i++){c.save();c.rotate(i*45);c.drawLine(0,-7,0,-10,p);c.restore();}
        } else if (kind.equals("profile")) {
            c.drawCircle(0,-5,4,p); c.drawArc(-8,1,8,16,180,180,false,p);
        } else if (kind.equals("tire")) {
            c.drawCircle(0,0,11,p); c.drawCircle(0,0,7,p);c.drawCircle(0,0,2,p);
            for(int i=0;i<5;i++){c.save();c.rotate(i*72);c.drawLine(0,-2,0,-7,p);c.restore();}
        } else if(kind.equals("warehouse")) {
            Path path=new Path();path.moveTo(-11,-4);path.lineTo(0,-11);path.lineTo(11,-4);path.lineTo(11,10);path.lineTo(-11,10);path.close();c.drawPath(path,p); c.drawRect(-7,-1,7,10,p);c.drawLine(-7,3,7,3,p);c.drawLine(-7,7,7,7,p);
        } else { c.drawLine(-8,0,8,0,p);c.drawLine(0,-8,0,8,p); }
        c.restore();
    }
}
