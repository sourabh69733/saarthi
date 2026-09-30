#include <jni.h>
#include <string>
#include <vector>
#include "whisper.h"

extern "C" JNIEXPORT jlong JNICALL
Java_dev_saarthi_app_WhisperEngine_initNative(JNIEnv *env, jobject, jstring model_path) {
    const char *path = env->GetStringUTFChars(model_path, nullptr);
    whisper_context *ctx = whisper_init_from_file_with_params(path, whisper_context_default_params());
    env->ReleaseStringUTFChars(model_path, path);
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT jstring JNICALL
Java_dev_saarthi_app_WhisperEngine_transcribeNative(JNIEnv *env, jobject, jlong handle, jfloatArray audio) {
    auto *ctx = reinterpret_cast<whisper_context *>(handle);
    if (ctx == nullptr) return env->NewStringUTF("");

    const jsize count = env->GetArrayLength(audio);
    std::vector<float> samples(count);
    env->GetFloatArrayRegion(audio, 0, count, samples.data());
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.no_context = true;
    params.no_timestamps = true;
    params.single_segment = true;
    params.language = "hi";
    params.translate = false;
    params.n_threads = 4;
    params.greedy.best_of = 1;
    params.temperature_inc = 0.0f;

    std::string result;
    if (whisper_full(ctx, params, samples.data(), count) == 0) {
        const int segments = whisper_full_n_segments(ctx);
        for (int i = 0; i < segments; ++i) {
            result += whisper_full_get_segment_text(ctx, i);
        }
    }
    // Whisper returns UTF-8. Java's NewStringUTF expects Modified UTF-8 and
    // aborts on malformed bytes; String(byte[], "UTF-8") replaces bad input.
    jbyteArray bytes = env->NewByteArray(static_cast<jsize>(result.size()));
    env->SetByteArrayRegion(bytes, 0, static_cast<jsize>(result.size()),
                            reinterpret_cast<const jbyte *>(result.data()));
    jclass string_class = env->FindClass("java/lang/String");
    jmethodID constructor = env->GetMethodID(string_class, "<init>", "([BLjava/lang/String;)V");
    jstring encoding = env->NewStringUTF("UTF-8");
    jstring decoded = static_cast<jstring>(env->NewObject(string_class, constructor, bytes, encoding));
    env->DeleteLocalRef(encoding);
    env->DeleteLocalRef(bytes);
    env->DeleteLocalRef(string_class);
    return decoded;
}

extern "C" JNIEXPORT void JNICALL
Java_dev_saarthi_app_WhisperEngine_freeNative(JNIEnv *, jobject, jlong handle) {
    whisper_free(reinterpret_cast<whisper_context *>(handle));
}
