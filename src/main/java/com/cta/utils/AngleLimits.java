package com.cta.utils;

/**
 * AngleLimits - Based on Tallyho's AngleLimits
 * Defines rotation limits for camera entities
 */
public class AngleLimits {
    private final float maxYaw;
    private final float maxPitch;
    private final float minPitch;

    public AngleLimits(float maxYaw, float maxPitch, float minPitch) {
        this.maxYaw = maxYaw;
        this.maxPitch = maxPitch;
        this.minPitch = minPitch;
    }

    public float maxYaw() {
        return maxYaw;
    }

    public float maxPitch() {
        return maxPitch;
    }

    public float minPitch() {
        return minPitch;
    }
    
    // Compatibility aliases
    public float lowerX() {
        return minPitch;
    }

    public float upperX() {
        return maxPitch;
    }

    public float Y() {
        return maxYaw;
    }
}
