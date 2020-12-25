/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame.navdoter.crowd;

import com.jme3.ai.navmesh.NavMesh;
import com.jme3.ai.navmesh.NavMeshPathfinder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 集群路径查找执行器
 * @author JhonKkk
 */
public class CrowdPathfinderActuator implements Runnable{
    public static abstract class CrowdPathfinder extends NavMeshPathfinder{
        protected boolean isTryToFind;
        protected boolean enable = true;
        public CrowdPathfinder(NavMesh navMesh) {
            super(navMesh);
        }
        public boolean isTryToFind(){
            return isTryToFind;
        }
        public void setTryToFind(boolean tryToFind){
            isTryToFind = tryToFind;
        }
        public void enable(boolean enable){
            this.enable = enable;
        }
        public boolean isEnable(){
            return enable;
        }
        /**
         * 执行查找路径,由CrowdPathfinderActuator调度,不要手动调用
         */
        public abstract void findPath();
    }
    // 所有集群查找器
    private List<CrowdPathfinder> crowdPathfinders;
    // 激活状态
    private boolean enable = false;
    // 执行池
    private ScheduledExecutorService executor;
    // 是否为本地执行池
    private boolean localExcutor;
    public CrowdPathfinderActuator(ScheduledExecutorService executorService){
        executor = executorService;
        if(executor == null){
            executor = Executors.newScheduledThreadPool(1);
            localExcutor = true;
        }
        crowdPathfinders = new ArrayList<>();
    }
    /**
     * 添加人群
     * @param crowdPathfinder 
     */
    public final void addCrowd(CrowdPathfinder crowdPathfinder){
        crowdPathfinders.add(crowdPathfinder);
    }
    /**
     * 移除人群
     * @param crowdPathfinder 
     */
    public final void removeCrowd(CrowdPathfinder crowdPathfinder){
        crowdPathfinders.remove(crowdPathfinder);
    }
    /**
     * 启动人群服务
     */
    public final void luanch(){
        if(enable)return;
        // 启动执行
        enable = true;
        // 每隔500毫秒执行一次
        executor.scheduleAtFixedRate(this, 0, 100L, TimeUnit.MILLISECONDS);
    }
    /**
     * 激活或关闭服务
     * @param enable 
     */
    public final void setEnable(boolean enable){
        this.enable = enable;
    }
    /**
     * 返回当前激活状态
     * @return 
     */
    public final boolean isEnable(){
        return enable;
    }
    /**
     * 销毁人群服务
     */
    public final void destory(){
        if(!enable)return;
        enable = false;
        if(localExcutor){
            localExcutor = false;
            // 销毁本地执行池
            executor.shutdownNow();
            executor = null;
        }
    }
    @Override
    public void run() {
        if(enable){
            // 1.遍历每个Pathfinder,判断是否需要执行路径查找
            for(CrowdPathfinder crowdPathfinder : crowdPathfinders){
                // 为了合理化,可以每个阶段只消费一个crowPathfinder,而不是一个阶段消费所有crowPathfinder
                if(!crowdPathfinder.isEnable())return;
                if(crowdPathfinder.isTryToFind){
                    // 2.查找路径
                    crowdPathfinder.findPath();
                    // 3.重设未已查找
                    crowdPathfinder.setTryToFind(false);
                    break;
                }
            }
        }
    }
    
}
