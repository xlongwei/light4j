package com.xlongwei.light4j.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.sun.speech.freetts.Age;
import com.sun.speech.freetts.Gender;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.audio.AudioPlayer;
import com.sun.speech.freetts.en.us.CMULexicon;
import com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory;

import de.dfki.lt.freetts.en.us.MbrolaVoice;
import lombok.extern.slf4j.Slf4j;

/**
 * freetts util
 * @author xlongwei
 *
 */
@Slf4j
public class FreettsUtil {
	
	/**
	 * @param voiceName kevin16,us1,us2,cn1
	 * @param text 文本（不关心中英文、分词等）
	 * @param speechData true返回语音字节byte[] data，false直接朗读
	 * @return 语音字节或null
	 */
	public static byte[] speak(String voiceName, String text, boolean speechData) {
		if(StringUtil.isBlank(text)) {
			return null;
		}
		Voice voice = voiceName!=null?VOICES.get(voiceName):null;
		if(voice==null) {
			voice = VOICES.get("kevin16");
		}
		if(voice!=null) {
			ByteArrayAudioPlayer audioPlayer = null;
			if(speechData) {
				audioPlayer = new ByteArrayAudioPlayer();
				voice.setAudioPlayer(audioPlayer);
			}
			voice.startBatch();
			voice.speak(text);
			voice.endBatch();
			if(speechData) {
				try{ audioPlayer.close(); } catch(Exception e) { }
				byte[] data = audioPlayer.toByteArray();
				voice.setAudioPlayer(null);
				audioPlayer = null;
				return data;
			}
		}
		return null;
	}
	
	public static void main(String[] args) {
		String hello = "hello world";
		for(String name : VOICES.keySet()) {
			speak(name, hello, false);
		}
		byte[] data = speak(null, hello, true);
		log.info("text: {}, speech data length: {}", hello, data!=null?data.length:0);
	}
	
	private static final Map<String, Voice> VOICES = new HashMap<>();
	static {
		for(Voice voice : new KevinVoiceDirectory().getVoices()) {
			VOICES.put(voice.getName(), voice);
		}
		String base = RedisConfig.get("mbrola.base");
		if(StringUtil.isBlank(base)) {
			base = System.getProperty("mbrola.base");
		} else {
			System.setProperty("mbrola.base", base);
		}
		if(!StringUtil.isBlank(base)) {
			File mbrola = new File(base);
			if(mbrola.exists()) {
				CMULexicon cmuLexicon = new CMULexicon();
				for(File voiceName : mbrola.listFiles()) {
					if(voiceName.isFile() || !new File(voiceName, voiceName.getName()).exists()) {
						continue;
					}
					String name = voiceName.getName();
					Voice voice = new MbrolaVoice(name, name, 150F, 180F, 22F, name, Gender.MALE, Age.YOUNGER_ADULT, null, Locale.US, "general", "mbrola", cmuLexicon);
					VOICES.put(name, voice);
				}
			}
		}
		log.info("available voices: {}", VOICES.keySet());
		for(Voice voice : VOICES.values()) {
			voice.allocate();
		}
		TaskUtil.addShutdownHook(new Runnable() { @Override
		public void run() { for(Voice voice : VOICES.values()) {
			voice.deallocate();
		} }});
	}
	
	/** 获得语音字节内容 */
	@SuppressWarnings({"rawtypes","unchecked"})
	public static class ByteArrayAudioPlayer implements AudioPlayer {
		private AudioFormat currentFormat;
		private byte outputData[];
		private int curIndex = 0;
		private int totBytes = 0;
		private Type outputType = Type.WAVE;
		private Vector outputList = new Vector();		
		private ByteArrayOutputStream baos = new ByteArrayOutputStream();
		/** 返回音频字节码（使用前先调用close()） */
		public byte[] toByteArray() { return baos.toByteArray(); }
		@Override
		public void setAudioFormat(AudioFormat audioformat) {currentFormat = audioformat; }
		@Override
		public AudioFormat getAudioFormat() {return currentFormat; }
		@Override
		public void begin(int size) throws IOException { outputData = new byte[size]; curIndex = 0; }
		@Override
		public boolean end() throws IOException { outputList.add(new ByteArrayInputStream(outputData)); totBytes += outputData.length; return true; }
		@Override
		public boolean write(byte audioData[]) throws IOException { return write(audioData, 0, audioData.length); }
		@Override
		public boolean write(byte bytes[], int offset, int size) throws IOException {
			System.arraycopy(bytes, offset, outputData, curIndex, size);
			curIndex += size;
			return true;
		}
		@Override
		public void close() throws IOException {
			InputStream is = new SequenceInputStream(outputList.elements());
			AudioInputStream ais = new AudioInputStream(is, currentFormat, totBytes / currentFormat.getFrameSize());
			AudioSystem.write(ais, outputType, baos);
		}
		@Override
		public float getVolume() {return 1.0F; }
		@Override
		public void setVolume(float f) { }
		@Override
		public boolean drain() { return true; }
		@Override
		public void startFirstSampleTimer() { }
		@Override
		public long getTime() { return -1L; }
		@Override
		public void resetTime() { }
		@Override
		public void pause() { }
		@Override
		public void resume() { }
		@Override
		public void reset() { }
		@Override
		public void cancel() { }
		@Override
		public void showMetrics() { }
	}
}
