#include <jni.h>
#include "tracker.hpp"
#include <memory>
using ase::Tracker;
extern "C" JNIEXPORT jlong JNICALL Java_com_adaptivestrength_nativeapp_NativeTracker_create(JNIEnv*,jobject){return reinterpret_cast<jlong>(new Tracker());}
extern "C" JNIEXPORT void JNICALL Java_com_adaptivestrength_nativeapp_NativeTracker_destroy(JNIEnv*,jobject,jlong p){delete reinterpret_cast<Tracker*>(p);}
extern "C" JNIEXPORT jboolean JNICALL Java_com_adaptivestrength_nativeapp_NativeTracker_seedNative(JNIEnv* e,jobject,jlong p,jbyteArray a,jint w,jint h,jint x,jint y,jint r){if(!p||!a||w<=0||h<=0||int64_t(w)*h!=e->GetArrayLength(a))return false;auto* b=e->GetByteArrayElements(a,nullptr);if(!b)return false;bool ok=reinterpret_cast<Tracker*>(p)->seed(reinterpret_cast<uint8_t*>(b),w,h,x,y,r);e->ReleaseByteArrayElements(a,b,JNI_ABORT);return ok;}
extern "C" JNIEXPORT jfloatArray JNICALL Java_com_adaptivestrength_nativeapp_NativeTracker_stepNative(JNIEnv* e,jobject,jlong p,jbyteArray a,jint w,jint h){if(!p||!a||w<=0||h<=0||int64_t(w)*h!=e->GetArrayLength(a))return nullptr;auto* b=e->GetByteArrayElements(a,nullptr);if(!b)return nullptr;auto q=reinterpret_cast<Tracker*>(p)->step(reinterpret_cast<uint8_t*>(b),w,h);e->ReleaseByteArrayElements(a,b,JNI_ABORT);if(!q.valid)return nullptr;jfloat v[]={q.x,q.y,q.q};auto out=e->NewFloatArray(3);if(out)e->SetFloatArrayRegion(out,0,3,v);return out;}
