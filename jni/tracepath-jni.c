#include <string.h>
#include <jni.h>

jstring
Java_ch_ethz_nervous_TracePath_getPath(JNIEnv* env, jobject thiz, jstring jhost, jint port) {
    char path[16 * 64] = "";
    const char *host = (*env)->GetStringUTFChars(env, jhost, 0);
    tracepath(host, port, path);
    (*env)->ReleaseStringUTFChars(env, jhost, host);
    return (*env)->NewStringUTF(env, path);
}
