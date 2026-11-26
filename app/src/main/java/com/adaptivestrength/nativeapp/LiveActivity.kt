package com.adaptivestrength.nativeapp
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.view.View
import android.view.MotionEvent
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.adaptivestrength.core.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.*

/** Local analysis preview (sampled frames). No capture, upload, or audio. All vision state lives on one worker. */
class LiveActivity:ComponentActivity(){
 private data class Frame(val b:Bitmap,val us:Long)
 private data class Seed(val frame:Frame,val x:Int,val y:Int)
 private val executor=Executors.newSingleThreadExecutor()
 private val pending=AtomicReference<Seed?>(null);private val posting=AtomicBoolean(false)
 @Volatile private var frozen=false
 @Volatile private var active=false
 @Volatile private var wantPose=true
 private var provider:ProcessCameraProvider?=null
 private var analyzer:ImageAnalysis?=null
 private var vision:Vision?=null;private var tracker:NativeTracker?=null;private var pose:Pose?=null
 private var poseFailed=false;private var poseError="";private var misses=0;private var segment=0;private var lastUs=-1L;private var lastUi=0L
 private val rows=ArrayDeque<Sample>();private var latest:Frame?=null;private var shownPath=listOf<Sample>();private var joints=listOf<Joint>()
 private lateinit var view:Preview;private lateinit var status:TextView
 private val paint=Paint(3);private var rect=RectF()
 override fun onCreate(b:Bundle?){super.onCreate(b)
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(16,16,16,16);setBackgroundColor(Color.rgb(16,20,24))}
  setContentView(root)
  fun label(s:String)=TextView(this).apply{text=s;textSize=16f;setTextColor(Color.WHITE);root.addView(this)}
  fun button(s:String,f:()->Unit)=Button(this).apply{text=s;setOnClickListener{f()};root.addView(this)}
  label("실시간 CameraX · 실험판");label("삼각대 고정 권장. 프레임 고정 → 원판 중심 터치. 미보정 px/s, 최근 500샘플.")
  status=label("카메라 시작을 누르세요");view=Preview();root.addView(view,LinearLayout.LayoutParams(-1,0,1f))
  CheckBox(this).apply{text="오프라인 신체 골격 (CPU)";isChecked=true;setTextColor(Color.WHITE);setOnCheckedChangeListener{_,v->wantPose=v};root.addView(this)}
  button("카메라 시작"){if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)startCamera() else requestPermissions(arrayOf(Manifest.permission.CAMERA),7)}
  button("프레임 고정 · 원판 지정"){if(active&&latest!=null){frozen=true;status.text="고정된 화면의 원판 중심을 터치하세요"}}
  button("카메라 정지"){stopCamera()}
 }
 override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<String>,grantResults:IntArray){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==7&&grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)startCamera() else if(requestCode==7)status.text="카메라 권한 없음"}
 @SuppressLint("MissingPermission")
 private fun startCamera(){
  if(active||isFinishing||isDestroyed||checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return
  if(!runCatching{Vision.initialize()}.getOrDefault(false)){status.text="OpenCV 초기화 실패";return}
  active=true;frozen=false;status.text="카메라 연결 중"
  val future=ProcessCameraProvider.getInstance(this)
  future.addListener({
   if(!active||isDestroyed)return@addListener
   try{val p=future.get();provider=p
    val a=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
    analyzer=a;a.setAnalyzer(executor){image->process(image)}
    p.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,a)
   }catch(e:Exception){stopCamera();status.text="카메라 연결 실패: ${e.message}"}
  },ContextCompat.getMainExecutor(this))
 }
 @androidx.annotation.OptIn(markerClass = [androidx.camera.core.ExperimentalGetImage::class])
 private fun process(image:ImageProxy){
  var bitmap:Bitmap?=null;var handed=false
  try{
   if(!active||frozen)return
   val source=image.image?:return
   val b=VideoFrames.bitmap(source,image.imageInfo.rotationDegrees);bitmap=b
   val v=vision?:Vision().also{vision=it};val gray=v.grayscale(b);val us=image.imageInfo.timestamp/1000
   if(us<=lastUs)return
   if(lastUs>=0&&us-lastUs>250000)segment++
   lastUs=us
   pending.getAndSet(null)?.let{s->
    tracker?.close();tracker=null;rows.clear();misses=0;segment=0
    if(s.frame.b.width==b.width&&s.frame.b.height==b.height){
     val n=NativeTracker();val seedGray=v.grayscale(s.frame.b)
     if(n.seed(seedGray,b.width,b.height,s.x,s.y,24))tracker=n else n.close()
     v.grayscale(b) // restore reused buffer before next step
    }
   }
   val q=tracker?.step(gray,b.width,b.height)
   if(tracker!=null){if(q==null){misses++;if(misses>=8){tracker?.close();tracker=null}}else{if(misses>0)segment++;misses=0;rows.addLast(Sample(us,q[0].toDouble(),q[1].toDouble(),q[2].toDouble(),segment));while(rows.size>500)rows.removeFirst()}}
   val now=android.os.SystemClock.elapsedRealtime()
   if(now-lastUi>=125&&posting.compareAndSet(false,true)){
    try{
     lastUi=now
     if(!wantPose){pose?.close();pose=null;poseFailed=false;poseError=""}
     var ls=listOf<Joint>()
     if(wantPose&&!poseFailed){try{val model=pose?:Pose(this).also{pose=it};ls=model.detect(b,us)}catch(e:Throwable){poseFailed=true;poseError=e.message?:"모델 실패";pose?.close();pose=null}}
     val path=rows.toList();val sum=Vbt.analyze(path,null);val tracking=tracker!=null;val error=poseError
     handed=true
     runOnUiThread{
      try{if(active&&!frozen&&!isDestroyed){latest=Frame(b,us);shownPath=path;joints=ls;view.invalidate();status.text="${if(tracking)"추적 중" else "원판 지정 필요"} · 평균 ${sum.mean?.let{"%.1f".format(it)}?:"—"} px/s · 후보 ${sum.reps.size}회 · AI ${ls.size}점${if(error.isEmpty())"" else " · AI 오류: $error"}"}else b.recycle()}finally{posting.set(false)}
     }
    }catch(e:Throwable){posting.set(false);throw e}
   }
  }catch(e:Throwable){runOnUiThread{if(active){stopCamera();status.text="분석 중단: ${e.message}"}}}
  finally{if(!handed)bitmap?.recycle();image.close()}
 }
 private fun stopCamera(){
  active=false;frozen=false;pending.set(null);analyzer?.clearAnalyzer();analyzer=null;provider?.unbindAll();provider=null
  if(!executor.isShutdown)executor.execute{tracker?.close();tracker=null;pose?.close();pose=null;vision?.close();vision=null;rows.clear();misses=0;segment=0;lastUs=-1;poseFailed=false;poseError=""}
  latest=null;shownPath=emptyList();joints=emptyList();view.invalidate();status.text="카메라 정지"
 }
 override fun onStop(){stopCamera();super.onStop()}
 override fun onDestroy(){executor.shutdown();super.onDestroy()}
 private inner class Preview:View(this@LiveActivity){
  override fun onDraw(c:Canvas){c.drawColor(Color.BLACK);val f=latest?:return;val b=f.b;val s=min(width.toFloat()/b.width,height.toFloat()/b.height);rect=RectF((width-b.width*s)/2,(height-b.height*s)/2,(width+b.width*s)/2,(height+b.height*s)/2);paint.style=Paint.Style.FILL;c.drawBitmap(b,null,rect,paint)
   c.save();c.translate(rect.left,rect.top);c.scale(s,s);paint.strokeWidth=2/s;paint.style=Paint.Style.STROKE;paint.color=Color.GREEN
   for(i in 1 until shownPath.size){val a=shownPath[i-1];val d=shownPath[i];if(a.segment==d.segment&&d.us-a.us in 1..250000)c.drawLine(a.x.toFloat(),a.y.toFloat(),d.x.toFloat(),d.y.toFloat(),paint)}
   paint.color=Color.CYAN;for((a,d) in listOf(11 to 12,11 to 13,13 to 15,12 to 14,14 to 16,11 to 23,12 to 24,23 to 24,23 to 25,25 to 27,24 to 26,26 to 28)){val x=joints.getOrNull(a);val y=joints.getOrNull(d);if(x!=null&&y!=null&&x.visibility>=.65f&&y.visibility>=.65f)c.drawLine(x.x,x.y,y.x,y.y,paint)}
   c.restore();paint.style=Paint.Style.FILL
  }
  override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action!=MotionEvent.ACTION_UP)return true;val f=latest?:return true;if(!active||!frozen||!rect.contains(e.x,e.y))return true;val x=((e.x-rect.left)/rect.width()*f.b.width).roundToInt();val y=((e.y-rect.top)/rect.height()*f.b.height).roundToInt();pending.set(Seed(f,x,y));frozen=false;status.text="원판 특징점 초기화 중";performClick();return true}
  override fun performClick():Boolean{super.performClick();return true}
 }
}
