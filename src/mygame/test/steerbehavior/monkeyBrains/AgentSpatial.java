/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame.test.steerbehavior.monkeyBrains;

import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.collision.UnsupportedCollisionException;
import com.jme3.math.Vector3f;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import java.util.Queue;

/**
 *
 * @author Lenovo
 */
public class AgentSpatial extends Spatial{

    /**
     * @return the disL
     */
    public float getDisL() {
        return disL;
    }

    /**
     * @param disL the disL to set
     */
    public void setDisL(float disL) {
        this.disL = disL;
    }
    private float disL = 0.005f;
    public Vector3f walkDirection = new Vector3f();
    protected IAgentListener iAgentListener;
    public static interface IAgentListener{
        void onChange(Vector3f dir, Vector3f walkDirection);
        void onEnd();
    }
    public AgentSpatial(String name, IAgentListener iAgentListener){
        super(name);
        this.iAgentListener = iAgentListener;
    }

    @Override
    public void setLocalTranslation(Vector3f localTranslation) {
        float disSq = getLocalTranslation().distance(localTranslation);
        super.setLocalTranslation(localTranslation); //To change body of generated methods, choose Tools | Templates.
//        if(localTranslation.equals(Vector3f.ZERO)){
//            if(iAgentListener != null){
//                iAgentListener.onEnd();
//            }
//        }
//        else{
//            if(iAgentListener != null){
//                iAgentListener.onChange(getLocalRotation().getRotationColumn(2), walkDirection);
//            }
//        }
        if(disSq >= 0.0045f && !localTranslation.equals(Vector3f.ZERO)){
            walkDirection.set(localTranslation);
            if(iAgentListener != null){
                iAgentListener.onChange(getLocalRotation().getRotationColumn(2), walkDirection);
            }
        }
        else{
            walkDirection.set(0, 0, 0);
            if(iAgentListener != null){
                iAgentListener.onEnd();
            }
        }
    }
    

    @Override
    public void updateModelBound() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setModelBound(BoundingVolume modelBound) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getVertexCount() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getTriangleCount() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void depthFirstTraversal(SceneGraphVisitor visitor, DFSMode mode) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void breadthFirstTraversal(SceneGraphVisitor visitor, Queue<Spatial> queue) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int collideWith(Collidable other, CollisionResults results) throws UnsupportedCollisionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
