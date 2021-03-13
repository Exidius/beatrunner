#ifndef Header_CrossExample
#define Header_CrossExample

#include <math.h>
#include <pthread.h>
#include <SuperpoweredAdvancedAudioPlayer.h>
#include <SuperpoweredFilter.h>
#include <SuperpoweredRoll.h>
#include <SuperpoweredFlanger.h>
#include <SuperpoweredSimple.h>
#include <SuperpoweredAnalyzer.h>
#include <OpenSource/SuperpoweredAndroidAudioIO.h>

#define HEADROOM_DECIBEL 3.0f
static const float headroom = powf(10.0f, -HEADROOM_DECIBEL * 0.05f);

class CrossExample {
public:

	CrossExample();
	~CrossExample();

	static float decode(const char *path);

private:

};

#endif
