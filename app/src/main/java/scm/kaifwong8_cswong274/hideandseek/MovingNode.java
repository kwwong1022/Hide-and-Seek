package scm.kaifwong8_cswong274.hideandseek;

import android.util.Log;

import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;

import java.util.Random;

public class MovingNode extends TransformableNode {
    private static final String TAG = "MovingNode";

    private float speedX = 0;
    private float speedZ = 0;
    private float rotationAngle = 0;

    private Random rand = new Random();

    public MovingNode(TransformationSystem transformationSystem) {
        super(transformationSystem);

        resetMovingDirection();
    }

    public void resetMovingDirection() {
        speedX = (rand.nextFloat() - 0.5f) * 0.01f;
        speedZ = (rand.nextFloat() - 0.5f) * 0.01f;
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);

        oneUpdateStep();
        updateFacingDirection();
    }

    public void oneUpdateStep() {
        Vector3 posi = getLocalPosition();
        posi.x += speedX;
        posi.z += speedZ;

        setLocalPosition(posi);
    }

    private void updateFacingDirection() {
        rotationAngle = (float) Math.toDegrees(Math.atan2(speedX, speedZ)) ;
        if (rotationAngle < 0) {
            rotationAngle += 360;
        }

        Quaternion q = Quaternion.axisAngle(new Vector3(0.0f, 1.0f, 0.0f), rotationAngle);

        this.setLocalRotation(q);

        //rotationAngle += 5;
    }


}
