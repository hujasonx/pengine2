package com.phonygames.pengine.audio.jni;

public class PFmodInterface {
  private static boolean initalized = false;

  /*JNI
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

   */

  public static native int __init(); /*
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
	*/

  public static boolean init() {
    if (initalized) {
      return true;
    }

    __init();
    initalized = true;
    return true;
  }

  public static boolean isInitalized() {
    return initalized;
  }

  public static native int __close(); /*
    FMOD_RESULT result = FMOD_Studio_System_Release(studioSystem);
    return ERRCHECK(result);
	*/

  public static boolean close() {
    if (!initalized) {
      return false;
    }

    initalized = false;
    __close();
    return true;
  }

  public static native int __update(); /*
    FMOD_RESULT result = FMOD_Studio_System_Update(studioSystem);
    return ERRCHECK(result);
	*/

  public static boolean update() {
    if (!initalized) {
      return false;
    }

    __update();
    return true;
  }

  public static native void setSystemParameterByName(String parameterName, float value, boolean ignoreSeekSpeed); /*
    ERRCHECK(FMOD_Studio_System_SetParameterByName(studioSystem, parameterName, value, ignoreSeekSpeed ? 1 : 0));
  */

  public static native int loadBank(String bankName); /*
    std::string bankNameString(bankName);
    if (banks.find(bankNameString) != banks.end()) {
      printf("loadBank:\tBank %s already loaded, skipping.\n", bankName);
      return 1;
    }

    FMOD_STUDIO_BANK* bank;
    FMOD_RESULT result = FMOD_Studio_System_LoadBankFile(studioSystem, bankName, FMOD_STUDIO_LOAD_BANK_NORMAL, &bank);
    banks[bankNameString] = bank;
    return ERRCHECK(result);
	*/

  public static native int unloadBank(String bankName); /*
    std::string bankNameString(bankName);
    if (banks.find(bankNameString) == banks.end()) {
      printf("unloadBank:\tBank %s was never loaded loaded, skipping.\n", bankName);
      return 1;
    }

    FMOD_RESULT result = FMOD_Studio_Bank_Unload(banks[bankNameString]);
    banks.erase(bankNameString);
    return ERRCHECK(result);
	*/

  public static native boolean isBankLoaded(String bankName); /*
    std::string bankNameString(bankName);
    if (banks.find(bankNameString) == banks.end()) {
      printf("getBankLoadingState:\tBank %s was never loaded loaded, skipping.\n", bankName);
      return 1;
    }

    FMOD_STUDIO_LOADING_STATE loadingState;
    FMOD_RESULT result = FMOD_Studio_Bank_GetLoadingState(banks[bankNameString], &loadingState);
    return loadingState == FMOD_STUDIO_LOADING_STATE_LOADED;
	*/


  /*JNI

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

   */

  public static native long getEventInstance(String pathOrId); /*
    FMOD_STUDIO_EVENTDESCRIPTION* description = loadEventDescription(studioSystem, pathOrId);
    FMOD_STUDIO_EVENTINSTANCE* instance;
    long instanceId = nextInstanceId;

    ERRCHECK(FMOD_Studio_EventDescription_CreateInstance(description, &instance));
    eventInstances[instanceId] = instance;
    nextInstanceId ++;
    return instanceId;
	*/

  public static native void startPlaybackForEventInstance(long instanceId); /*
    ERRCHECK(FMOD_Studio_EventInstance_Start(eventInstances[instanceId]));
  */

  public static enum FMOD_STUDIO_STOP_MODE {
    FMOD_STUDIO_STOP_ALLOWFADEOUT, FMOD_STUDIO_STOP_IMMEDIATE, FMOD_STUDIO_STOP_FORCEINT
  }

  public static native void __stopPlaybackForEventInstance(long instanceId, int stopMode); /*
    FMOD_STUDIO_STOP_MODE studioStopMode;
    switch (stopMode) {
      case 0: studioStopMode = FMOD_STUDIO_STOP_ALLOWFADEOUT; break;
      case 1: studioStopMode = FMOD_STUDIO_STOP_IMMEDIATE; break;
      case 2: studioStopMode = FMOD_STUDIO_STOP_FORCEINT; break;
    }
    ERRCHECK(FMOD_Studio_EventInstance_Stop(eventInstances[instanceId], studioStopMode));
  */

  public static void stopPlaybackForEventInstance(long instanceId, FMOD_STUDIO_STOP_MODE stopMode) {
    __stopPlaybackForEventInstance(instanceId, stopMode.ordinal());
  }

  public static native void releaseEventInstance(long instanceId); /*
    ERRCHECK(FMOD_Studio_EventInstance_Release(eventInstances[instanceId]));
    eventInstances.erase(instanceId);
  */

  public static native boolean eventInstanceExists(long instanceId); /*
    return eventInstances.find(instanceId) != eventInstances.end();
  */

  public static native void setEventInstancePitch(long instanceId, float pitch); /*
    ERRCHECK(FMOD_Studio_EventInstance_SetPitch(eventInstances[instanceId], pitch));
  */

  public static native void setEventInstanceVolume(long instanceId, float volume); /*
    ERRCHECK(FMOD_Studio_EventInstance_SetVolume(eventInstances[instanceId], volume));
  */

  public static native void setEventInstancePaused(long instanceId, boolean paused); /*
    ERRCHECK(FMOD_Studio_EventInstance_SetPaused(eventInstances[instanceId], paused ? 1 : 0));
  */

  public static native void __set3dListenerTransform(int index, float posx, float posy, float posz, float velx, float vely, float velz, float forx, float fory, float forz,
                                                     float upx, float upy, float upz); /*
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
  */

  public static native void __setEventInstance3dTransform(long instanceId, float posx, float posy, float posz, float velx, float vely, float velz, float forx, float fory,
                                                          float forz, float upx, float upy, float upz); /*
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
  */

  public static native void __setEventInstance3dPosition(long instanceId, float posx, float posy, float posz); /*
    FMOD_3D_ATTRIBUTES outAttributes;
    ERRCHECK(FMOD_Studio_EventInstance_Get3DAttributes(eventInstances[instanceId], &outAttributes));
    outAttributes.position.x = posx;
    outAttributes.position.y = posy;
    outAttributes.position.z = posz;
    ERRCHECK(FMOD_Studio_EventInstance_Set3DAttributes(eventInstances[instanceId], &outAttributes));
  */

  public static native void setEventInstanceParameterByName(long instanceId, String parameterName, float value, boolean ignoreSeekSpeed); /*
    ERRCHECK(FMOD_Studio_EventInstance_SetParameterByName(eventInstances[instanceId], parameterName, value, ignoreSeekSpeed ? 1 : 0));
  */


}
