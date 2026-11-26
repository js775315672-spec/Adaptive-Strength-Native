package com.adaptivestrength.nativeapp
import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.roundToInt
class Vision:AutoCloseable {
 private val rgba=Mat();private val gray=Mat();private val smooth=Mat();private val circles=Mat()
 fun grayscale(b:Bitmap):ByteArray{Utils.bitmapToMat(b,rgba);Imgproc.cvtColor(rgba,gray,Imgproc.COLOR_RGBA2GRAY);return ByteArray(b.width*b.height).also{gray.get(0,0,it)}}
 fun candidates(b:Bitmap):List<Circle>{grayscale(b);Imgproc.GaussianBlur(gray,smooth,Size(5.0,5.0),1.5);Imgproc.HoughCircles(smooth,circles,Imgproc.HOUGH_GRADIENT,1.2,24.0,100.0,35.0,8,minOf(b.width,b.height)/3);return (0 until minOf(circles.cols(),8)).mapNotNull{val q=circles.get(0,it);if(q==null||q.size<3)null else Circle(q[0].roundToInt(),q[1].roundToInt(),q[2].roundToInt())}}
 override fun close(){rgba.release();gray.release();smooth.release();circles.release()}
 companion object {fun initialize()=OpenCVLoader.initLocal()}
}
data class Circle(val x:Int,val y:Int,val r:Int)
data class Joint(val x:Float,val y:Float,val visibility:Float)
class Pose(context:Context):AutoCloseable {
 private val engine=PoseLandmarker.createFromOptions(context,PoseLandmarker.PoseLandmarkerOptions.builder().setBaseOptions(BaseOptions.builder().setModelAssetPath("pose_landmarker_lite.task").build()).setRunningMode(RunningMode.VIDEO).setNumPoses(1).build())
 private var last=-1L
 fun detect(b:Bitmap,us:Long):List<Joint>{val t=maxOf(last+1,us/1000);last=t;val image=BitmapImageBuilder(b).build();try{return engine.detectForVideo(image,t).landmarks().firstOrNull()?.map{Joint(it.x()*b.width,it.y()*b.height,it.visibility().orElse(0f))}?:emptyList()}finally{image.close()}}
 override fun close(){engine.close()}
}
