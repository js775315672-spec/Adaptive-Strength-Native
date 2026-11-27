package com.adaptivestrength.nativeapp
class NativeTracker: AutoCloseable {
 companion object { init { System.loadLibrary("ase_native") } }
 private var handle=create()
 private external fun create():Long
 private external fun destroy(p:Long)
 private external fun seedNative(p:Long,a:ByteArray,w:Int,h:Int,x:Int,y:Int,r:Int):Boolean
 private external fun stepNative(p:Long,a:ByteArray,w:Int,h:Int):FloatArray?
 fun seed(a:ByteArray,w:Int,h:Int,x:Int,y:Int,r:Int)=seedNative(handle,a,w,h,x,y,r)
 fun step(a:ByteArray,w:Int,h:Int)=stepNative(handle,a,w,h)
 override fun close(){if(handle!=0L){destroy(handle);handle=0}}
}
