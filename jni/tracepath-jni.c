#include <string.h>
#include <jni.h>

jstring
Java_ch_ethz_nervous_TracePath_getPath(JNIEnv* env, jobject thiz) {
    char path[16 * 64] = "";
    tracepath("ethz.ch", 44444, path);
    return (*env)->NewStringUTF(env, path);
}
