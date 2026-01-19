package com.phasetranscrystal.blockoffensive.compat;

public interface IPassThroughEntity {
    boolean blockoffensive$isWall();
    void blockoffensive$setThroughWall(boolean passed);

    boolean blockoffensive$isSmoke();
    void blockoffensive$setThroughSmoke(boolean passed);
}
