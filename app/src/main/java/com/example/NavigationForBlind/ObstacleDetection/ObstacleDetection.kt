package com.example.NavigationForBlind.ObstacleDetection

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.NavigationForBlind.DeviceData.Accelerometer
import com.example.NavigationForBlind.SettingsUtils.DetectionHeight
import com.example.NavigationForBlind.SettingsUtils.DetectionWidth
import com.example.NavigationForBlind.SettingsUtils.Settings
import com.example.obstacledetection.R
import org.opencv.android.CameraBridgeViewBase
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.pow

class ObstacleDetection(context: Context, private var accelerometer: Accelerometer)
    : CameraBridgeViewBase.CvCameraViewListener2 {

    private lateinit var mainMat: Mat
    private lateinit var edgeMat: Mat
    private lateinit var tempMat: Mat
    private lateinit var linesMat: Mat

    // FPS
    private var MIN_FPS = 10000
    private var MAX_FPS = 0
    private var CURRENT_FPS = 0
    private var framesCounter = 0
    private var startTime: Long = 0
    private var currentTime: Long = 1000

    //  Obstacle detection data
    private lateinit var tempLineArr: DoubleArray
    private lateinit var leftBorder: Line
    private lateinit var rightBorder: Line
    private var sizeScale = -1;
    private var framesWithoutCollision = 10;
    private lateinit var lastClosestPoint: Point
    private var lastClosestDistSqr = 0.0
    private lateinit var personPositionPoint: Point
    private var detectionHeight = 0
    private var detectionWidth = 0
    private var maxDistSqr = 0

    private val RED = Scalar(255.0, 0.0, 0.0)
    private val GREEN = Scalar(0.0, 255.0, 0.0)
    private val BLUE = Scalar(0.0, 0.0, 255.0)

    private val collisionSound: MediaPlayer = MediaPlayer.create(context, R.raw.snap2x)
    private var isInitialized = false

    override fun onCameraViewStarted(width: Int, height: Int) {
        if (width<1100)
            sizeScale = 2
        else if (width<1900)
            sizeScale = 3
        else
            sizeScale = 4

        mainMat = Mat()
        edgeMat = Mat()
        linesMat =  Mat()
        tempMat = Mat()

        Log.wtf("size", "$width $height")

        setObstacleDetectionSettings(width, height)

        personPositionPoint = Point((width/2).toDouble(), height.toDouble())
        lastClosestPoint = Point((width/2).toDouble(), (height/2).toDouble())

        collisionSound.isLooping = true

        collisionSound.start()
        collisionSound.setVolume(0f, 0f)
//        collisionSound.playbackParams = collisionSound.playbackParams.setSpeed(4F);

        isInitialized = true
    }

    fun setObstacleDetectionSettings(width: Int, height: Int)
    {
        when(Settings.detectionHeight)
        {
            DetectionHeight.High -> detectionHeight=height/4
            DetectionHeight.Normal -> detectionHeight=height/2
            DetectionHeight.Small -> detectionHeight=height*2/3
        }

        when(Settings.detectionWidth)
        {
            DetectionWidth.Wide -> detectionWidth = width*2/3
            DetectionWidth.Normal -> detectionWidth = width/2
            DetectionWidth.Narrow -> detectionWidth = width/4
        }

        leftBorder = Line((width / 2 - detectionWidth/2).toFloat(),  height.toFloat(), (width / 2).toFloat(), detectionHeight.toFloat())
        rightBorder = Line((width / 2 + detectionWidth/2).toFloat(), height.toFloat(), (width / 2).toFloat(), detectionHeight.toFloat())

        maxDistSqr = detectionHeight*detectionHeight
    }

    override fun onCameraViewStopped() {
        mainMat.release()
        edgeMat.release()
        tempMat.release()
        linesMat.release()
        collisionSound.setVolume(0f, 0f)
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        if(!isInitialized)
            return inputFrame.rgba()

        mainMat = inputFrame.rgba()
        val height = mainMat.height()
        val width = mainMat.width()
        var currentClosestPoint = Point(-1000.0,-1000.0)
        var currentClosestDistSqr = 1000000.0
        var intersected = false
        val horizonAdjustment = mapValue(accelerometer.orientation[2],   0f,  (-Math.PI / 2).toFloat(), - height.toFloat() / 4,height.toFloat() / 4)

        leftBorder.p2.y= (detectionHeight + horizonAdjustment).toDouble()
        rightBorder.p2.y= (detectionHeight + horizonAdjustment).toDouble()

        // Draw intersection lines;
        Imgproc.line(mainMat, leftBorder.p1, leftBorder.p2, GREEN, 3)
        Imgproc.line(mainMat, rightBorder.p1, rightBorder.p2, GREEN, 3)


        // Dont resize tmpmat to tmpmat cuz u gonna get memory leaks
        Imgproc.resize(inputFrame.gray(), tempMat, Size((width / sizeScale).toDouble(), (height / sizeScale).toDouble()))
        val meanColorScalar = Core.mean(tempMat)
        val meanColor = meanColorScalar.`val`[0]
        val lowerCanny = (Math.max(0.0, (1.0 - 0.33) * meanColor))
        val upperCanny = (Math.min(255.0, (1.0 + 0.33) * meanColor))

        Imgproc.medianBlur(tempMat, tempMat, 5) //noise removal, blurring
        Imgproc.blur(tempMat, tempMat, Size(5.0,5.0)) // bluring small edges
        Imgproc.dilate(tempMat, tempMat, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0,5.0 ))) // white pixels grow larger, as an effect  black small edges disappear  (in sidewalk)
//        Imgproc.erode(tempMat, tempMat, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))) // not used, makes black edges between sidewalk blocks bigger
        Imgproc.medianBlur(tempMat, tempMat, 5)  // noise removal, blurring
        Imgproc.Canny(tempMat, edgeMat, lowerCanny, upperCanny) // edge detection -> to binary image - zero ones only

        Log.wtf("meanvalue", meanColor.toString() + " "+lowerCanny + " "+ upperCanny)


        if(meanColor>10) {
            Imgproc.HoughLinesP(edgeMat, linesMat,1.0,Math.PI / 180, 40, 40.0,10.0) // finding lines on edge image

            //   Draw edge lines
            for (i in 0 until linesMat.rows()) {
                tempLineArr = linesMat[i, 0]
                var p1 = Point(tempLineArr[0], tempLineArr[1])
                var p2 = Point(tempLineArr[2], tempLineArr[3])

                p1 = translatePoint(p1, sizeScale)
                p2 = translatePoint(p2, sizeScale)

                Imgproc.line(mainMat, p1, p2, BLUE, 3)

                if (isPointInTriangle(p1, leftBorder.p1, rightBorder.p1, rightBorder.p2)) {
                    Imgproc.circle(mainMat, p1, 8, RED, Imgproc.FILLED)
                    intersected = true

                    val distSqr = distanceSqr(personPositionPoint, p1)
                    if(distSqr<currentClosestDistSqr){
                        currentClosestPoint = p1
                        currentClosestDistSqr = distSqr
                    }
                }
                if (isPointInTriangle(p2, leftBorder.p1, rightBorder.p1, rightBorder.p2)) {
                    Imgproc.circle(mainMat, p2, 8, RED, Imgproc.FILLED)
                    intersected = true
                    val distSqr = distanceSqr(personPositionPoint, p2)
                    if(distSqr<currentClosestDistSqr) {
                        currentClosestPoint = p2
                        currentClosestDistSqr = distSqr
                    }
                }

                if (!intersected) {
                    val intersectionPoint1 = getLineIntersection(leftBorder.p1, leftBorder.p2, p1, p2)
                    val intersectionPoint2 = getLineIntersection(rightBorder.p1, rightBorder.p2, p1, p2)

                    if (intersectionPoint1.x != 0.0 && intersectionPoint1.y != 0.0) {
                        Imgproc.circle( mainMat,  intersectionPoint1,8,  RED,  Imgproc.FILLED )
                        intersected = true
                        val distSqr = distanceSqr(personPositionPoint, intersectionPoint1)
                        if(distSqr<currentClosestDistSqr) {
                            currentClosestPoint = intersectionPoint1
                            currentClosestDistSqr = distSqr
                        }
                    }
                    if (intersectionPoint2.x != 0.0 && intersectionPoint2.y != 0.0) {
                        Imgproc.circle(mainMat, intersectionPoint2,  8,   RED,   Imgproc.FILLED)
                        intersected = true
                        val distSqr = distanceSqr(personPositionPoint, intersectionPoint2)
                        if(distSqr<currentClosestDistSqr){
                            currentClosestPoint = intersectionPoint2
                            currentClosestDistSqr = distSqr
                        }
                    }
                }
            }
        }

        if(intersected){
            currentClosestPoint.x = (currentClosestPoint.x + lastClosestPoint.x)/2
            currentClosestPoint.y = (currentClosestPoint.y + lastClosestPoint.y)/2
            lastClosestPoint = currentClosestPoint
            lastClosestDistSqr = currentClosestDistSqr
            framesWithoutCollision = 0
        }
        else
            framesWithoutCollision++


        if (framesWithoutCollision<5) {
            Imgproc.circle(mainMat, lastClosestPoint,20, RED, Imgproc.FILLED)

            val weightedRight = mapValue(lastClosestPoint.x, leftBorder.p1.x, rightBorder.p1.x, 0.0, 1.0)
            val weightedLeft = 1 - weightedRight

            val heightSqr = distanceSqr(personPositionPoint, leftBorder.p2)
            val volumeStrength = mapValue(lastClosestDistSqr, 0.0, heightSqr, 1.5, 0.2).toFloat()
            Log.wtf("volumeStrength",volumeStrength.toString() +" "+ lastClosestDistSqr + " " +(heightSqr))
            collisionSound.setVolume(weightedLeft.toFloat() * volumeStrength, weightedRight.toFloat() * volumeStrength)

            //Throws errors
//            val speed = mapValue(currentClosestDistSqr, 0.0, detectionHeight.toDouble(), 1.0, 5.0).toFloat()
//            collisionSound.setPlaybackParams(collisionSound.playbackParams.setSpeed(speed));
        }
        else if(framesWithoutCollision == 5)
            collisionSound.setVolume(0f, 0f)

        // draw horizon line
//        val horizonHeight = mapValue(accelerometerManager.orientation[2],   0f,  (-Math.PI / 2).toFloat(), 0f,height.toFloat() / 2)
//        Imgproc.line(  mainMat, Point(0.0, horizonHeight.toDouble()), Point(width.toDouble(),  horizonHeight.toDouble()), GREEN, 3)

        updateFPS()
        return mainMat
    }


    private fun updateFPS() {
        if (currentTime - startTime >= 1000) {
            CURRENT_FPS = framesCounter

            if(CURRENT_FPS>MAX_FPS) {
                MAX_FPS = CURRENT_FPS
            }

            if(CURRENT_FPS<MIN_FPS && CURRENT_FPS>0) {
                MIN_FPS = CURRENT_FPS
            }

            framesCounter = 0
            startTime = System.currentTimeMillis()

            Log.wtf("FPS", "current: $CURRENT_FPS, min: $MIN_FPS, max: $MAX_FPS")
        }
        currentTime = System.currentTimeMillis()
        framesCounter += 1

    }

    private fun mapValue( value: Float, old_min: Float,old_max: Float, new_min: Float, new_max: Float  ): Float {
        return if (old_max - old_min != 0f) (value - old_min) / (old_max - old_min) * (new_max - new_min) + new_min else 0f
    }

    private fun mapValue( value: Double,old_min: Double,old_max: Double, new_min: Double, new_max: Double  ): Double {
        return if (old_max - old_min != 0.0) (value - old_min) / (old_max - old_min) * (new_max - new_min) + new_min else 0.0
    }


    private fun getLineIntersection(p0: Point, p1: Point, p2: Point, p3: Point): Point {
        val intersectionPoint = Point(0.0, 0.0)
        val s1_x: Double = p1.x - p0.x
        val s1_y: Double = p1.y - p0.y
        val s2_x: Double = p3.x - p2.x
        val s2_y: Double = p3.y - p2.y
        val s: Double
        val t: Double
        s = (-s1_y * (p0.x - p2.x) + s1_x * (p0.y - p2.y)) / (-s2_x * s1_y + s1_x * s2_y)
        t = (s2_x * (p0.y - p2.y) - s2_y * (p0.x - p2.x)) / (-s2_x * s1_y + s1_x * s2_y)
        if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
            intersectionPoint.x = p0.x + t * s1_x
            intersectionPoint.y = p0.y + t * s1_y
        }
        return intersectionPoint
    }

    fun sign(p1: Point, p2: Point, p3: Point): Double {
        return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)
    }

    fun isPointInTriangle(pt: Point, left: Point, right: Point, top: Point): Boolean {
        if (pt.x > right.x || pt.x < left.x || pt.y < top.y) return false
        val hasNeg: Boolean
        val hasPos: Boolean
        val d1: Double = sign(pt, left, right)
        val d2: Double = sign(pt, right, top)
        val d3: Double = sign(pt, top, left)
        hasNeg = d1 < 0 || d2 < 0 || d3 < 0
        hasPos = d1 > 0 || d2 > 0 || d3 > 0
        return !(hasNeg && hasPos)
    }


    private fun translatePoint(p: Point, scale: Int): Point {
        p.x *= scale.toDouble()
        p.y *= scale.toDouble()
        return p
    }

    private fun distanceSqr(p1: Point, p2: Point) : Double
    {
        return (p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2)
    }
}