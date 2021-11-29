package scm.kaifwong8_cswong274.hideandseek;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

public class AimView extends View {
    private static final String TAG = "AimView";

    private boolean first;
    private Paint paint;
    private float width, height;
    private float aimLineLength;

    public AimView(Context context) {
        super(context);
        init();
    }

    private void init() {
        this.first = true;
        this.paint = new Paint();
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(3);
        this.paint.setColor(Color.rgb(240, 240, 240));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (first) {
            this.width = getMeasuredWidth();
            this.height = getMeasuredHeight();
            this.aimLineLength = width/3;
            first = !first;
        }

        canvas.drawLine(width/2, 0, width/2, height, paint);
        canvas.drawLine(0, height/2, width, height/2, paint);
        canvas.drawCircle(width/2, height/2, width/2 - width/10, paint);

        //canvas.drawColor(Color.RED);
    }
}
