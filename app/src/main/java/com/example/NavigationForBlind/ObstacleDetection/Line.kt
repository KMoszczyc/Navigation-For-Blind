package com.example.NavigationForBlind.ObstacleDetection

import org.opencv.core.Point

class Line {
    var p1: Point
    var p2: Point

    constructor(x1: Float, y1: Float, x2: Float, y2: Float) {
        p1 = Point(x1.toDouble(), y1.toDouble())
        p2 = Point(x2.toDouble(), y2.toDouble())
    }
}