package scm.kaifwong8_cswong274.hideandseek;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.view.View;

public class DistToHintGraph extends View {
    private static final String TAG = "DistToHintGraph";

    private boolean first;
    private Paint paint;
    private float width, height;
    private float normalDist;
    private boolean inBossArea;

    public DistToHintGraph(Context context) {
        super(context);
        init();
    }

    private void init() {
        this.first = true;
        this.inBossArea = false;
        this.paint = new Paint();
        normalDist = 50;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (first) {
            this.first = false;
            this.width = getMeasuredWidth();
            this.height = getMeasuredHeight();
        }

        if (!inBossArea) {
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeWidth(12);
            this.paint.setColor(Color.rgb(240, 240, 240));
            canvas.drawLine(0, height/2, width, height/2, paint);
            // mark
            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setColor(Color.rgb(51, 201, 255));
            canvas.drawCircle(normalDist, height-height/2, height/5, paint);
        }

    }

    public void toggleInBossArea() {
        this.inBossArea = !this.inBossArea;
    }

    public void setDistMark(float fullDist, float currDist) {
        normalDist = (currDist - 0) / (fullDist - 0) * (width - 0);
        invalidate();
    }
}
