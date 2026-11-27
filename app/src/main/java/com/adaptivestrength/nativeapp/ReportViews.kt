package com.adaptivestrength.nativeapp
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.adaptivestrength.core.Rep
import com.adaptivestrength.core.ProfileSession
import com.adaptivestrength.core.SpeedProfile
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.*

class RepChart(context:Context):View(context){
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
 private var data=listOf<Rep>();private var unit="px"
 fun update(values:List<Rep>,u:String){data=values.takeLast(24);unit=u;contentDescription="반복별 평균 상승 속도: "+data.joinToString{String.format(Locale.US,"%.2f %s/s",it.mean,unit)};invalidate()}
 override fun onDraw(c:Canvas){super.onDraw(c);c.drawColor(Color.rgb(24,31,38));val d=resources.displayMetrics.density;paint.textSize=12*d;paint.color=Color.WHITE
  if(data.isEmpty()){c.drawText("완결 상승 구간 없음",12*d,30*d,paint);return}
  val left=12*d;val right=width-12*d;val top=30*d;val bottom=height-30*d;if(right<=left||bottom<=top)return
  val max=data.maxOf{it.mean}.coerceAtLeast(.001);val slot=(right-left)/data.size
  c.drawText(String.format(Locale.US,"최대 %.2f %s/s · 최근 %d회",max,unit,data.size),left,18*d,paint)
  data.forEachIndexed{i,r->val x=left+i*slot;val h=(r.mean/max*(bottom-top)).toFloat();paint.color=Color.rgb(52,211,153);c.drawRect(x+slot*.16f,bottom-h,x+slot*.84f,bottom,paint);if(slot>=32*d){paint.color=Color.WHITE;c.drawText(String.format(Locale.US,"%.2f",r.mean),x,bottom+15*d,paint)}}
 }
}
/** Explicit user save, summary only, app-private preferences, bounded to newest 50 records.
 * Never stores source video, landmark coordinates or filenames.
 */
object SessionHistory{
 private const val PREF="ase_local_history"
 fun save(a:Activity,report:String,confirmed:Boolean):Boolean{
  val r=JSONObject(report);val p=a.getSharedPreferences(PREF,Context.MODE_PRIVATE)
  val old=runCatching{JSONArray(p.getString("rows","[]"))}.getOrDefault(JSONArray())
  val id=r.optString("session_id");if(id.isBlank()||(0 until old.length()).any{old.optJSONObject(it)?.optString("session_id")==id})return false
  val row=JSONObject().put("saved_at_ms",System.currentTimeMillis()).put("confirmed",confirmed)
  for(key in listOf("session_id","exercise","setup","median_rep_speed","status","mass_kg","coverage","unit","mean_ascent","peak_ascent","loss_pct"))row.put(key,r.opt(key)?:JSONObject.NULL)
  row.put("reps",r.optJSONArray("reps")?.length()?:0)
  val list=JSONArray();list.put(row);for(i in 0 until min(49,old.length()))old.optJSONObject(i)?.let{list.put(it)}
  return p.edit().putString("rows",list.toString()).commit()
 }
 private fun record(r:JSONObject,confirmed:Boolean,reps:Int)=ProfileSession(r.optString("session_id"),r.optString("exercise"),r.optString("setup"),r.optDouble("mass_kg"),r.optDouble("coverage"),r.optString("unit"),r.optString("status"),reps,r.optDouble("median_rep_speed"),confirmed)
 fun compare(a:Activity,report:String,confirmed:Boolean){
  val r=JSONObject(report);val p=a.getSharedPreferences(PREF,Context.MODE_PRIVATE);val rows=runCatching{JSONArray(p.getString("rows","[]"))}.getOrDefault(JSONArray())
  val prior=(0 until rows.length()).mapNotNull{i->rows.optJSONObject(i)?.let{record(it,it.optBoolean("confirmed"),it.optInt("reps"))}}
  val result=SpeedProfile.compare(record(r,confirmed,r.optJSONArray("reps")?.length()?:0),prior)
  val msg=if(result==null)"동일 운동·중량·촬영 조건의 확인된 보정 기록 3개 이상, 각 3회 이상, 추적률 95% 이상 필요. 현재 분석 직접 확인 및 조건 이름 입력 필요." else String.format(Locale.KOREA,"비교 기록 %d개\n기준 반복 중앙값 %.3f m/s\n현재 %.3f m/s\n차이 %+.1f%%\n\n참고용 측정값. 중량 증가 권고 아님.",result.count,result.baselineMedian,result.currentMedian,result.changePercent)
  AlertDialog.Builder(a).setTitle("개인 속도 비교 · 미검증").setMessage(msg).setPositiveButton("닫기",null).show()
 }
 fun show(a:Activity){
  val p=a.getSharedPreferences(PREF,Context.MODE_PRIVATE);val rows=runCatching{JSONArray(p.getString("rows","[]"))}.getOrDefault(JSONArray())
  val fmt=java.text.SimpleDateFormat("MM/dd HH:mm",Locale.KOREA)
  val lines=(0 until rows.length()).mapNotNull{i->rows.optJSONObject(i)?.let{r->fmt.format(java.util.Date(r.optLong("saved_at_ms")))+" · "+r.optString("mass_kg")+" kg · "+r.optInt("reps")+"회 · 평균 "+r.optString("mean_ascent")+" "+r.optString("unit")+"/s\n"+r.optString("status")}}
  AlertDialog.Builder(a).setTitle("기기 내 운동 기록 · 미검증").setMessage(if(lines.isEmpty())"저장된 요약 없음" else lines.joinToString("\n\n")).setPositiveButton("닫기",null).show()
 }
}
