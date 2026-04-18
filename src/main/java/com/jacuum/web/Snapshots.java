package com.jacuum.web;

import com.jacuum.map.GameMap;
import com.jacuum.web.dto.MapSnapshot;

interface Snapshots {
    MapSnapshot of(GameMap map);
}
