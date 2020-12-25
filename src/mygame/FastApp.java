/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.post.FilterPostProcessor;
import com.jme3.system.AppSettings;

/**
 *
 * @author Lenovo
 */
public abstract class FastApp extends SimpleApplication{
    public FastApp(int w, int h, String title){
        settings = new AppSettings(true);
        settings.setResolution(w, h);
        settings.setSamples(2);
        settings.setGammaCorrection(true);
        settings.setVSync(true);
        settings.setTitle(title);
        setShowSettings(false);
    }
}
