package edu.fgcu.scaryturret.network.webrtc;

import org.webrtc.EglBase;

/**
 * This is written in Java due to some weird library restriction on Java static methods in Kotlin.
 * There was probably another, possibly better way to fix it, but I really don't have time to
 * investigate it right now. If you find yourself reading this comment, it may be worth checking.
 */
public class RootEglBaseBuilder {

    public EglBase getRootEglBase() {
        return EglBase.create();
    }

}
