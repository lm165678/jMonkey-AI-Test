/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame.test.path;

import com.jme3.ai.steering.Obstacle;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;
import mygame.test.steerbehavior.TestSteering;

/**
 *
 * @author JhonKkk
 */
public class SteerBehaviorServer {
    private static SteerBehaviorServer s_Instance;
    // 邻居
    private List<Spatial> neighbours;
    public SteerBehaviorServer(){
        neighbours = new ArrayList<>();
    }
    /**
     * 查找指定对象指定半径范围内的所有邻居,并将结果存入targetNeighbours列表中。
     * @param target
     * @param targetNeighbours
     * @param radius 
     */
    public final void findNeighbours(Spatial target, List<Spatial> targetNeighbours, float radius){
        // 查找邻居
        float r2 = radius*radius;
        for (Spatial s : neighbours) {
            if(s == target)continue;
            float d = target.getWorldTranslation().subtract(s.getWorldTranslation()).lengthSquared();
                if (d<r2) 
                    targetNeighbours.add(s);
        }
    }
    /**
     * 添加邻居
     * @param neighbour 
     */
    public final void addNeighbour(Spatial neighbour){
        neighbours.add(neighbour);
    }
    /**
     * 移除邻居
     * @param neighbour 
     */
    public final void removeNeighbour(Spatial neighbour){
        neighbours.remove(neighbour);
    }
    /**
     * 计算分离转向力
     * @param target
     * @param steering
     * @return 
     */
    public Vector3f calculateSeparationForce(Spatial target, Vector3f steering){
        if(steering == null){
            steering = new Vector3f();
        }
        return calculateSeparationForce(target.getWorldTranslation(), steering, neighbours);
    }
    public Vector3f calculateSeparationForce(Spatial target, Vector3f steering, List<Spatial> neighbours){
        if(steering == null){
            steering = new Vector3f();
        }
        return calculateSeparationForce(target.getWorldTranslation(), steering, neighbours);
    }
    /**
     * 计算分离转向力
     * @param targetLocation
     * @param steering
     * @param neighbours
     * @return 
     */
    public Vector3f calculateSeparationForce(Vector3f targetLocation, 
                                    Vector3f steering, 
                                    List<Spatial> neighbours) {
        
        
        for (Spatial o : neighbours) {
            Vector3f loc = o.getWorldTranslation().subtract(targetLocation);
            float len2 = loc.lengthSquared();
            loc.normalizeLocal();
            steering.addLocal(loc.negate().mult(1f/len2));
        }
        
        return steering;
    }
    /**
     * 返回单例对象
     * @return 
     */
    public final static SteerBehaviorServer getInstance(){
        if(s_Instance == null){
            s_Instance = new SteerBehaviorServer();
        }
        return s_Instance;
    }
}
