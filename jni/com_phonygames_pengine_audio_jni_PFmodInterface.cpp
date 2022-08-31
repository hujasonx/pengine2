#include <com_phonygames_pengine_audio_jni_PFmodInterface.h>

//@line:6

  #include <atomic>
  #include <string>
  #include <map>
  #include "jni-headers/fmod/fmod.h"
  #include "jni-headers/fmod/fmod_dsp.h"
  #include "jni-headers/fmod/fmod_dsp_effects.h"
  #include "jni-headers/fmod/fmod_studio.h"
  #include "jni-headers/fmod/fmod_errors.h"

  static FMOD_STUDIO_SYSTEM* studioSystem;
  static FMOD_SYSTEM* coreSystem;
  static FMOD_STUDIO_BANK* masterBank;
  static FMOD_STUDIO_BUS* masterBus;
  static FMOD_STUDIO_BUS* bgmBus;
  static FMOD_CHANNELGROUP* masterChannelGroup;
  static FMOD_CHANNELGROUP* bgmChannelGroup;

  static FMOD_DSP* bgmFFT;

  static std::map<std::string, FMOD_STUDIO_BANK*> banks;
  static std::map<std::string, FMOD_STUDIO_EVENTDESCRIPTION*> eventDescriptions;
  static std::map<long, FMOD_STUDIO_EVENTINSTANCE*> eventInstances;

  static std::atomic<long> nextInstanceId(0);



  int ERRCHECK(FMOD_RESULT result) {
    if (result != FMOD_OK) {
      printf("FMOD ERROR: %s\n", FMOD_ErrorString(result));
      return 1;
    } else {
      return 0;
    }
  }

   JNIEXPORT jint JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface__1_1init(JNIEnv* env, jclass clazz) {


//@line:45

    // Initialize the FMOD core and studio systems.
    void *extraDriverData = NULL;
    ERRCHECK(FMOD_Studio_System_Create(&studioSystem, FMOD_VERSION));
    ERRCHECK(FMOD_Studio_System_GetCoreSystem(studioSystem, &coreSystem));
    ERRCHECK(FMOD_Studio_System_Initialize(studioSystem, 512, FMOD_STUDIO_INIT_NORMAL, FMOD_INIT_NORMAL, extraDriverData));

    // Create the DSP system for the BGM.
    ERRCHECK(FMOD_System_CreateDSPByType(coreSystem, FMOD_DSP_TYPE_FFT, &bgmFFT));
    ERRCHECK(FMOD_DSP_SetParameterInt(bgmFFT, FMOD_DSP_FFT_WINDOWSIZE, 2048));
    ERRCHECK(FMOD_DSP_SetParameterInt(bgmFFT, FMOD_DSP_FFT_WINDOWTYPE, FMOD_DSP_FFT_WINDOW_HANNING));
//    ERRCHECK(FMOD_DSP_SetParameterInt(bgmFFT, 0, 2048));
//    ERRCHECK(FMOD_DSP_SetParameterInt(bgmFFT, 1, FMOD_DSP_FFT_WINDOW_HANNING));

    ERRCHECK(FMOD_Studio_System_GetBus(studioSystem, "bus:/Master Bus", &masterBus));
    ERRCHECK(FMOD_Studio_Bus_GetChannelGroup(masterBus, &masterChannelGroup));

    ERRCHECK(FMOD_Studio_System_GetBus(studioSystem, "bus:/BGM", &bgmBus));
    ERRCHECK(FMOD_Studio_Bus_GetChannelGroup(bgmBus, &bgmChannelGroup));
    return 0;
	

}

JNIEXPORT jint JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface__1_1close(JNIEnv* env, jclass clazz) {


//@line:81

    FMOD_RESULT result = FMOD_Studio_System_Release(studioSystem);
    return ERRCHECK(result);
	

}

JNIEXPORT jint JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface__1_1update(JNIEnv* env, jclass clazz) {


//@line:96

    FMOD_RESULT result = FMOD_Studio_System_Update(studioSystem);
    return ERRCHECK(result);
	

}

JNIEXPORT void JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface_setSystemParameterByName(JNIEnv* env, jclass clazz, jstring obj_parameterName, jfloat value, jboolean ignoreSeekSpeed) {
	char* parameterName = (char*)env->GetStringUTFChars(obj_parameterName, 0);


//@line:110

    ERRCHECK(FMOD_Studio_System_SetParameterByName(studioSystem, parameterName, value, ignoreSeekSpeed ? 1 : 0));
  
	env->ReleaseStringUTFChars(obj_parameterName, parameterName);

}

static inline jint wrapped_Java_com_phonygames_pengine_audio_jni_PFmodInterface_loadBank
(JNIEnv* env, jclass clazz, jstring obj_bankName, char* bankName) {

//@line:114

    std::string bankNameString(bankName);
    if (banks.find(bankNameString) != banks.end()) {
      printf("loadBank:\tBank %s already loaded, skipping.\n", bankName);
      return 1;
    }

    FMOD_STUDIO_BANK* bank;
    FMOD_RESULT result = FMOD_Studio_System_LoadBankFile(studioSystem, bankName, FMOD_STUDIO_LOAD_BANK_NORMAL, &bank);
    banks[bankNameString] = bank;
    return ERRCHECK(result);
	
}

JNIEXPORT jint JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface_loadBank(JNIEnv* env, jclass clazz, jstring obj_bankName) {
	char* bankName = (char*)env->GetStringUTFChars(obj_bankName, 0);

	jint JNI_returnValue = wrapped_Java_com_phonygames_pengine_audio_jni_PFmodInterface_loadBank(env, clazz, obj_bankName, bankName);

	env->ReleaseStringUTFChars(obj_bankName, bankName);

	return JNI_returnValue;
}

static inline jint wrapped_Java_com_phonygames_pengine_audio_jni_PFmodInterface_unloadBank
(JNIEnv* env, jclass clazz, jstring obj_bankName, char* bankName) {

//@line:127

    std::string bankNameString(bankName);
    if (banks.find(bankNameString) == banks.end()) {
      printf("unloadBank:\tBank %s was never loaded loaded, skipping.\n", bankName);
      return 1;
    }

    FMOD_RESULT result = FMOD_Studio_Bank_Unload(banks[bankNameString]);
    banks.erase(bankNameString);
    return ERRCHECK(result);
	
}

JNIEXPORT jint JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface_unloadBank(JNIEnv* env, jclass clazz, jstring obj_bankName) {
	char* bankName = (char*)env->GetStringUTFChars(obj_bankName, 0);

	jint JNI_returnValue = wrapped_Java_com_phonygames_pengine_audio_jni_PFmodInterface_unloadBank(env, clazz, obj_bankName, bankName);

	env->ReleaseStringUTFChars(obj_bankName, bankName);

	return JNI_returnValue;
}

static inline jboolean wrapped_Java_com_phonygames_pengine_audio_jni_PFmodInterface_isBankLoaded
(JNIEnv* env, jclass clazz, jstring obj_bankName, char* bankName) {

//@line:139

    std::string bankNameString(bankName);
    if (banks.find(bankNameString) == banks.end()) {
      printf("getBankLoadingState:\tBank %s was never loaded loaded, skipping.\n", bankName);
      return 1;
    }

    FMOD_STUDIO_LOADING_STATE loadingState;
    FMOD_RESULT result = FMOD_Studio_Bank_GetLoadingState(banks[bankNameString], &loadingState);
    return loadingState == FMOD_STUDIO_LOADING_STATE_LOADED;
	
}

JNIEXPORT jboolean JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface_isBankLoaded(JNIEnv* env, jclass clazz, jstring obj_bankName) {
	char* bankName = (char*)env->GetStringUTFChars(obj_bankName, 0);

	jboolean JNI_returnValue = wrapped_Java_com_phonygames_pengine_audio_jni_PFmodInterface_isBankLoaded(env, clazz, obj_bankName, bankName);

	env->ReleaseStringUTFChars(obj_bankName, bankName);

	return JNI_returnValue;
}


//@line:152


  static FMOD_STUDIO_EVENTDESCRIPTION* loadEventDescription(FMOD_STUDIO_SYSTEM *system, const char *pathOrId) {
    std::string pathOrIdString(pathOrId);
    if (eventDescriptions.find(pathOrIdString) != eventDescriptions.end()) {
      return eventDescriptions[pathOrIdString];
    }

    FMOD_STUDIO_EVENTDESCRIPTION *event;
    ERRCHECK(FMOD_Studio_System_GetEvent(system, pathOrId, &event));
    eventDescriptions[pathOrIdString] = event;
    return event;
  }

   static inline jlong wrapped_Java_com_phonygames_pengine_audio_jni_PFmodInterface_getEventInstance
(JNIEnv* env, jclass clazz, jstring obj_pathOrId, char* pathOrId) {

//@line:168

    FMOD_STUDIO_EVENTDESCRIPTION* description = loadEventDescription(studioSystem, pathOrId);
    FMOD_STUDIO_EVENTINSTANCE* instance;
    long instanceId = nextInstanceId;

    ERRCHECK(FMOD_Studio_EventDescription_CreateInstance(description, &instance));
    eventInstances[instanceId] = instance;
    nextInstanceId ++;
    return instanceId;
	
}

JNIEXPORT jlong JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface_getEventInstance(JNIEnv* env, jclass clazz, jstring obj_pathOrId) {
	char* pathOrId = (char*)env->GetStringUTFChars(obj_pathOrId, 0);

	jlong JNI_returnValue = wrapped_Java_com_phonygames_pengine_audio_jni_PFmodInterface_getEventInstance(env, clazz, obj_pathOrId, pathOrId);

	env->ReleaseStringUTFChars(obj_pathOrId, pathOrId);

	return JNI_returnValue;
}

JNIEXPORT void JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface_startPlaybackForEventInstance(JNIEnv* env, jclass clazz, jlong instanceId) {


//@line:179

    ERRCHECK(FMOD_Studio_EventInstance_Start(eventInstances[instanceId]));
  

}

JNIEXPORT void JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface__1_1stopPlaybackForEventInstance(JNIEnv* env, jclass clazz, jlong instanceId, jint stopMode) {


//@line:187

    FMOD_STUDIO_STOP_MODE studioStopMode;
    switch (stopMode) {
      case 0: studioStopMode = FMOD_STUDIO_STOP_ALLOWFADEOUT; break;
      case 1: studioStopMode = FMOD_STUDIO_STOP_IMMEDIATE; break;
      case 2: studioStopMode = FMOD_STUDIO_STOP_FORCEINT; break;
    }
    ERRCHECK(FMOD_Studio_EventInstance_Stop(eventInstances[instanceId], studioStopMode));
  

}

JNIEXPORT void JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface_releaseEventInstance(JNIEnv* env, jclass clazz, jlong instanceId) {


//@line:201

    ERRCHECK(FMOD_Studio_EventInstance_Release(eventInstances[instanceId]));
    eventInstances.erase(instanceId);
  

}

JNIEXPORT jboolean JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface_eventInstanceExists(JNIEnv* env, jclass clazz, jlong instanceId) {


//@line:206

    return eventInstances.find(instanceId) != eventInstances.end();
  

}

JNIEXPORT void JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface_setEventInstancePitch(JNIEnv* env, jclass clazz, jlong instanceId, jfloat pitch) {


//@line:210

    ERRCHECK(FMOD_Studio_EventInstance_SetPitch(eventInstances[instanceId], pitch));
  

}

JNIEXPORT void JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface_setEventInstanceVolume(JNIEnv* env, jclass clazz, jlong instanceId, jfloat volume) {


//@line:214

    ERRCHECK(FMOD_Studio_EventInstance_SetVolume(eventInstances[instanceId], volume));
  

}

JNIEXPORT void JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface_setEventInstancePaused(JNIEnv* env, jclass clazz, jlong instanceId, jboolean paused) {


//@line:218

    ERRCHECK(FMOD_Studio_EventInstance_SetPaused(eventInstances[instanceId], paused ? 1 : 0));
  

}

JNIEXPORT void JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface__1_1set3dListenerTransform(JNIEnv* env, jclass clazz, jint index, jfloat posx, jfloat posy, jfloat posz, jfloat velx, jfloat vely, jfloat velz, jfloat forx, jfloat fory, jfloat forz, jfloat upx, jfloat upy, jfloat upz) {


//@line:223

    FMOD_3D_ATTRIBUTES outAttributes;
    ERRCHECK(FMOD_Studio_System_GetListenerAttributes(studioSystem, index, &outAttributes, nullptr));
    outAttributes.position.x = posx;
    outAttributes.position.y = posy;
    outAttributes.position.z = posz;
    outAttributes.velocity.x = velx;
    outAttributes.velocity.y = vely;
    outAttributes.velocity.z = velz;
    outAttributes.forward.x = forx;
    outAttributes.forward.y = fory;
    outAttributes.forward.z = forz;
    outAttributes.up.x = upx;
    outAttributes.up.y = upy;
    outAttributes.up.z = upz;
    ERRCHECK(FMOD_Studio_System_SetListenerAttributes(studioSystem, index, &outAttributes, nullptr));
  

}

JNIEXPORT void JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface__1_1setEventInstance3dTransform(JNIEnv* env, jclass clazz, jlong instanceId, jfloat posx, jfloat posy, jfloat posz, jfloat velx, jfloat vely, jfloat velz, jfloat forx, jfloat fory, jfloat forz, jfloat upx, jfloat upy, jfloat upz) {


//@line:242

    FMOD_3D_ATTRIBUTES outAttributes;
    ERRCHECK(FMOD_Studio_EventInstance_Get3DAttributes(eventInstances[instanceId], &outAttributes));
    outAttributes.position.x = posx;
    outAttributes.position.y = posy;
    outAttributes.position.z = posz;
    outAttributes.velocity.x = velx;
    outAttributes.velocity.y = vely;
    outAttributes.velocity.z = velz;
    outAttributes.forward.x = forx;
    outAttributes.forward.y = fory;
    outAttributes.forward.z = forz;
    outAttributes.up.x = upx;
    outAttributes.up.y = upy;
    outAttributes.up.z = upz;
    ERRCHECK(FMOD_Studio_EventInstance_Set3DAttributes(eventInstances[instanceId], &outAttributes));
  

}

JNIEXPORT void JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface__1_1setEventInstance3dPosition(JNIEnv* env, jclass clazz, jlong instanceId, jfloat posx, jfloat posy, jfloat posz) {


//@line:260

    FMOD_3D_ATTRIBUTES outAttributes;
    ERRCHECK(FMOD_Studio_EventInstance_Get3DAttributes(eventInstances[instanceId], &outAttributes));
    outAttributes.position.x = posx;
    outAttributes.position.y = posy;
    outAttributes.position.z = posz;
    ERRCHECK(FMOD_Studio_EventInstance_Set3DAttributes(eventInstances[instanceId], &outAttributes));
  

}

JNIEXPORT void JNICALL Java_com_phonygames_pengine_audio_jni_PFmodInterface_setEventInstanceParameterByName(JNIEnv* env, jclass clazz, jlong instanceId, jstring obj_parameterName, jfloat value, jboolean ignoreSeekSpeed) {
	char* parameterName = (char*)env->GetStringUTFChars(obj_parameterName, 0);


//@line:269

    ERRCHECK(FMOD_Studio_EventInstance_SetParameterByName(eventInstances[instanceId], parameterName, value, ignoreSeekSpeed ? 1 : 0));
  
	env->ReleaseStringUTFChars(obj_parameterName, parameterName);

}

