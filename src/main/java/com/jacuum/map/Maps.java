package com.jacuum.map;

public interface Maps {
    GameMap generate(String hash, SizePreset size) throws Exception;
}
