package mygame.test.path;

import com.jme3.ai.navmesh.NavMesh;
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
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.debug.Arrow;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.EdgeFilteringMode;
import mygame.FastApp;
import mygame.navdoter.NavigationControl;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 * @author normenhansen
 */
public class ZbsTest extends FastApp implements ActionListener{
    NavigationControl navigationControl;
    BulletAppState bulletAppState;
    CharacterControl characterControl;
    boolean isMove;
    float walkMoveSpeed = 0.01f;
    float runMoveSpeed = 0.025f;
    float moveSpeed = 0.025f;
    AnimChannel animChannel;
    Vector3f walkDirection;
    Node player;
    ZbsControl zbsControl;
    public static void main(String[] args) {
        ZbsTest app = new ZbsTest(900, 600, "ZbsTest");
        app.start();
    }

    public ZbsTest(int w, int h, String title) {
        super(w, h, title);
    }
    private void initOther(){
        walkDirection = new Vector3f();
    }
    private void initBulletAppState(){
        bulletAppState = new BulletAppState(PhysicsSpace.BroadphaseType.DBVT);
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        bulletAppState.setDebugEnabled(false);
    }

    @Override
    public void simpleInitApp() {
        initOther();
        initBulletAppState();
        Node scene = (Node) assetManager.loadModel("Scenes/ZbsmeshScene.j3o");
        player = (Node) scene.getChild("Player");
        
        
        bulletAppState.getPhysicsSpace().add(scene.getChild("terrain").getControl(PhysicsControl.class));
        bulletAppState.getPhysicsSpace().add(scene.getChild("Box").getControl(PhysicsControl.class));
        rootNode.attachChild(scene);
        navigationControl = new NavigationControl(new NavMesh(((Geometry)(scene.getChild("NavMesh"))).getMesh()), this, true);
        navigationControl.setEntityRadius(0.2f);
        navigationControl.setPointDistanceClip(2.0f);
        navigationControl.setNavigationListener(new NavigationControl.INavigationListener() {
            @Override
            public void onNext(Vector3f nextPos) {
                //如果距离当前远的话,就run
                //否则,就walk
                if(player.getWorldTranslation().subtract(nextPos).lengthSquared() > 128.0f){
                    moveSpeed = runMoveSpeed;
                }
                else{
                    moveSpeed = walkMoveSpeed;
                }
            }

            @Override
            public void onViewChange(Vector3f dir) {
                zbsControl.onChangeView(dir);
                zbsControl.setNav(true);
                isMove = true;
//                characterControl.setViewDirection(dir);
            }

            @Override
            public void onEnd() {
                isMove = false;
                zbsControl.setNav(false);
                System.out.println(".onEnd()");
            }
        });
        Node player = (Node) scene.getChild("Player");
        Node target = new Node("target");
        scene.attachChild(target);
        zbsControl = new ZbsControl(bulletAppState);
        zbsControl.setTarget(target);
        player.addControl(zbsControl);
        player.addControl(navigationControl);
        Node mainCamera = (Node) scene.getChild("mainCamera");
        cam.setFrustumPerspective(45.0f, cam.getWidth() * 1.0f / cam.getHeight(), 0.01f, 500.0f);
        if(mainCamera != null){
            cam.setLocation(mainCamera.getWorldTranslation());
            cam.setRotation(mainCamera.getWorldRotation());
            flyCam.setMoveSpeed(10.0f);
        }
        flyCam.setDragToRotate(true);
        //注册pick事件
        inputManager.addListener(this, "pick");
        inputManager.addMapping("pick", new MouseButtonTrigger(1));
        initMark();
        initFilter(scene);
    }
    protected void initFilter(Node scene){
        FilterPostProcessor filterPostProcessor = new FilterPostProcessor(assetManager);
        DirectionalLightShadowFilter directionalLightShadowFilter = new DirectionalLightShadowFilter(assetManager, 512, 2);
        DirectionalLight dl = (DirectionalLight) scene.getLocalLightList().get(1);
        directionalLightShadowFilter.setEdgeFilteringMode(EdgeFilteringMode.Bilinear);
//        directionalLightShadowFilter.setEdgesThickness(1);
        directionalLightShadowFilter.setLight(dl);
        directionalLightShadowFilter.setLambda(0.5f);
        directionalLightShadowFilter.setShadowZExtend(cam.getFrustumFar());
        filterPostProcessor.addFilter(directionalLightShadowFilter);
        viewPort.addProcessor(filterPostProcessor);
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
    
    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
        
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    @Override
    public void stop() {
        super.stop(); //To change body of generated methods, choose Tools | Templates.
        navigationControl.shutdown();
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
                // 最接近的碰撞点才是真正的撞击:
                CollisionResult closest = results.getClosestCollision();
                mark.setLocalTranslation(closest.getContactPoint().add(0, 2, 0));
                rootNode.attachChild(mark);
                navigationControl.setTarget(closest.getContactPoint());
                rootNode.getChild("target").setLocalTranslation(closest.getContactPoint());
            }
            else{
                rootNode.detachChild(mark);
            }
        }
    }
}
