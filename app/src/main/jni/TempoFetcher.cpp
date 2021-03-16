#include "TempoFetcher.h"
#include <Superpowered.h>
#include <SuperpoweredSimple.h>
#include <SuperpoweredCPU.h>
#include <jni.h>
#include <android/log.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_AndroidConfiguration.h>
#include <vector>

TempoFetcher::TempoFetcher()
{
    Superpowered::Initialize(
            "ExampleLicenseKey-WillExpire-OnNextUpdate",
            true, // enableAudioAnalysis (using SuperpoweredAnalyzer, SuperpoweredLiveAnalyzer, SuperpoweredWaveform or SuperpoweredBandpassFilterbank)
            false, // enableFFTAndFrequencyDomain (using SuperpoweredFrequencyDomain, SuperpoweredFFTComplex, SuperpoweredFFTReal or SuperpoweredPolarFFT)
            false, // enableAudioTimeStretching (using SuperpoweredTimeStretching)
            false,  // enableAudioEffects (using any SuperpoweredFX class)
            true,  // enableAudioPlayerAndDecoder (using SuperpoweredAdvancedAudioPlayer or SuperpoweredDecoder)
            false, // enableCryptographics (using Superpowered::RSAPublicKey, Superpowered::RSAPrivateKey, Superpowered::hasher or Superpowered::AES)
            false  // enableNetworking (using Superpowered::httpRequest)
    );
}


float TempoFetcher::decode(const char *path) {

    auto *decoder = new Superpowered::Decoder();

    decoder->open(path,false,false,0,0,nullptr);

    auto *intBuffer = (short int *)malloc(decoder->getFramesPerChunk() * 2 * sizeof(short int) + 16384);
    auto *floatBuffer = (float *)malloc(decoder->getFramesPerChunk() * 2 * sizeof(float) + 16384);

    auto *analyzer = new Superpowered::Analyzer(decoder->getSamplerate(),(int)decoder->getDurationSeconds());

    while (true) {
        int framesDecoded = decoder->decodeAudio(intBuffer, decoder->getFramesPerChunk());
        if (framesDecoded < 1) break;

        // Submit the decoded audio to the analyzer.
        Superpowered::ShortIntToFloat(intBuffer, floatBuffer, framesDecoded);
        analyzer->process(floatBuffer, framesDecoded);
    };

    analyzer->makeResults(60,200,0,0,false,0,false,false,false);
    float bpm = analyzer->bpm;

    free(decoder);
    free(analyzer);
    free(intBuffer);
    free(floatBuffer);

    return bpm;
}

TempoFetcher::~TempoFetcher() {

}

static TempoFetcher *example = NULL;

// TempoFetcher - Create the app and initialize the players.
extern "C" JNIEXPORT void
Java_com_barad_beatrunner_data_MusicStore_tempoFetcher(JNIEnv * __unused env, jobject __unused obj) {
    example = new TempoFetcher();
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_barad_beatrunner_data_MusicStore_decode(JNIEnv *env, jobject thiz, jstring audioPath) {
    const char *path = env->GetStringUTFChars(audioPath, JNI_FALSE);
    return example->decode(path);
}