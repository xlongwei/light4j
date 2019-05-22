package com.xlongwei.light4j.util;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.xlongwei.light4j.util.FileUtil.CharsetNames;

import lombok.extern.slf4j.Slf4j;

/**
 * mail util
 * @author xlongwei
 *
 */
@Slf4j
public class MailUtil {

	public static boolean send(String to, String title, String content) {
		log.info("send mail to:{}, title:{}, content:{}", to, title, content);
		if(!StringUtil.isEmail(to) || StringUtil.isBlank(title) || StringUtil.isBlank(content)) {
			return false;
		}
		String from = StringUtil.firstNotBlank(RedisConfig.get("mail.xlongwei.username"), "noreply@xlongwei.com");
		String password = RedisConfig.get("mail.xlongwei.password");
		log.info("send mail from:{}, password is ok:{}", from, !StringUtil.isBlank(password));
		if(StringUtil.isBlank(password)) {
			return false;
		}
		try {
			Properties props = new Properties();
	        props.put("mail.smtp.host", "smtp.exmail.qq.com");
	        props.put("mail.smtp.auth", "true");
	        Session session = Session.getInstance(props);
	        MimeMessage message = new MimeMessage(session);
	        message.setFrom(new InternetAddress(from));
	        message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
	        message.setSubject(title);
	        boolean isHtml = StringUtil.hasLength(StringUtil.getPatternString(content, "<([a-zA-Z]+)>"));
	        if(isHtml) {
	        	message.setContent(content, "text/html;charset=utf-8");
	        }else {
	        	message.setText(content, CharsetNames.UTF_8);
	        }
	        log.info("mail content is html:{}", isHtml);
	        message.setSentDate(new Date());
	        message.saveChanges();
	        Transport transport = session.getTransport("smtp");
	        transport.connect(from, password);
	        transport.sendMessage(message, message.getAllRecipients());
	        transport.close();
	        log.info("mail send success");
	        return true;
		}catch(Exception e) {
			log.warn("fail to send mail, ex: {}", e.getMessage());
		}
		return false;
	}
}
