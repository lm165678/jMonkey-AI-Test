/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame.test.path;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author JhonKkk
 */
public class SteerZbsControl extends AbstractControl implements PhysicsCollisionListener{
    // 邻居
    private List<Spatial> neighbours;
    private boolean isInit = false;
    private BetterCharacterControl betterCharacterControl;
    boolean isNav;
    float walkMoveSpeed = 0.01f;
    float runMoveSpeed = 0.025f;
    // 执行导航策略时,使用的移动速率
    float moveSpeed = runMoveSpeed;
    AnimChannel animChannel;
    Vector3f walkDirection = new Vector3f();
    BulletAppState bulletAppState;
    // 攻击范围(与目标小于这个范围时,执行攻击范围,这个值建议为实体半径+目标半径*0.8+0.1)
    private float attackDisSq = 1.5f;
    // 细步范围(与目标小于这个范围时不执行导航策略,而是执行细步行到目标的策略,这个值建议为当前实体直径+目标直径+0.1)
    private float walkDisSq = 2.5f;
    // 这个半径决定与邻居的分离策略(建议为当前直径*1.5)
    private float neighboursRadius = 0.5f;

    public void setWalkDisSq(float walkDisSq) {
        this.walkDisSq = walkDisSq;
    }

    public float getWalkDisSq() {
        return walkDisSq;
    }
    

    public void setRunMoveSpeed(float runMoveSpeed) {
        this.runMoveSpeed = runMoveSpeed;
    }

    public float getRunMoveSpeed() {
        return runMoveSpeed;
    }

    public void setWalkMoveSpeed(float walkMoveSpeed) {
        this.walkMoveSpeed = walkMoveSpeed;
    }

    public float getWalkMoveSpeed() {
        return walkMoveSpeed;
    }
    
    public SteerZbsControl(BulletAppState bulletAppState){
        this.bulletAppState = bulletAppState;
    }
    public void setAttackDisSq(float attackDisSq){
        this.attackDisSq = attackDisSq;
    }

    public float getAttackDisSq() {
        return attackDisSq;
    }

    public float getNeighboursRadius() {
        return neighboursRadius;
    }

    public void setNeighboursRadius(float neighboursRadius) {
        this.neighboursRadius = neighboursRadius;
    }
    
    
    protected void onInit(){
        animChannel = ((Node)spatial).getChild("speed").getControl(AnimControl.class).createChannel();
        BoundingBox bb = (BoundingBox) spatial.getWorldBound();
        neighboursRadius = (bb.getXExtent() * 1.5f + runMoveSpeed);
        walkDisSq = bb.getXExtent() * 2.5f * 8;
        attackDisSq = bb.getXExtent() * 2.5f * 1.2f;
        betterCharacterControl = new BetterCharacterControl(bb.getXExtent() * 2.5f, bb.getYExtent() * 2.5f, 8);
        spatial.addControl(betterCharacterControl);
        this.bulletAppState.getPhysicsSpace().add(betterCharacterControl);
        // 如果没有这一行,则第一次进入disSq <= 4这阶段,第一次没有调用changeDir()
        // 将导致在"disSq <= 4"的内部逻辑中插值无效的朝向
        // 由于如果zbs刚开始就位于disSq <= 4内,所以这个zbs已经执行过changeDir()
        // 而其他没有进入disSq <= 4的实体在第一次时还没有调用changeDir()就执行插值朝向会有问题
        // 所以这里确保第一次不需要等待,第一次直接changeDir()以便正确插值
        watingTime = 600;
        
        // 用于转向实施的邻居列表
        neighbours = new ArrayList<>();
    }

    public BetterCharacterControl getBetterCharacterControl() {
        return betterCharacterControl;
    }
    String lastAnim;
    private void playAnim(String name, float blendTime){
        if(lastAnim != null && lastAnim.equals(name))return;
        animChannel.setAnim(name, blendTime);
        lastAnim = name;
    }
    private Vector3f targetDir = new Vector3f();
    private Vector3f targetDir2 = new Vector3f();
    private float changeAmnt = 0;
    private float changeAmnt2 = 0;
    private float rotateAmntSpeed = 2.0f;
    private Spatial target;
    private float watingTime = 0;
    private void lookToDir(float tpf){
        if(changeAmnt >= 1)return;
        changeAmnt += tpf * rotateAmntSpeed;
        if(changeAmnt >= 1)changeAmnt = 1;
        betterCharacterControl.setViewDirection(betterCharacterControl.getViewDirection().interpolateLocal(targetDir, changeAmnt));
    }
    private void lookToDir(Vector3f dir, float tpf){
        if(changeAmnt2 >= 1)return;
        changeAmnt2 += tpf * rotateAmntSpeed;
        if(changeAmnt2 >= 1){
            changeAmnt2 = 1;
        }
        betterCharacterControl.setViewDirection(betterCharacterControl.getViewDirection().interpolateLocal(dir, changeAmnt2));
    }

    /**
     * 设置插值朝向速率
     * @param rotateAmntSpeed 
     */
    public void setRotateAmntSpeed(float rotateAmntSpeed) {
        this.rotateAmntSpeed = rotateAmntSpeed;
    }
    
    /**
     * 设置目标对象
     * @param target 
     */
    public void setTarget(Spatial target){
        this.target = target;
    }
    /**
     * 释放处于导航状态
     * @param move 
     */
    public void setNav(boolean move){
        isNav = move;
    }
    public void onChangeView(Vector3f dir){
        targetDir.set(dir);
        // 施加分离转向力
        neighbours.clear();
        SteerBehaviorServer.getInstance().findNeighbours(spatial, neighbours, neighboursRadius);
        Vector3f steer = SteerBehaviorServer.getInstance().calculateSeparationForce(spatial, null, neighbours);
        steer.y = 0;
        steer.normalizeLocal();
        // 判断是否和当前方向垂直相反,如果是,则随机选择左右45°方向
        if(targetDir.negate().dot(steer) >= 0.85f){
            Quaternion q = new Quaternion();
            q.fromAngleAxis((float) Math.toRadians(Math.random() > 0.5f ? 45 : -45), Vector3f.UNIT_Y).multLocal(steer);
        }
        // 权重0.5f
        targetDir.addLocal(steer.multLocal(0.25f));
        targetDir.y = 0;
        targetDir.normalizeLocal();
        changeAmnt = 0;
    }
    float rotateSpeed = 0.1f;
    Quaternion q = new Quaternion();
    float _left = (float) Math.toRadians(45);
    float _right = (float) Math.toRadians(-45);
    private void changeDir(){
        Vector3f tarPos = target.getWorldTranslation();
        tarPos.subtract(spatial.getWorldTranslation(), targetDir2);
        if(targetDir2.dot(betterCharacterControl.getViewDirection()) <= 0.2f){
            targetDir2.set(betterCharacterControl.getViewDirection());
            q.fromAngleAxis(_left, Vector3f.UNIT_Y);
            q.multLocal(targetDir2);
        }
        targetDir2.y = 0;
        targetDir2.normalizeLocal();
        changeAmnt2 = 0;
    }
    @Override
    protected void controlUpdate(float tpf) {
        try {
            if(!isInit){
                onInit();
                isInit = true;
            }
            if(betterCharacterControl != null){
                boolean isMove = false;
                // 以便进行下面if(moveSpeed < disSq)测试
                moveSpeed = runMoveSpeed;
                walkDirection.set(0, 0, 0);
                // 如果足够靠近玩家,则不按照导航进行,而是直接靠近玩家
                float disSq = target.getWorldTranslation().distanceSquared(spatial.getWorldTranslation());
                if(disSq <= walkDisSq){
                    // 隔一段时间计算方向
                    // 朝向玩家
                    watingTime += tpf * 1000L;
                    if(watingTime >= 500){
                        // 更新朝向
                        changeDir();
                    }
                    lookToDir(targetDir2, tpf);
                    // 判断具体距离,如果距离小于移动速率,则移动,
                    // 否则,判断释放处于工具范围距离,是,则执行攻击,
                    // 否则,按照距离计算,计算出具体的移动速率为:当前disSq的开放根-工具距离-0.1
                    // 这样,就可以在当前帧移动到工具范围内
                    // 由于进入这一环节表明zbs不可能执行run动画,根据具体的移动速率执行walk动画
                    
                    // 判断具体距离,如果距离小于移动速率,则移动,
                    if(moveSpeed * moveSpeed < disSq){
                        //播放run动画
                        playAnim("run", 0.5f);
                        isMove = true;
                    }
                    else{
                        // 否则,判断释放处于工具范围距离,是,则执行攻击,
                        if(disSq <= attackDisSq){
                            // 攻击或待机
                            // 只有朝向玩家时,才能执行攻击
                            if(betterCharacterControl.getViewDirection().dot(target.getWorldTranslation().subtract(spatial.getWorldTranslation()).normalizeLocal()) >= 0.8f){
                                playAnim("attack", 0.5f);
                            }
                            else{
                                playAnim("idle", 0.5f);
                            }
                        }
                        else{
                            // 否则,按照距离计算,计算出具体的移动速率为:当前disSq的开放根-攻击距离开放根+0.1(这里,如果直接把实际速度设置为Math.sqrt(disSq - attackDisSq),假设攻击距离attackSq = 1.5f,则zbs按照计算的速率移动后可能disSq为1.52f,所以要把实际的移动速率+0.1f,让zbs多移动一点)
                            // 这样,就可以在当前帧移动到工具范围内
                            // 由于进入这一环节表明zbs不可能执行run动画,根据具体的移动速率执行walk动画
                            // 注意:有可能+0.1后,导致移动速率过多,然后zbs已经碰到了target对象,但是这个时候zbs已经处于attack范围,所以可以忽略这个问题
                            
                            // 请注意,确保朝向玩家(而不是背向玩家)再walk
                            if(betterCharacterControl.getViewDirection().dot(target.getWorldTranslation().subtract(spatial.getWorldTranslation()).normalizeLocal()) >= 0.8f){
                                isMove = true;
                                float dis = (float) (Math.sqrt(disSq - attackDisSq) + 0.1f);
                                // 播放walk动画
                                moveSpeed = dis;
                                playAnim("walk", 0.5f);
                            }
                            else{
                                playAnim("idle", 0.5f);
                            }
                        }
                    }
                }
                else if(isNav){
                    // 处于路径导航
                    // 如果进入这一环节,则表明距离玩家距离平方>4
                    // 则表明比较远离玩家,执行run动画
                    playAnim("run", 0.5f);
                    lookToDir(tpf);
                    moveSpeed = runMoveSpeed;
                    isMove = true;
                }
                else{
                    // 1.考虑以下情况,zbs不处于Nav状态同时disSq > 4
                    // 这个时候可以先待机，然后触发一次nav寻路,使得zbs处于nav状态
                    // 2.还有一种情况是,zbs进入了walk状态,然后zbs刚好是背向玩家的
                    // 这个时候,zbs一旦walk,就会导致zbs的disSq > 4同时zbs又不是处于Nav状态
                    // 这个时候可以按照1的情况处理,也可以确保zbs朝向玩家后再walk
                    // 但是如果按照1的情况处理,有可能导致上一帧处于isNav状态,执行run
                    // 下一帧处于disSq < 4状态,然后又再次背向玩家,重复如此
                    // 所以最好的处理就是让zbs朝向玩家之后,再walk
                    playAnim("idle", 0.5f);
                }
                if(isMove){
                    //移动角色
                    Vector3f dir = betterCharacterControl.getViewDirection().clone();
                    walkDirection.addLocal(dir.mult(moveSpeed));
                }
                betterCharacterControl.setWalkDirection(walkDirection);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    @Override
    public void collision(PhysicsCollisionEvent event) {
        
    }

    
    
}
