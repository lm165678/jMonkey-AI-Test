/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame.navdoter;

import com.jme3.ai.navmesh.NavMesh;
import com.jme3.ai.navmesh.NavMeshPathfinder;
import com.jme3.ai.navmesh.Path;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.cinematic.MotionPath;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.Spline;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lenovo
 */
public class NavigationControl extends NavMeshPathfinder implements Control,
        JmeCloneable , Runnable{

    /**
     * @return the iNavigationListener
     */
    public INavigationListener getNavigationListener() {
        return iNavigationListener;
    }

    /**
     * @param iNavigationListener the iNavigationListener to set
     */
    public void setNavigationListener(INavigationListener iNavigationListener) {
        this.iNavigationListener = iNavigationListener;
    }
    private static final Logger LOG = Logger.getLogger(NavigationControl.class.
            getName());
    private final ScheduledExecutorService executor;
    private Spatial spatial;
    private boolean pathfinding;
    private Vector3f wayPosition;
    private final boolean debug;
    private MotionPath motionPath;
    private boolean showPath;
    private final SimpleApplication app;
    private Vector3f target;
    private INavigationListener iNavigationListener;
    private boolean isMove = false;
    //使用内部工作线程
    private boolean localExecutor;
    private float pointDistanceClip = 0.25f;
    public static interface INavigationListener{
        /**
         * 下一个路径点
         * @param nextPos 
         */
        void onNext(Vector3f nextPos);
        /**
         * 改变移动朝向回调
         * @param dir 
         */
        void onViewChange(Vector3f dir);
        /**
         * 结束回调
         */
        void onEnd();
    }
    public NavigationControl(ScheduledExecutorService scheduledExecutorService, NavMesh navMesh, Application app, boolean debug){
        super(navMesh); //sets the NavMesh for this control
        this.app = (SimpleApplication) app;
        this.debug = debug;
        if (debug) {
            motionPath = new MotionPath();
            motionPath.setPathSplineType(Spline.SplineType.Linear);
        }
        if(scheduledExecutorService == null){
            executor = Executors.newScheduledThreadPool(1);
            localExecutor = true;
        }
        else{
            executor = scheduledExecutorService;
        }
        startPathFinder();
    }

    public NavigationControl(NavMesh navMesh, Application app, boolean debug) {
        this(null, navMesh, app, debug);
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        try {
            NavigationControl c = (NavigationControl) clone();
            c.spatial = null; // to keep setSpatial() from throwing an exception
            c.setSpatial(spatial);
            return c;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Can't clone control for spatial", e);
        }
    }

    @Override
    public Object jmeClone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Can't clone control for spatial", e);
        }
    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
        this.spatial = cloner.clone(spatial);
    }

    @Override
    public void setSpatial(Spatial spatial) {
        if (this.spatial != null && spatial != null && spatial != this.spatial) {
            throw new IllegalStateException(
                    "This control has already been added to a Spatial");
        }
        this.spatial = spatial;
        if (spatial == null) {
            if(localExecutor)
                shutdownAndAwaitTermination(executor);
        } else {
        }
    }
    public void shutdown(){
        if(localExecutor)
            shutdownAndAwaitTermination(executor);
    }

    //standard shutdown process for executor
    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(6, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(6, TimeUnit.SECONDS)) {
                    LOG.log(Level.SEVERE, "Pool did not terminate {0}", pool);
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
    public void setPointDistanceClip(float pointDistanceClip){
        this.pointDistanceClip = pointDistanceClip;
    }

    @Override
    public void update(float tpf) {
        if (getWayPosition() != null) {
            Vector3f spatialPosition = spatial.getWorldTranslation();
            Vector2f aiPosition = new Vector2f(spatialPosition.x,
                    spatialPosition.z);
            Vector2f waypoint2D = new Vector2f(getWayPosition().x,
                    getWayPosition().z);
            float distance = aiPosition.distance(waypoint2D);
            //在路径点之间移动字符，直到路径点到达，然后设置为空(这样就可以走下一个分支,即走下一个路径点)
            if (distance > pointDistanceClip) {
                Vector2f direction = waypoint2D.subtract(aiPosition);
                direction.mult(tpf);
                if(iNavigationListener != null){
                    iNavigationListener.onViewChange(new Vector3f(direction.x, 0, direction.y).normalize());
                }
            } else {
                setWayPosition(null);
            }
        } else if (!isPathfinding() && getNextWaypoint() != null
                && !isAtGoalWaypoint()) {
            if (showPath) {
                showPath();
                showPath = false;
            }
            //获取下一个路径点
            goToNextWaypoint();
            setWayPosition(new Vector3f(getWaypointPosition()));
            if(iNavigationListener != null){
                iNavigationListener.onNext(wayPosition);
            }
            if(!isMove){
                isMove = true;
            }
        } else {
            if(isMove){
                isMove = false;
                if(iNavigationListener != null){
                    iNavigationListener.onEnd();
                }
            }
        }
    }

    @Override
    public void render(RenderManager rm, ViewPort vp) {

    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    //Computes a path using the A* algorithm. Every 1/2 second checks target
    //for processing. Path will remain untill a new path is generated.
    private void startPathFinder() {
        executor.scheduleWithFixedDelay(this, 0, 500, TimeUnit.MILLISECONDS);
    }

    /**
     * @return the pathfinding
     */
    public boolean isPathfinding() {
        return pathfinding;
    }

    /**
     * @return the wayPosition
     */
    public Vector3f getWayPosition() {
        return wayPosition;
    }

    /**
     * @param wayPosition the wayPosition to set
     */
    public void setWayPosition(Vector3f wayPosition) {
        this.wayPosition = wayPosition;
    }

    //Displays a motion path showing each waypoint. Stays in scene until another 
    //path is set.
    private void showPath() {
        if (motionPath.getNbWayPoints() > 0) {
            motionPath.clearWayPoints();
            motionPath.disableDebugShape();
        }

        for (Path.Waypoint wp : getPath().getWaypoints()) {
            motionPath.addWayPoint(wp.getPosition());
        }
        motionPath.enableDebugShape(app.getAssetManager(), app.getRootNode());
    }

    /**
     * @param target the target to set
     */
    public void setTarget(Vector3f target) {
        this.target = target;
    }
    /**
     * 清理,提供手动停止的调用,避免不必要的update调用
     */
    public void clear(){
        clearPath();
        setWayPosition(null);
    }

    @Override
    public void run() {
        if (target != null) {
            clearPath();
            setWayPosition(null);
            pathfinding = true;
            //setPosition must be set before computePath is called.
            setPosition(spatial.getWorldTranslation());
            //*The first waypoint on any path is the one you set with 
            //`setPosition()`.
            //*The last waypoint on any path is always the `target` Vector3f.
            //computePath() adds one waypoint to the cell *nearest* to the 
            //target only if you are not in the goalCell (the cell target is in), 
            //and if there is a cell between first and last waypoint, 
            //and if there is no direct line of sight. 
            //*If inside the goalCell when a new target is selected, 
            //computePath() will do a direct line of sight placement of 
            //target. This means there will only be 2 waypoints set, 
            //`setPosition()` and `target`.
            //*If the `target` is outside the `NavMesh`, your endpoint will 
            //be also.
            //warpInside(target) moves endpoint within the navMesh always.
            warpInside(target);
//            System.out.println("Target " + target);
            boolean success;
            //compute the path
            success = computePath(target);
//            System.out.println("SUCCESS = " + success);
            if (success) {
                //clear target if successful
                target = null;
                if (debug) {
                    //display motion path
                    showPath = true;
                }
            }
            pathfinding = false;
        }
    }

}
