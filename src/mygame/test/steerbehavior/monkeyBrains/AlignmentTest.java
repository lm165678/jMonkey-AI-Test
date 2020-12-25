/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame.test.steerbehavior.monkeyBrains;

import com.jme3.ai.agents.Agent;
import com.jme3.ai.agents.behaviors.npc.SimpleMainBehavior;
import com.jme3.ai.agents.behaviors.npc.steering.AlignmentBehavior;
import com.jme3.ai.agents.behaviors.npc.steering.ArriveBehavior;
import com.jme3.ai.agents.behaviors.npc.steering.CompoundSteeringBehavior;
import com.jme3.ai.agents.behaviors.npc.steering.PursuitBehavior;
import com.jme3.ai.agents.behaviors.npc.steering.SeekBehavior;
import com.jme3.ai.agents.behaviors.npc.steering.WanderAreaBehavior;
import com.jme3.ai.agents.util.GameEntity;
import com.jme3.ai.agents.util.control.MonkeyBrainsAppState;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.debug.Arrow;
import com.jme3.shadow.CompareMode;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import mygame.FastApp;

/**
 * 测试Alignment(对齐)指导行为
 * @author JhonKkk
 */
public class AlignmentTest extends FastApp implements ActionListener{
    private MonkeyBrainsAppState brainsAppState = MonkeyBrainsAppState.getInstance();
    Agent npcAgent;
    Agent playAgent;
    AnimChannel animChannel;
    BulletAppState bulletAppState;
    CharacterControl characterControl;
    Node jaimeNpc;
    Geometry jaimeNpcCheckSphere;
    Node player;
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
    WanderAreaBehavior wanderAreaBehavior;
    AlignmentBehavior alignmentBehavior;
    public static void main(String[] args) {
        new AlignmentTest(900, 600, "AlignmentTest(对齐行为)").start();
    }
    public AlignmentTest(int w, int h, String title) {
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
        t = tpf;
    }
    Geometry mark;
    protected void initMark() {
        Arrow arrow = new Arrow(Vector3f.UNIT_Y.mult(-2f));

        //Sphere sphere = new Sphere(30, 30, 0.2f);
        mark = new Geometry("BOOM!", arrow);
        //mark = new Geometry("BOOM!", sphere);
        Material mark_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mark_mat.getAdditionalRenderState().setLineWidth(3);
        mark_mat.setColor("Color", ColorRGBA.Red);
        mark.setMaterial(mark_mat);
    }
    
    //瞄点
    Vector3f npcAimingPoint;
    @Override
    public void simpleInitApp() {
        initBullet();
        Node scene = (Node) assetManager.loadModel("Scenes/PursuitSteerScene.j3o");
        rootNode.attachChild(scene);
        Node mainCamera = (Node) scene.getChild("mainCamera");
        if(mainCamera != null){
            cam.setLocation(mainCamera.getWorldTranslation());
            cam.setRotation(mainCamera.getWorldRotation());
            flyCam.setMoveSpeed(10.0f);
        }
        initMark();
        //初始化物理引擎构件
        Node terrain = (Node) scene.getChild("terrain");
        bulletAppState.getPhysicsSpace().add(terrain.getControl(PhysicsControl.class));
        jaimeNpc = (Node) scene.getChild("jaime_npc");
        npcAimingPoint = jaimeNpc.getWorldTranslation().clone();
        jaimeNpcCheckSphere = (Geometry) jaimeNpc.getChild("CheckSphere");
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
        playAgent = new Agent("playerAgent", new TargetAgentSpatial("targetAgentSpatial", npcAimingPoint, mark));
        //指导行为可以嵌套添加
        //创建指导行为
        SimpleMainBehavior simpleMainBehavior = new SimpleMainBehavior(npcAgent);
        //可以添加多个指导行为,但这个demo演示"漫游巡逻指导行为",在x方向来回巡逻。
        wanderAreaBehavior = new WanderAreaBehavior(npcAgent);
        wanderAreaBehavior.setArea(new Vector3f(0, 0.7f, 0), new Vector3f(15, 0, 1));
        List<GameEntity> neighbours = new ArrayList<GameEntity>();
        neighbours.add(playAgent);
        alignmentBehavior = new AlignmentBehavior(npcAgent, neighbours);
        alignmentBehavior.setVelocity(new Vector3f(1, 1, 1));
        alignmentBehavior.setupStrengthControl(10, 0, 10);
        alignmentBehavior.setEnabled(false);
        //想场景图一样,将wanderAreaBehavior附加到simpleMain中
        //如果用组合行为,则组合行为本身是一个行为,因此不能对组合行为的每个字行为进行开启或关闭,因为被视为一个整体,
        //只能通过组合行为本身来开启或关闭整体,为了更好的设置每个行为,这里直接将wanderAreaBehavior添加到simpleMainBehavior中
        simpleMainBehavior.addBehavior(wanderAreaBehavior);
        simpleMainBehavior.addBehavior(alignmentBehavior);
        //为代理设置主指导行为
        npcAgent.setMainBehavior(simpleMainBehavior);
        //设置移动和旋转指导影响力
        npcAgent.setMoveSpeed(.5f);
        npcAgent.setRotationSpeed(1.5f);
        npcAgent.setMass(40.0f);
        npcAgent.setMaxForce(4);
        //添加到dai'li
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
        //检查标记
        executor.scheduleAtFixedRate(new Runnable() {
            Ray checkRay = new Ray();
            CollisionResults results = new CollisionResults();
            @Override
            public void run() {
                //检查玩家是否靠近npc,然后检查是否在npc的可视范围,是则开启追捕行为,关闭巡逻行为
                checkRay.setOrigin(mark.getWorldTranslation().add(0, 5, 0));
                checkRay.setDirection(Vector3f.UNIT_Y.negate().mult(5));
                results.clear();
                jaimeNpcCheckSphere.collideWith(checkRay, results);
                if(results.size() > 0){
                    wanderAreaBehavior.setEnabled(false);
                    alignmentBehavior.setEnabled(true);
                }
                else{
                    wanderAreaBehavior.setEnabled(true);
                    alignmentBehavior.setEnabled(false);
                }
            }
        }, 0, aiRate, TimeUnit.MILLISECONDS);
        DirectionalLightShadowRenderer directionalLightShadowRenderer = new DirectionalLightShadowRenderer(assetManager, 2048, 2);
        DirectionalLight dl = (DirectionalLight) scene.getLocalLightList().get(0);
        directionalLightShadowRenderer.setLight(dl);
        directionalLightShadowRenderer.setEdgesThickness(5);
        directionalLightShadowRenderer.setLambda(0.5f);
        directionalLightShadowRenderer.setShadowCompareMode(CompareMode.Hardware);
        directionalLightShadowRenderer.setShadowZFadeLength(0.13f);
        directionalLightShadowRenderer.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
        viewPort.addProcessor(directionalLightShadowRenderer);
        //输入事件
        inputManager.addListener(this, "pick");
        inputManager.addMapping("pick", new MouseButtonTrigger(1));
        flyCam.setDragToRotate(true);
    }

    @Override
    public void stop() {
        super.stop(); //To change body of generated methods, choose Tools | Templates.
        executor.shutdown();
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if("pick".equals(name) && !isPressed){
            CollisionResults results = new CollisionResults();
            Vector3f click3d = cam.getWorldCoordinates(inputManager.getCursorPosition().clone(), 0.0f).clone();
            Vector3f dir = cam.getWorldCoordinates(inputManager.getCursorPosition().clone(), 1.0f).subtractLocal(click3d).normalize();
            Ray ray = new Ray(click3d, dir);
            rootNode.collideWith(ray, results);
            
            for(int i = 0;i < results.size();i++){
                //遍历每个hit,处理一些逻辑
            }
            
            if(results.size() > 0){
                for(int i = 0;i < results.size();i++){
                    if(results.getCollision(i).getGeometry() != jaimeNpcCheckSphere){
                        mark.setLocalTranslation(results.getCollision(i).getContactPoint().add(0, 2, 0));
                        rootNode.attachChild(mark);
                        break;
                    }
                }
            }
            else{
                rootNode.detachChild(mark);
            }
        }
    }
    
    
}
