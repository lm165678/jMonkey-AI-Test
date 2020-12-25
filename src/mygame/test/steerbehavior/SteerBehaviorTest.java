/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame.test.steerbehavior;

import mygame.FastApp;

/**
 *
 * @author Lenovo
 */
public class SteerBehaviorTest extends FastApp{
    public static void main(String[] args) {
        new SteerBehaviorTest(900, 600, "SteerBehaviorTest").start();
    }
    public SteerBehaviorTest(int w, int h, String title) {
        super(w, h, title);
    }

    @Override
    public void simpleInitApp() {
    }
    
}
