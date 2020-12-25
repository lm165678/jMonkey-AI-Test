/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame.test.steerbehavior.monkeyBrains;

import com.jme3.ai.agents.Agent;
import com.jme3.ai.agents.behaviors.npc.SimpleMainBehavior;
import com.jme3.ai.agents.behaviors.npc.steering.CompoundSteeringBehavior;
import com.jme3.ai.agents.behaviors.npc.steering.WanderAreaBehavior;
import com.jme3.ai.agents.util.control.MonkeyBrainsAppState;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import mygame.FastApp;

/**
 * 测试Wander指导行为
 * @author JhonKkk
 */
public class WanderTest extends FastApp{
    private MonkeyBrainsAppState brainsAppState = MonkeyBrainsAppState.getInstance();
    Agent npcAgent;
    AnimChannel animChannel;
    BulletAppState bulletAppState;
    CharacterControl characterControl;
    Node jaimeNpc;
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    public static void main(String[] args) {
        new WanderTest(900, 600, "WanderTest(区域漫游,巡逻)").start();
    }
    public WanderTest(int w, int h, String title) {
        super(w, h, title);
    }
    private void initBullet(){
        bulletAppState = new BulletAppState(PhysicsSpace.BroadphaseType.DBVT);
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
    }
    String lastAnimName;
    private void playAnim(String name, float blendTime){
        if(lastAnimName != null && lastAnimName.equals(name))return;
        lastAnimName = name;
        animChannel.setAnim(name, blendTime);
    }
    float t = 0;

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf); //To change body of generated methods, choose Tools | Templates.
//        brainsAppState.update(tpf);
        t = tpf;
    }
    

    @Override
    public void simpleInitApp() {
        initBullet();
        Node scene = (Node) assetManager.loadModel("Scenes/WanderSteerScene.j3o");
        rootNode.attachChild(scene);
        Node mainCamera = (Node) scene.getChild("mainCamera");
        if(mainCamera != null){
            cam.setLocation(mainCamera.getWorldTranslation());
            cam.setRotation(mainCamera.getWorldRotation());
            flyCam.setMoveSpeed(10.0f);
        }
        //初始化物理引擎构件
        Node terrain = (Node) scene.getChild("terrain");
        bulletAppState.getPhysicsSpace().add(terrain.getControl(PhysicsControl.class));
        jaimeNpc = (Node) scene.getChild("jaime_npc");
        characterControl = jaimeNpc.getControl(CharacterControl.class);
        characterControl.setEnabled(true);
        animChannel = jaimeNpc.getControl(AnimControl.class).createChannel();
        bulletAppState.getPhysicsSpace().add(characterControl);
        playAnim("Idle", .5f);
        //创建代理
        npcAgent = new Agent("jaime_npc", new AgentSpatial("jaime_npc", new AgentSpatial.IAgentListener() {
            Vector3f tempDir = new Vector3f();
            @Override
            public void onChange(Vector3f dir, Vector3f walkDirection) {
                tempDir.set(dir.x, 0, dir.z).normalizeLocal();
                characterControl.setViewDirection(tempDir);
                characterControl.setWalkDirection(tempDir.multLocal(0.01f, 0, 0.01f));
                playAnim("Walk", 0.5f);
            }

            @Override
            public void onEnd() {
                playAnim("Idle", 0.5f);
                characterControl.setWalkDirection(Vector3f.ZERO);
            }
        }));
        //指导行为可以嵌套添加
        //创建指导行为
        SimpleMainBehavior simpleMainBehavior = new SimpleMainBehavior(npcAgent);
        //为主指导行为添加"复合指导行为"
        CompoundSteeringBehavior compoundSteeringBehavior = new CompoundSteeringBehavior(npcAgent);
        //可以添加多个指导行为,但这个demo演示"漫游巡逻指导行为",在x方向来回巡逻。
        WanderAreaBehavior wanderAreaBehavior = new WanderAreaBehavior(npcAgent);
        wanderAreaBehavior.setArea(new Vector3f(0, 0.7f, 0), new Vector3f(15, 0, 1));
        compoundSteeringBehavior.addSteerBehavior(wanderAreaBehavior);
        //想场景图一样,将compoundSteer附加到simpleMain中
        simpleMainBehavior.addBehavior(compoundSteeringBehavior);
        //为代理设置主指导行为
        npcAgent.setMainBehavior(simpleMainBehavior);
        //设置移动和旋转指导影响力
        npcAgent.setMoveSpeed(.5f);
        npcAgent.setRotationSpeed(10.5f);
        npcAgent.setMass(40.0f);
        npcAgent.setMaxForce(4);
        brainsAppState.addAgent(npcAgent);
        brainsAppState.setEnabled(true);
        brainsAppState.start();
        //在独立线程中执行
        //以60帧率每秒更新ai
        long aiRate = (long) ((1.0f / 60.0f) * 1000);
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                brainsAppState.update(t);
            }
        }, 0, aiRate, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        super.stop(); //To change body of generated methods, choose Tools | Templates.
        executor.shutdown();
    }
    
    
}
