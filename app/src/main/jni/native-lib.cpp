#include <jni.h>
#include <android/bitmap.h>

#define TAG "native-lib"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)

int* lastFrame;
int lastSize = -1;

extern "C"{
    JNIEXPORT void JNICALL
    Java_pers_lxt_remotecontrol_view_ClickPad_compoundImage(JNIEnv *env, jclass type, jobject bmp,
                                                            jlong id) {

        AndroidBitmapInfo bmpInfo={0};
        AndroidBitmap_getInfo(env,bmp,&bmpInfo);
        int* data=NULL;
        AndroidBitmap_lockPixels(env,bmp,(void**)&data);
        int size = bmpInfo.width*bmpInfo.height;
        if (lastFrame == NULL || id==0 || lastSize != size) {
            lastSize = size;
            lastFrame = new int[size]();
            for(int i = 0;i<size;i++){
                lastFrame[i] = data[i];
            }
        }else{
            for(int i = 0;i<size;i++){
                lastFrame[i] = data[i] ^ lastFrame[i];
                data[i] = lastFrame[i];
            }
        }
        AndroidBitmap_unlockPixels(env,bmp);
    }
}
