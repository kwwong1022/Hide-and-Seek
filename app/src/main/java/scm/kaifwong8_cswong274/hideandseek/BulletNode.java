package scm.kaifwong8_cswong274.hideandseek;

import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;

public class BulletNode extends TransformableNode {
    private static final String TAG = "BossNode";

    private float rotationAngle = 90;

    public BulletNode(TransformationSystem transformationSystem) {
        super(transformationSystem);
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);

    }

    public void updateFacingDirection() {
        Quaternion q = Quaternion.axisAngle(new Vector3(0.0f, 1.0f, 0.0f), rotationAngle);

        this.setLocalRotation(q);
    }
}
