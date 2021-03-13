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

class TempoFetcher {
public:

	TempoFetcher();
	~TempoFetcher();

	static float decode(const char *path);

private:

};

#endif
