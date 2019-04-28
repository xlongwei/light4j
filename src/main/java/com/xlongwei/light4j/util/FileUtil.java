package com.xlongwei.light4j.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileUtil {
	public static String	unixLineSeparator		= "\n";
	public static String	dosLineSeparator		= "\r\n";
	public static String	defaultlineSeparator		= System.getProperty("line.separator");
	public static String	lineSeparator		= defaultlineSeparator;
	public static String	defaultCharsetName	= Charset.defaultCharset().name();
	public static SSLContext sslContext = null;
	private static Logger logger = LoggerFactory.getLogger(FileUtil.class);
	
	static {
		try {
	        SSLContext sc = SSLContext.getInstance("SSL");  
	        sc.init(null, new TrustManager[] { new X509TrustManager() {
						public void checkClientTrusted(X509Certificate[] arg0,String arg1) throws CertificateException {}
						public void checkServerTrusted(X509Certificate[] arg0,String arg1) throws CertificateException {}
						public X509Certificate[] getAcceptedIssuers() { return null; }
					}
	        }, null);  
	        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {  
				public boolean verify(String urlHostName, SSLSession session) { return true; }  
			});
	        sslContext = sc;
		}catch(Exception e) {
			logger.warn("fail to init https ssl context, ex: {}", e.getMessage());
		}
	}

	/**
	 * @param file 删除文件，或清空并删除目录
	 */
	public static void clear(File file) {
		if(file == null || !file.exists()) return;
		if(file.isFile()) {
			tryDeleteFile(file, 3);
		}else {
			for(File subfile : file.listFiles()) {
				if(subfile.isDirectory()) {
					clear(subfile);
				}else {
					tryDeleteFile(subfile, 3);
				}
			}
			tryDeleteFile(file, 3);
		}
	}
	
	/**
	 * close a closeable ignoring exceptions
	 */
	public static void close(Closeable closeable) {
		if(closeable == null) return;
		try {
			closeable.close();
		} catch (IOException e) {
			logger.warn("fail to close {}, ex: {}", closeable, e.getMessage());
		}
	}
	
	/**
	 * @param in source stream to pull byte from
	 * @param out target stream to push byte to
	 */
	public static void copyStream(InputStream in, OutputStream out) throws IOException {
		int bufSize = 4 * 1024, bytesRead = -1;
		byte[] buf = new byte[bufSize];
		while((bytesRead = in.read(buf, 0, bufSize)) != -1) {
			out.write(buf, 0, bytesRead);
		}
	}
	
	/** 下载url到文件target */
	public static boolean down(String url, File target) {
		InputStream in = null; OutputStream out = null;
		try {
			in = stream(url);
			if(!target.getParentFile().exists())
				target.getParentFile().mkdirs();
			out = new FileOutputStream(target);
			copyStream(in, out);
			return true;
		}catch(Exception e) {
			return false;
		}finally {
			close(out);
		}
	}
	
	/** 获取网址字节内容 */
	public static byte[] bytes(String url) {
		InputStream in = stream(url);
		if(in != null) {
			ByteArrayOutputStream baos = readStream(in);
			if(baos != null) return baos.toByteArray();
		}
		return null;
	}
	
	/** 获取网址字节流 */
	public static InputStream stream(String url) {
		try {
			HttpURLConnection con = (HttpURLConnection)new URL(url).openConnection();
			if(con.getResponseCode()==301) return stream(con.getHeaderField("Location"));
			return con.getInputStream();
		}catch(Exception e) {
			logger.warn("fail to get stream of url: {}, ex: {}", url, e.getMessage());
		}
		return null;
	}
	
	/**
	 * 根据文件大小自动调用合适的复制方法
	 */
	public static boolean copyFile(File in, File out) {
		boolean state = false;
		long mbs = in.length() / 1024 / 1024;
		if(mbs <= 3) {
			state = copySmallFile(in, out);
		}else if(mbs <= 1024) {
			state = copyChannelFile(in, out);
			if(!state) state = copyBigFile(in, out);
		}else {
			state = copyBigFile(in, out);
		}
		return state;
	}
	
	/**
	 * 复制3M以下的小文件
	 */
	public static boolean copySmallFile(File in, File out) {
		if(!in.exists() || !in.isFile()) return false;
		if(out.exists()) out.delete();
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(in);
			fos= new FileOutputStream(out);
			copyStream(fis, fos);
			return true;
		}catch(IOException e) {
			logger.warn("fail to copy small file: {} to: {}, ex: {}", in, out, e.getMessage());
			return false;
		}finally {
			close(fis);
			close(fos);
		}
	}
	
	/**
	 * 快速复制3M至1G的大文件
	 */
	public static boolean copyChannelFile(File srcFile, File dstFile) {
		try(
				FileInputStream fin = new FileInputStream(srcFile);
				FileOutputStream fout = new FileOutputStream(dstFile);
				FileChannel srcChannel = fin.getChannel();
				FileChannel dstChannel = fout.getChannel();
		){
			dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
			return true;
		} catch (Exception e) {
			logger.warn("fail to copy channel file: {} to: {}, ex: {}", srcFile, dstFile, e.getMessage());
			return false;
		}
	}
	
	/**
	 * 复制1G以上的大文件
	 */
	public static boolean copyBigFile(File in, File out) {
		if(!in.exists() || !in.isFile()) return false;
		if(out.exists()) out.delete();
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(in);
			fos= new FileOutputStream(out);
			int bufSize = 4 * 1024 * 1024, bytesRead = -1;
			byte[] buf = new byte[bufSize];
			while((bytesRead = fis.read(buf, 0, bufSize)) != -1) {
				fos.write(buf, 0, bytesRead);
			}
			return true;
		}catch(IOException e) {
			logger.warn("fail to copy big file: {} to: {}, ex: {}", in, out, e.getMessage());
			return false;
		}finally {
			close(fis);
			close(fos);
		}
	}
	
	/**
	 * @param in source directory to pull file from
	 * @param out target directory to push file to
	 */
	public static void copyDir(File in, File out) {
		if(!in.exists() || !in.isDirectory()) return;
		if(!out.exists()) out.mkdirs();
		for(File file : in.listFiles()) {
			File target = new File(out, file.getName());
			if(file.isFile()) {
				copyFile(file, target);
			}else {
				copyDir(file, target);
			}
		}
	}
	
	/**
	 * @return true only if file exist and deleted.
	 */
	public static boolean tryDeleteFile(File file, int tryCount) {
		if(!file.exists()) return false;
		boolean deleteSucceed = false;
		while(!deleteSucceed && tryCount-- > 0) {
			deleteSucceed = file.delete();
			if(deleteSucceed) {
				return true;
			}else {
				nap(100);
			}
		}
		
		if(deleteSucceed==false) logger.info("fail to delete file: {}, try count: {}", file, tryCount);
		return deleteSucceed;
	}
	
	/**
	 * rename oldFile to newFile
	 * @return false if failed or either file is direcrory, true if succeedded
	 */
	public static boolean tryRenameTo(File oldFile, File newFile, int tryCount) {
		if(!oldFile.exists() || oldFile.isDirectory() || newFile.isDirectory()) {
			return false;
		}
		if(newFile.exists()) {
			boolean deleteSucceed = tryDeleteFile(newFile, tryCount);
			if(deleteSucceed == false) return false;
		}
		boolean renameSucceed = false;
		while(!renameSucceed && tryCount-- > 0) {
			renameSucceed = oldFile.renameTo(newFile);
			if(renameSucceed) {
				return true;
			}else {
				nap(100);
			}
		}
		if(renameSucceed==false) logger.info("fail to rename file: {} to: {}, try count: {}", oldFile, newFile, tryCount);
		return renameSucceed;
	}
	
	/**
	 * try copy file to do the best.
	 */
	public static boolean tryCopyFile(File source, File target, int tryCount) {
		if(!source.exists() || source.isDirectory() || target.isDirectory()) {
			return false;
		}
		boolean copySucceed = false;
		while(!copySucceed && tryCount-- > 0) {
			if(target.exists() && target.delete() == false) {
				nap(100);
				continue;
			}else {
				copySucceed = copyFile(source, target);
				if(copySucceed) {
					return true;
				}else {
					nap(100);
				}
			}
		}
		
		if(copySucceed==false) logger.info("fail to copy file: {} to: {}, try count: {}", source, target, tryCount);
		return copySucceed;
	}
	
	/**
	 * @param milliseconds take a little nap
	 */
	public static void nap(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		}catch(Exception e) {
			logger.warn("fail to nap millis: {}, ex: {}", milliseconds, e.getMessage());
		}
	}
	
	/**
	 * @param textfile
	 * @param charsetName
	 * @return list of text lines from File textfile of Charset charsetName
	 */
	public static List<String> readLines(File textfile, String charsetName) {
		List<String> lines = new LinkedList<String>();
		String content = readString(textfile, charsetName);
		if(content != null) {
			StringTokenizer tokens = new StringTokenizer(content, "\r\n");
			while(tokens.hasMoreTokens()) {
				String token = tokens.nextToken();
				if(token.length() > 0) lines.add(token);
			}
		}
		return lines;
	}
	
	public static List<String> readLines(InputStream inputStream, String charsetName) {
		List<String> lines = new LinkedList<String>();
		String content = readString(inputStream, charsetName);
		if(content != null) {
			StringTokenizer tokens = new StringTokenizer(content, "\r\n");
			while(tokens.hasMoreTokens()) {
				String token = tokens.nextToken();
				if(token.length() > 0) lines.add(token);
			}
		}
		return lines;
	}
	
	/** 读取二进制文件到对象 */
	public static Object[] readObject(File file) {
		List<Object> objs = new ArrayList<>();
		try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))){
			Object obj = null;
			while((obj=in.readObject())!=null) objs.add(obj);
		}catch(EOFException e) {
			//正常
		}catch(Exception e) {
			logger.warn("fail to read object file: {}", e.getMessage());
		}
		return objs.toArray();
	}
	
	/**
	 * @return byte stream of file
	 */
	public static ByteArrayOutputStream readStream(File file) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			copyStream(fis, baos);
		}
		catch (IOException e) {
			logger.warn("fail to read stream from file: {}, ex: {}", file, e.getMessage());
		}finally {
			close(fis);
		}
		return baos;
	}
	
	/**
	 * @return byte stream of input stream
	 */
	public static ByteArrayOutputStream readStream(InputStream in) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			copyStream(in, baos);
		}catch(Exception e) {
			logger.warn("fail to read stream from inputstream: {}, ex: {}", in, e.getMessage());
		}finally {
			close(in);
		}
		return baos;
	}
	
	/**
	 * @return content String of File textfile
	 */
	public static String readString(File textfile, String charsetName) {
		String content = null;
		ByteArrayOutputStream baos = readStream(textfile);
		try {
			content = new String(baos.toByteArray(), charsetName);
		} catch (IOException e) {
			logger.warn("fail to read string from file: {}, charset: {}, ex: {}", textfile, charsetName, e.getMessage());
		}
		return content;
	}
	
	/**
	 * @return content String of InputStream inputStream in charsetName
	 */
	public static String readString(InputStream inputStream, String charsetName) {
		String content = null;
		ByteArrayOutputStream baos = readStream(inputStream);
		try {
			content = new String(baos.toByteArray(), charsetName);
		} catch (IOException e) {
			logger.warn("fail to read string from inputstream: {}, charset: {}, ex: {}", inputStream, charsetName, e.getMessage());
		}
		return content;
	}
	
	/**
	 * @param algorithm one of [SHA, MD5, MD2, SHA-256, SHA-384, SHA-512]
	 * @return digest of a file
	 */
	public static String digest(File file, String algorithm) {
		byte[] digest = null;
		if(file.exists() && file.isFile()) {
			FileInputStream fis = null;
			try {
				MessageDigest md = MessageDigest.getInstance(algorithm);
				fis = new FileInputStream(file);
				int bufSize = 4 * 1024, bytesRead = -1;
				byte[] buf = new byte[bufSize];
				while((bytesRead = fis.read(buf, 0, bufSize)) != -1) {
					md.update(buf, 0, bytesRead);
				}
				digest = md.digest();
			} catch (Exception e) {
				logger.warn("fail to digest file: {}, algo: {}, ex: {}", file, algorithm, e.getMessage());
			}finally {
				close(fis);
			}
		}
		return StringUtil.toHexString(digest);
	}
	
	/** 写对象到二进制文件 */
	public static void writeObject(File file, Object ... objs) {
		try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))){
			for(Object obj : objs ) out.writeObject(obj);
		}catch(Exception e) {
			logger.warn("fail to write object to file, ex: {}", e.getMessage());
		}
	}
	
	public static OutputStream writeStream(File file) {
		try {
			File parentFile = file.getParentFile();
			if(!parentFile.exists()) parentFile.mkdirs();
			return new FileOutputStream(file);
		}catch (Exception e) {
			logger.info("fail to open write stream to file: {}, ex: {}", file, e.getMessage());
		}
		return null;
	}
	
	/**
	 * @param file target file to append content to
	 * @param charsetName usually one of [utf-8, utf-16, gbk]
	 */
	public static void writeString(File file, String content, String charsetName) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file, file.exists());
			copyStream(new ByteArrayInputStream(content.getBytes(charsetName)), fos);
		} catch (IOException e) {
			logger.warn("fail to write string to file: {}, charset: {}, ex: {}", file, charsetName, e.getMessage());
		}finally {
			close(fos);
		}
	}
	
	public static void writeStream(File file, InputStream in) {
		OutputStream fos = null;
		try {
			fos = writeStream(file);
			if(fos!=null) copyStream(in, fos);
		} catch (IOException e) {
			logger.warn("fail to write stream to file: {}, ex: {}", file, e.getMessage());
		}finally {
			close(fos);
		}
	}
	
	public static void writeBytes(File file, byte[] bytes) {
		OutputStream fos = null;
		try {
			fos = writeStream(file);
			if(fos!=null) fos.write(bytes);
		} catch (IOException e) {
			logger.warn("fail to write bytes to file: {}, ex: {}", file, e.getMessage());
		}finally {
			close(fos);
		}
	}
	
	/**
	 * unzip file zip to parent directory
	 */
	public static void unZip(File zip, File parent) {
		if(!zip.exists() || !zip.isFile()) return;
		ZipInputStream zis = null;
		ZipEntry ze = null;
		try {
			zis = new ZipInputStream(new ByteArrayInputStream(readStream(zip).toByteArray()));
			while((ze = zis.getNextEntry()) != null) {
				File target = new File(parent, ze.getName());
				if(ze.isDirectory()) {
					target.mkdirs();
				}else {
					FileOutputStream fos = null;
					try {
						fos = new FileOutputStream(target);
						copyStream(zis, fos);
					}catch(IOException e) {
						logger.warn("fail to unzip file: {}, entry: {}, ex: {}", zip, ze.getName(), e.getMessage());
					}finally {
						close(fos);
					}
				}
			}
		} catch (IOException e) {
			logger.warn("fail to unzip file: {}, ex: {}", zip, e.getMessage());
		}finally {
			close(zis);
		}
	}
	
	/**
	 * zip file to zip
	 */
	public static void zip(File file, File zip) {
		if(!file.exists()) return;
		ZipOutputStream zos = null;
		File current = null;
		try {
			zos = new ZipOutputStream(new FileOutputStream(zip));
			zos.setLevel(Deflater.BEST_COMPRESSION);
			Queue<File> files = new LinkedList<File>();
			files.add(file);
			int pos = file.getCanonicalFile().getParent().length() + 1;
			while((current = files.poll()) != null) {
				if(current.isFile()) {
					zos.putNextEntry(new ZipEntry(current.getCanonicalPath().substring(pos)));
					InputStream is = new ByteArrayInputStream(readStream(current).toByteArray());
					copyStream(is, zos);
					zos.closeEntry();
				}else {
					zos.putNextEntry(new ZipEntry(current.getCanonicalPath().substring(pos) + "/"));
					zos.closeEntry();
					for(File f : current.listFiles()) {
						files.add(f);
					}
				}
			}
		}catch(IOException e) {
			logger.warn("fail to zip file: {} to: {}, ex: {}", file, zip, e.getMessage());
		}finally {
			close(zos);
		}
	}
	
	/**
	 * get file name without extension.
	 */
	public static String getFileName(File file) {
		String name = file.getName();
		int lastIndexOfDot = name.lastIndexOf('.');
		return lastIndexOfDot != -1 ? name.substring(0, lastIndexOfDot) : name;
	}
	
	public static String getFileName(String name) {
		return getFileName(new File(name));
	}
	
	/**
	 * get file extension.
	 */
	public static String getFileExt(File file) {
		return getFileExt(file.getName());
	}
	
	public static String getFileExt(String name) {
		int lastIndexOfDot = name.lastIndexOf('.');
		return lastIndexOfDot != -1 ? name.substring(lastIndexOfDot + 1) : "";
	}
	
	/** get file names in dir that matches filter */
	public static List<String> getFileNames(File dir, ExtFileFilter filter) {
		List<String> fileNames = new ArrayList<>();
		if(!dir.exists() || !dir.isDirectory()) return fileNames;
		File[] files = dir.listFiles(filter);
		for(File file : files) {
			fileNames.add(file.getName());
		}
		return fileNames;
	}
	
	/**
	 * get string key-value map from text file.
	 */
	public static Map<String, String> getStringMap(File file, String charsetName){
		Map<String, String> stringMap = new HashMap<String, String>();
		TextReader textReader = new TextReader();
		textReader.open(file, charsetName);
		String key = null, value = null;
		while((key = textReader.read()) != null && (value = textReader.read()) != null) {
			stringMap.put(key, value);
		}
		textReader.close();
		return stringMap;
	}
	
	public static class CharsetNames {
		public static String UTF_8 = "UTF-8";	//变长字节通用性强的国际编码
		public static String US_ASCII = "US-ASCII";	//7 bytes for alphabets, numbers and punctuations
		public static String ISO_88591 = "ISO-8859-1";	//http响应内容编码
		public static String GB2312 = "GB2312";	//区位码，可推算出汉字拼音
		public static String GBK = "GBK";		//兼容区位码，添加了CJK汉字等
		public static String GB18030 = "GB18030";//unicode 3.1，包括少数民族字符等
	}
	public static class Charsets {
		public static final Charset UTF_8 = Charset.forName("UTF-8");
		public static final Charset US_ASCII = Charset.forName("US-ASCII");
	    public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
	    public static final Charset GB2312 = Charset.forName("GB2312");
	    public static final Charset GBK = Charset.forName("GBK");
	    public static final Charset GB18030 = Charset.forName("GB18030");
	    public static Charset forName(String charset, Charset defVal) {
	    	try {
	    		Charset cs = charset!=null ? Charset.forName(charset) : null;
	    		if(cs != null) return cs;
	    	}catch(Exception e) {}
	    	return defVal;
	    }
	}
	public static class TextReader {
		private Scanner	in;
		public TextReader() {}
		public TextReader(File file) { open(file); }
		public TextReader(File file, String charsetName) { open(file, charsetName); }
		public TextReader(InputStream is) { open(is); }
		public TextReader(InputStream is, String charsetName) { open(is, charsetName); }
		public TextReader(Reader reader) { open(reader); }
		public TextReader(String path) { open(path); }
		public TextReader(String path, String charsetName) { open(path, charsetName); }
		
		public void open(File file) { open(file, defaultCharsetName); }
		public void open(File file, String charsetName) {
			try {
				close();
				in = new Scanner(file, charsetName);
			} catch (FileNotFoundException e) {
				logger.warn("fail to open file: {}, charset: {}, ex: {}", file, charsetName, e.getMessage());
			}
		}
		public void open(InputStream is) { open(is, defaultCharsetName); }
		public void open(InputStream is, String charsetName) {
			close();
			in = new Scanner(new BufferedInputStream(is), charsetName);
		}
		public void open(Reader reader) {
			close();
			in = new Scanner(new BufferedReader(reader));
		}
		public void open(String path) { open(path, defaultCharsetName); }
		public void open(String path, String charsetName) { open(new File(path), charsetName); }

		public void close() { FileUtil.close(in); }
		
		public String read() {
			if (in != null) {
				try {
					return in.nextLine();
				} catch (Exception e) { }
			}
			return null;
		}
		
		public String readAsString(File file) {
			open(file);
			return readAsString();
		}
		public String readAsString(String path) {
			open(path);
			return readAsString();
		}
		public String readAsString(InputStream is) {
			open(is);
			return readAsString();
		}
		public String readAsString() {
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = read()) != null) {
				sb.append(line + "\n");
			}
			close();
			return sb.toString();
		}
	}
	
	public static class TextWriter {
		private PrintWriter	out;
		private File file;
		private String charsetName = defaultCharsetName;
		private String lineSeparator = defaultlineSeparator;
		private boolean append = false, autoFlush = false;

		public TextWriter() {}
		public TextWriter(File file) { open(file, charsetName, append, autoFlush); }
		public TextWriter(File file, boolean append) { open(file, charsetName, append, autoFlush); }
		public TextWriter(File file, String charsetName) { open(file, charsetName, append, autoFlush); }
		public TextWriter(File file, String charsetName, boolean append) { open(file, charsetName, append, autoFlush); }
		public TextWriter(File file, String charsetName, boolean append, boolean autoFlush) { open(file, charsetName, append, autoFlush); }
		public TextWriter(OutputStream os) { open(os); }
		public TextWriter(OutputStream os, String charsetName) { open(os, charsetName); }
		public TextWriter(OutputStream os, String charsetName, boolean autoFlush) { open(os, charsetName, autoFlush); }
		public TextWriter(String path) { open(path); }
		public TextWriter(String path, boolean append) { this(new File(path), append); }
		public TextWriter(String path, String charsetName) { this(new File(path), charsetName); }
		public TextWriter(String path, String charsetName, boolean append) { this(new File(path), charsetName, append); }
		public TextWriter(String path, String charsetName, boolean append, boolean autoFlush) { this(new File(path), charsetName, append, autoFlush); }
		public TextWriter(Writer writer) { close(); out = new PrintWriter(new BufferedWriter(writer)); };
		public void open(File file) { open(file, defaultCharsetName, false); }
		public void open(File file, boolean append) { open(file, defaultCharsetName, append); }
		public void open(File file, String charsetName) { open(file, charsetName, false); }
		public void open(File file, String charsetName, boolean append) { open(file, charsetName, append, false); }
		public void open(OutputStream os) { open(os, defaultCharsetName); }
		public void open(OutputStream os, String charsetName) { open(os, charsetName, false); }
		public void open(String path) { open(path, defaultCharsetName, false); }
		public void open(String path, boolean append) { open(path, defaultCharsetName, append); }
		public void open(String path, String charsetName) { open(path, charsetName, false); }
		public void open(String path, String charsetName, boolean append) { open(new File(path), charsetName, append); }
		public void open(File file, String charsetName, boolean append, boolean autoFlush) {
			close();
			try {
				out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), charsetName)), autoFlush);
			} catch (IOException e) {
				logger.warn("fail to open write file: {}, charset: {}, append: {}, ex: {}", file, charsetName, append, e.getMessage());
			}
		}
		public void open(OutputStream os, String charsetName, boolean autoFlush) {
			close();
			try {
				out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, charsetName)), autoFlush);
			} catch (IOException e) {
				logger.warn("fail to open write stream: {}, charset: {}, flush: {}, ex: {}", file, charsetName, autoFlush, e.getMessage());
			}
		}
		
		public void setFile(File file) { this.file = file; }
		public void setFile(String path) { file = new File(path); }
		public void open() { close(); if(file != null) { open(file, charsetName, append, autoFlush); } }
		
		public void setCharsetName(String charsetName) { this.charsetName = charsetName; }
		public void setAppend(boolean append) { this.append = append; }
		public void setAutoFlush(boolean autoFlush) { this.autoFlush = autoFlush; }
		public TextWriter setLineSeparator(String lineSeparator) { this.lineSeparator = lineSeparator; return this; }

		public void close() { if (out != null) { out.close(); out = null; file = null; } }
		public void flush() { if (out != null) { out.flush(); } }
		
		/**
		 * write str to file, no close it, you can write many times.
		 */
		public void write(String str) { if (out != null) { out.print(str); } }
		public void writeln() { write(lineSeparator); }
		public void writeln(String str) { write(str + lineSeparator); }
		
		/**
		 * write content to file, clear it ahead.
		 */
		public void writeFile(File file, String content) { open(file); write(content); close(); }
		public void writeFile(String path, String content) { writeFile(new File(path), content); }
		
		/**
		 * append content to file, no line separator.
		 */
		public void appendFile(File file, String appendContent) { open(file, true); write(appendContent); close(); }
		public void appendFile(String path, String appendContent) { appendFile(new File(path), appendContent); }
		
		/**
		 * append line to file which will be opened and closed automatically.
		 */
		public void appendlnFile(File file, String appendLine) { appendFile(file, appendLine + lineSeparator); }
		public void appendlnFile(String path, String appendLine) { appendFile(new File(path), appendLine + lineSeparator); }
	}
	
	public static class ExtFileFilter extends javax.swing.filechooser.FileFilter implements FileFilter {
		private boolean acceptDirectory = true;
		
		public ExtFileFilter() {}
		public ExtFileFilter(String extension) { addExtension(extension); }
		public ExtFileFilter(String extension, String description) { addExtension(extension); setDescription(description); }
		
		@Override
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return acceptDirectory;
			}
			for (String extension : extensions) {
				if (f.getName().toLowerCase().endsWith(extension)) {
					return true;
				}
			}
			return extensions.size() == 0;
		}
		
		public boolean accept(File dir, String name) {
			File f = new File(dir, name);
			if (f.isDirectory()) {
				return acceptDirectory;
			}
			for (String extension : extensions) {
				if (name.toLowerCase().endsWith(extension)) {
					return true;
				}
			}
			return extensions.size() == 0;
		}
		
		public ExtFileFilter addExtension(String extension) {
			if(StringUtil.isBankCardNumber(extension)) return this;
			String[] exts = extension.split("[,;]");
			for (String ext : exts) {
				if ((ext == null) || (ext.trim().length() == 0)) {
					continue;
				}
				ext = ext.trim().toLowerCase();
				if (ext.startsWith("*")) {
					ext = ext.substring(1);
				}
				if (!ext.startsWith(".")) {
					ext = "." + ext;
				}
				if (!extensions.contains(ext)) {
					extensions.add(ext);
				}
			}
			return this;
		}
		
		public ExtFileFilter removeExtension(String extension) {
			String[] exts = extension.split("[,;]");
			for (String ext : exts) {
				if ((ext == null) || (ext.trim().length() == 0)) {
					continue;
				}
				ext = ext.trim().toLowerCase();
				if (ext.startsWith("*")) {
					ext = ext.substring(1);
				}
				if (!ext.startsWith(".")) {
					ext = "." + ext;
				}
				if (extensions.contains(ext)) {
					extensions.remove(ext);
				}
			}
			return this;
		}
		
		public ExtFileFilter setExtension(String extension) {
			extensions.clear();
			return addExtension(extension);
		}
		
		public List<String> getExtention(){
			return extensions;
		}
		
		@Override
		public String getDescription() {
			return description;
		}
		
		public ExtFileFilter setDescription(String description) {
			this.description = description;
			return this;
		}
		
		public ExtFileFilter acceptDirectory(boolean acceptDirectory) {
			this.acceptDirectory = acceptDirectory;
			return this;
		}
		
		private String description = null;
		private List<String> extensions = new LinkedList<String>();
		
		public static final ExtFileFilter images = new ExtFileFilter("jpg,jpeg,png,gif").acceptDirectory(false);
		public static final ExtFileFilter audios = new ExtFileFilter("mp3,wav,ogg").acceptDirectory(false);
		public static final ExtFileFilter videos = new ExtFileFilter("mp4,ogv,webm").acceptDirectory(false);
		public static final ExtFileFilter folders = new ExtFileFilter("none").acceptDirectory(true);
		public static final ExtFileFilter files = new ExtFileFilter("").acceptDirectory(false);
		public FilenameFilter filenameFilter() {
			return new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return ExtFileFilter.this.accept(dir, name);
				}
			};
		}
	}
	public static Comparator<File> fileComparator = new Comparator<File>() {
		public int compare(File o1, File o2) {
			if(o1==null) return o2==null ? 0 : -1;
			else if(o2==null) return 1;
			return fileNameComparator.compare(o1.getName(), o2.getName());
		}
	};
	public static Comparator<String> fileNameComparator = new Comparator<String>() {
		public int compare(String o1, String o2) {
			if(o1==null) return o2==null ? 0 : -1;
			else if(o2==null) return 1;
			int len1=o1.length(), len2 = o2.length(), len = Math.min(len1, len2);
			for(int i=0; i<len; i++) {
				char c1 = o1.charAt(i), c2 = o2.charAt(i);
				if(c1 == c2) continue;
				if(StringUtil.isDigit(c1) && StringUtil.isDigit(c2)) {
					StringBuilder num1 = new StringBuilder().append(c1), num2 = new StringBuilder().append(c2);
					int j = i+1; char c;
					while(j<len1 && StringUtil.isDigit(c=o1.charAt(j++))) num1.append(c);
					j = i+1;
					while(j<len2 && StringUtil.isDigit(c=o2.charAt(j++))) num2.append(c);
					int n1 = Integer.parseInt(num1.toString()), n2 = Integer.parseInt(num2.toString());
					if(n1 != n2) return n1 > n2 ? 1 : -1;
					i += Math.min(num1.length(), num2.length()) - 1;
				}else if(StringUtil.isChinese(c1) || StringUtil.isChinese(c2)) {
					String p1 = PinyinUtil.getPinyin(c1), p2 = PinyinUtil.getPinyin(c2);
					if(p1!=null && !p1.equals(p2)) return p1.compareTo(p2);
				}else return c1 > c2 ? 1 : -1;
			}
			return 0;
		}
	};
}
