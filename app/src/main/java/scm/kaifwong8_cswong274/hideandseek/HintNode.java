package scm.kaifwong8_cswong274.hideandseek;

import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;

public class HintNode extends TransformableNode {
    private static final String TAG = "HintNode";

    private static final float radius = .4f;
    private static final float speed = .05f;
    private float angle = 0;
    private Vector3 oriPos, nodePos;

    public HintNode(TransformationSystem transformationSystem) {
        super(transformationSystem);
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        super.onUpdate(frameTime);

        if (oriPos == null) oriPos = getLocalPosition();
        if (nodePos == null) nodePos = getLocalPosition();
        nodePos.x = (float) (oriPos.x + radius * Math.cos(angle));
        nodePos.z = (float) (oriPos.z + radius * Math.sin(angle));
        nodePos.y = oriPos.z + 1;
        setLocalPosition(nodePos);

        angle += speed;
    }
}
