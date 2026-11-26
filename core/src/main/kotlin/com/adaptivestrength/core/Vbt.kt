package com.adaptivestrength.core
import kotlin.math.*
data class Sample(val us:Long,val x:Double,val y:Double,val confidence:Double,val segment:Int)
data class Rep(val startUs:Long,val endUs:Long,val rom:Double,val mean:Double,val peak:Double)
data class Summary(val unit:String,val mean:Double?,val peak:Double?,val min:Double?,val reps:List<Rep>,val lossPercent:Double?,val samples:Int,val powerEstimate:Double?)
object Vbt {
 fun analyze(points:List<Sample>,metersPerPixel:Double?,kg:Double=0.0,axis:Double=-PI/2):Summary {
  require(metersPerPixel==null||metersPerPixel.isFinite()&&metersPerPixel>0);require(axis.isFinite());require(kg.isFinite()&&kg>=0)
  val scale=metersPerPixel?:1.0;val minRom=if(metersPerPixel==null) 12.0 else .08;val dead=if(metersPerPixel==null) 3.0 else .025
  fun z(p:Sample)=(p.x*cos(axis)+p.y*sin(axis))*scale
  fun valid(p:Sample)=p.us>=0&&p.x.isFinite()&&p.y.isFinite()&&p.confidence in .78..1.001
  var dist=0.0;var duration=0.0;var peak:Double?=null;var min:Double?=null;val reps=mutableListOf<Rep>()
  var low:Sample?=null;var high:Sample?=null;var active=false;var repPeak=0.0
  fun reset(){low=null;high=null;active=false;repPeak=0.0}
  for(i in 1 until points.size){val a=points[i-1];val b=points[i];val dt=(b.us-a.us)/1e6
   if(!valid(a)||!valid(b)||a.segment!=b.segment||dt<=0||dt>.25){reset();continue}
   val delta=z(b)-z(a);val speed=delta/dt
   if(delta>0){dist+=delta;duration+=dt;peak=max(peak?:0.0,speed);min=kotlin.math.min(min?:speed,speed)}
   if(low==null){low=a;high=a}
   if(!active){if(z(b)<z(low!!)){low=b;high=b;repPeak=0.0};repPeak=max(repPeak,speed);if(z(b)-z(low!!)>=dead){active=true;high=b}}
   else {if(z(b)>z(high!!)){high=b;repPeak=max(repPeak,speed)};if(z(high!!)-z(b)>=dead){val l=low!!;val h=high!!;val rom=z(h)-z(l);val t=(h.us-l.us)/1e6;if(rom>=minRom&&t>=.15)reps.add(Rep(l.us,h.us,rom,rom/t,repPeak));low=b;high=b;active=false;repPeak=0.0}}
  }
  val mean=if(duration>0)dist/duration else null;val best=reps.maxOfOrNull{it.mean};val loss=if(best!=null&&reps.size>=2&&best>0)100*(1-reps.last().mean/best) else null
  return Summary(if(metersPerPixel==null)"px" else "m",mean,peak,min,reps,loss,points.count(::valid),if(metersPerPixel!=null&&mean!=null&&kg>0)kg*9.80665*mean else null)
 }
 fun angle(ax:Double,ay:Double,bx:Double,by:Double,cx:Double,cy:Double):Double?{val x=ax-bx;val y=ay-by;val u=cx-bx;val v=cy-by;val n=hypot(x,y)*hypot(u,v);return if(n>1e-9&&n.isFinite()) acos(((x*u+y*v)/n).coerceIn(-1.0,1.0))*180/PI else null}
}

/** Two user-confirmed static background anchors. Reject disagreement rather than invent motion. */
object MotionCorrection {
 fun shift(a:Sample,b:Sample,ax:Double,ay:Double,bx:Double,by:Double,tolerance:Double=2.5):Pair<Double,Double>? {
  if(!listOf(a.x,a.y,b.x,b.y,ax,ay,bx,by,tolerance).all{it.isFinite()}||tolerance<0||a.confidence !in .78..1.001||b.confidence !in .78..1.001||a.us!=b.us)return null
  val dx=a.x-ax;val dy=a.y-ay;val ex=b.x-bx;val ey=b.y-by
  return if(hypot(dx-ex,dy-ey)<=tolerance)(dx+ex)/2 to (dy+ey)/2 else null
 }
}

/** Inverse rigid camera transform from two user-confirmed stationary anchors.
 * Reject changing separation (zoom/perspective), poor scores, and large rotations.
 * Two anchors cannot distinguish camera motion from coherent motion of the anchors.
 */
data class RigidCorrection(val ax:Double,val ay:Double,val x:Double,val y:Double,val cos:Double,val sin:Double){
 fun correct(p:Sample):Sample {val dx=p.x-x;val dy=p.y-y;return p.copy(x=ax+cos*dx+sin*dy,y=ay-sin*dx+cos*dy)}
}
object RigidBackground {
 fun fit(a:Sample,b:Sample,ax:Double,ay:Double,bx:Double,by:Double):RigidCorrection?{
  if(!listOf(a.x,a.y,b.x,b.y,ax,ay,bx,by).all{it.isFinite()}||a.confidence !in .78..1.001||b.confidence !in .78..1.001||a.us!=b.us)return null
  val dx=bx-ax;val dy=by-ay;val ex=b.x-a.x;val ey=b.y-a.y;val d=hypot(dx,dy);val e=hypot(ex,ey)
  if(d<24||e<24||abs(e/d-1)>.04)return null
  val angle=atan2(dx*ey-dy*ex,dx*ex+dy*ey)
  if(abs(angle)>PI/12)return null
  return RigidCorrection((ax+bx)/2,(ay+by)/2,(a.x+b.x)/2,(a.y+b.y)/2,cos(angle),sin(angle))
 }
}
