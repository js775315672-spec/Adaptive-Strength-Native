package com.adaptivestrength.nativeapp
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlin.math.*
/** Decoder output presentation timestamps, not frame-index or playback-clock estimates. */
object VideoFrames {
 fun bitmap(image:Image,rotation:Int):Bitmap {
  require(image.format==ImageFormat.YUV_420_888){"Unsupported YUV format ${image.format}"}
  val rect=image.cropRect;val sw=rect.width();val sh=rect.height();val scale=min(1.0,640.0/max(sw,sh));val w=max(1,(sw*scale).roundToInt());val h=max(1,(sh*scale).roundToInt());val argb=IntArray(w*h)
  val planes=image.planes;val bufs=planes.map{it.buffer};val starts=bufs.map{it.position()}
  fun read(p:Int,x:Int,y:Int):Int {val pl=planes[p];val index=starts[p]+y*pl.rowStride+x*pl.pixelStride;require(index in starts[p] until bufs[p].limit());return bufs[p].get(index).toInt() and 255}
  for(y in 0 until h)for(x in 0 until w){val sx=rect.left+x*sw/w;val sy=rect.top+y*sh/h;val yy=max(0,read(0,sx,sy)-16);val u=read(1,sx/2,sy/2)-128;val v=read(2,sx/2,sy/2)-128;val r=((298*yy+409*v+128) shr 8).coerceIn(0,255);val g=((298*yy-100*u-208*v+128) shr 8).coerceIn(0,255);val b=((298*yy+516*u+128) shr 8).coerceIn(0,255);argb[y*w+x]=0xff000000.toInt() or (r shl 16) or (g shl 8) or b}
  val bm=Bitmap.createBitmap(argb,w,h,Bitmap.Config.ARGB_8888);if(rotation%360==0)return bm
  return Bitmap.createBitmap(bm,0,0,w,h,Matrix().apply{postRotate(rotation.toFloat())},true).also{if(it!==bm)bm.recycle()}
 }
 fun read(context:Context,uri:Uri,cancel:()->Boolean,consume:(Long,Bitmap)->Boolean){
  val ex=MediaExtractor();var codec:MediaCodec?=null
  try {ex.setDataSource(context,uri,null);val track=(0 until ex.trackCount).firstOrNull{ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/")==true}?:error("영상 트랙 없음")
   ex.selectTrack(track);val format=ex.getTrackFormat(track);val rotation=if(format.containsKey(MediaFormat.KEY_ROTATION))format.getInteger(MediaFormat.KEY_ROTATION) else 0;format.setInteger(MediaFormat.KEY_ROTATION,0);format.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
   val decoder=MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!);codec=decoder;decoder.configure(format,null,null,0);decoder.start();val info=MediaCodec.BufferInfo();var inputDone=false;var outputDone=false;var idle=0
   while(!outputDone&&!cancel()){
    if(!inputDone){val i=decoder.dequeueInputBuffer(5000);if(i>=0){val buf=decoder.getInputBuffer(i)!!;buf.clear();val n=ex.readSampleData(buf,0);if(n<0){decoder.queueInputBuffer(i,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputDone=true}else{decoder.queueInputBuffer(i,0,n,ex.sampleTime,0);ex.advance()}}}
    val o=decoder.dequeueOutputBuffer(info,10000)
    if(o>=0){idle=0;try{outputDone=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0;if(info.size>0&&info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG==0){val image=decoder.getOutputImage(o)?:error("디코더 YUV 프레임 미지원");val bm=image.use{bitmap(it,rotation)};if(!consume(info.presentationTimeUs,bm))return}}finally{decoder.releaseOutputBuffer(o,false)}} else if(o==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){idle=0}else if(++idle>600)error("영상 디코딩 시간 초과")
   }
  }finally{codec?.let{try{it.stop()}catch(_:Exception){};it.release()};ex.release()}
 }
}
