import com.adaptivestrength.core.*
import com.adaptivestrength.nativeapp.NativeTracker
import kotlin.math.*
var checks=0
fun ck(ok:Boolean,name:String){check(ok){name};println("PASS $name");checks++}
fun eq(a:Double?,b:Double)=a!=null&&abs(a-b)<1e-8
fun main(){
 fun p(t:Long,y:Double,s:Int=0,q:Double=1.0)=Sample(t,0.0,y,q,s)
 val a=Vbt.analyze(listOf(p(0,100.0),p(100000,90.0),p(300000,80.0)),.01,100.0)
 ck(eq(a.mean,2.0/3),"time-weighted mean 0.6667 m/s");ck(eq(a.peak,1.0),"peak");ck(eq(a.min,.5),"min");ck(eq(a.powerEstimate,100*9.80665*2/3),"gravity power estimate")
 ck(Vbt.analyze(listOf(p(0,100.0),p(100000,90.0)),null,100.0).powerEstimate==null,"uncalibrated power withheld")
 ck(Vbt.analyze(listOf(p(0,100.0),p(100000,90.0,1)),.01).mean==null,"segment gap excluded")
 ck(Vbt.analyze(listOf(p(0,100.0),p(400000,90.0)),.01).mean==null,"time gap excluded")
 ck(Vbt.analyze(listOf(p(0,100.0),p(0,90.0)),.01).mean==null,"duplicate time excluded")
 ck(Vbt.analyze(listOf(p(0,100.0),p(100000,90.0,0,.5)),.01).mean==null,"low confidence excluded")
 val cycle=listOf(p(0,100.0),p(100000,95.0),p(200000,90.0),p(300000,80.0),p(400000,85.0),p(500000,100.0),p(700000,95.0),p(900000,80.0),p(1000000,90.0))
 val r=Vbt.analyze(cycle,.01);ck(r.reps.size==2,"two complete ascent candidates");ck(eq(r.reps[0].rom,.2),"rep ROM");ck(eq(r.reps[0].mean,2.0/3),"rep mean");ck(eq(r.lossPercent,25.0),"speed loss")
 ck(Vbt.analyze(cycle.take(4),.01).reps.isEmpty(),"incomplete ascent withheld")
 ck(Vbt.analyze(emptyList(),null).mean==null,"empty input");ck(eq(Vbt.angle(1.0,0.0,0.0,0.0,0.0,1.0),90.0),"90 degree angle");ck(Vbt.angle(0.0,0.0,0.0,0.0,0.0,1.0)==null,"degenerate angle withheld")
 ck(runCatching{Vbt.analyze(cycle,0.0)}.isFailure,"invalid scale rejected")
 val w=120;val h=120;val random=java.util.Random(42);val f=ByteArray(w*h);random.nextBytes(f);val b=ByteArray(w*h);for(y in 0 until h)for(x in 0 until w)if(x>=3&&y>=4)b[y*w+x]=f[(y-4)*w+x-3]
 NativeTracker().use{t->ck(t.seed(f,w,h,60,60,24),"JNI seed");val q=t.step(b,w,h);ck(q!=null&&abs(q[0]-63)<.01&&abs(q[1]-64)<.01,"JNI known displacement");ck(t.step(ByteArray(3),w,h)==null,"JNI buffer length rejection")}
 println("KOTLIN_JNI_PASS=$checks")
}
