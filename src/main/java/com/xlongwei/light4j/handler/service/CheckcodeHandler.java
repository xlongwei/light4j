package com.xlongwei.light4j.handler.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;

import com.networknt.utility.Tuple;
import com.wf.captcha.ArithmeticCaptcha;
import com.wf.captcha.ChineseCaptcha;
import com.wf.captcha.ChineseGifCaptcha;
import com.wf.captcha.GifCaptcha;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.ArithmeticCaptchaAbstract;
import com.wf.captcha.base.Captcha;
import com.wf.captcha.base.ChineseCaptchaAbstract;
import com.xlongwei.light4j.handler.ServiceHandler.AbstractHandler;
import com.xlongwei.light4j.util.HandlerUtil;
import com.xlongwei.light4j.util.IdWorker;
import com.xlongwei.light4j.util.ImageUtil;
import com.xlongwei.light4j.util.MailUtil;
import com.xlongwei.light4j.util.NumberUtil;
import com.xlongwei.light4j.util.RedisCache;
import com.xlongwei.light4j.util.RedisConfig;
import com.xlongwei.light4j.util.StringUtil;

import cloud.tianai.captcha.generator.ImageCaptchaGenerator;
import cloud.tianai.captcha.generator.common.model.dto.ImageCaptchaInfo;
import cloud.tianai.captcha.generator.impl.MultiImageCaptchaGenerator;
import cloud.tianai.captcha.resource.ImageCaptchaResourceManager;
import cloud.tianai.captcha.resource.impl.DefaultImageCaptchaResourceManager;
import cloud.tianai.captcha.validator.ImageCaptchaValidator;
import cloud.tianai.captcha.validator.impl.BasicCaptchaTrackValidator;
import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ICaptcha;
import cn.hutool.captcha.generator.AbstractGenerator;
import cn.hutool.captcha.generator.MathGenerator;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.math.Calculator;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import io.undertow.server.HttpServerExchange;
import lombok.extern.slf4j.Slf4j;

/**
 * checkcode handler
 * 
 * @author xlongwei
 *
 */
@Slf4j
public class CheckcodeHandler extends AbstractHandler {

	int width = 130, height = 48;
    Field generatorField = null;
    Field codeField = null;
    Field lengthField = null;
	ImageCaptchaGenerator imageCaptchaGenerator;
    ImageCaptchaValidator imageCaptchaValidator;
	
	public CheckcodeHandler() {
		generatorField = ReflectUtil.getField(AbstractCaptcha.class, "generator");
		codeField = ReflectUtil.getField(AbstractCaptcha.class, "code");
		lengthField = ReflectUtil.getField(AbstractGenerator.class, "length");
		ImageCaptchaResourceManager imageCaptchaResourceManager = new DefaultImageCaptchaResourceManager();
        imageCaptchaGenerator = new MultiImageCaptchaGenerator(imageCaptchaResourceManager).init(true);
        imageCaptchaValidator = new BasicCaptchaTrackValidator();
	}

	public void image(HttpServerExchange exchange) throws Exception {
		String checkcode = HandlerUtil.getParam(exchange, "checkcode");
		int type = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "type"), -2);
		String style = HandlerUtil.getParam(exchange, "style");
		if (StringUtil.hasLength(checkcode)) {
			boolean ajax = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "ajax"), true);
			String image = null;
			OutputStream outputStream = ajax ? null : exchange.getOutputStream();
			if (type == -3) {
				Captcha captcha = captcha(style);
				while (captcha instanceof ArithmeticCaptchaAbstract) {
					captcha = captcha(style);
				}
				ReflectUtil.setFieldValue(captcha, "chars", checkcode);
				if (ajax) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					captcha.out(baos);
					image = ImageUtil.encode(baos.toByteArray(), null);
				} else {
					captcha.out(outputStream);
				}
			} else if (type == -4) {
				if ("random".equals(style)) {
					style = iStyles[RandomUtil.randomInt(0, iStyles.length)];
				}
				ICaptcha iCaptcha = iCaptcha(style, width, height);
				ReflectUtil.setFieldValue(iCaptcha, codeField, checkcode);
				if (ajax) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					iCaptcha.write(baos);
					image = ImageUtil.encode(baos.toByteArray(), null);
				} else {
					iCaptcha.write(outputStream);
				}
			} else {
				if (ajax) {
					image = ImageUtil.encode(ImageUtil.bytes(ImageUtil.create(checkcode)), null);
				} else {
					outputStream.write(ImageUtil.bytes(ImageUtil.create(checkcode)));
				}
			}
			if (ajax) {
				HandlerUtil.setResp(exchange, StringUtil.params("image", image));
			} else {
				outputStream.close();
			}
		} else {
			String sid = HandlerUtil.getParam(exchange, "sid");
			if (sid != null && (sid = sid.trim()).length() > 13) {
				int length = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "length"), 4);
				boolean specials = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "specials"), false);
				String[] special = type < -1 ? null : ImageUtil.special(type);
				String code = special != null && special.length > 0 ? special[0] : null;
				String check = null;
				Captcha captcha = null;
				ICaptcha iCaptcha = null;
				if (code == null) {
					if (type == -3) {
						captcha = captcha(style);
						if (!(captcha instanceof ArithmeticCaptchaAbstract)) {
							captcha.setLen(length);
						}
						if (!(captcha instanceof ChineseCaptchaAbstract)) {
							int font = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "font"), 0);
							font = 0 <= font && font <= 9 ? font : (font == -1 ? RandomUtil.randomInt(0, 10) : 0);
							captcha.setFont(font);
						}
						check = code = captcha.text();
					} else if (type == -4) {
						if ("random".equals(style)) {
							style = iStyles[RandomUtil.randomInt(0, iStyles.length)];
						}
						iCaptcha = iCaptcha(style, width, height);
						String generator = HandlerUtil.getParam(exchange, "generator");
						if ("math".equals(generator) || ("random".equals(generator) && RandomUtil.randomBoolean())) {
							ReflectUtil.setFieldValue(iCaptcha, generatorField, new MathGenerator());
							code = iCaptcha.getCode();
							check = "" + (int) Calculator.conversion(iCaptcha.getCode());
						} else {
							if (length != 5) {
								Object obj = ReflectUtil.getFieldValue(iCaptcha, generatorField);
								ReflectUtil.setFieldValue(obj, lengthField, length);
							}
							check = code = iCaptcha.getCode();
						}
					} else {
						check = code = ImageUtil.random(length, specials);
					}
				} else {
					check = special.length > 1 ? special[1] : code;
				}
				log.info("captcha code: {}, check: {}, sid: {}", code, check, sid);
				RedisCache.set(ImageUtil.attr, sid, check);
				OutputStream outputStream = exchange.getOutputStream();
				if (captcha != null) {
					captcha.out(outputStream);
				} else if (iCaptcha != null) {
					iCaptcha.write(outputStream);
				} else {
					outputStream.write(ImageUtil.bytes(ImageUtil.create(code)));
				}
				outputStream.close();
			}
		}
	}

	private String[] iStyles = { "circle", "gif", "shear", "line" };

    private ICaptcha iCaptcha(String style, int width, int height) {
        switch (StrUtil.trimToEmpty(style)) {
            case "circle":
                return CaptchaUtil.createCircleCaptcha(width, height);
            case "gif":
                return CaptchaUtil.createGifCaptcha(width, height);
            case "shear":
                return CaptchaUtil.createShearCaptcha(width, height);
            default:
                return CaptchaUtil.createLineCaptcha(width, height);
        }
    }

	private String[] styles = { "gif", "chinese", "arithmetic", "chinesegif", "spec" };

	private Captcha captcha(String style) {
		switch (StrUtil.trimToEmpty(style)) {
			case "gif":
				return new GifCaptcha();
			case "chinese":
				return new ChineseCaptcha();
			case "arithmetic":
				return new ArithmeticCaptcha();
			case "chinesegif":
				return new ChineseGifCaptcha();
			case "random":
				return captcha(styles[RandomUtil.randomInt(0, styles.length)]);
			default:// spec
				return new SpecCaptcha();
		}
	}

	public void code(HttpServerExchange exchange) throws Exception {
		int length = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "length"), 4);
		boolean specials = NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "specials"), false);
		int type = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "type"), -2);
		String style = HandlerUtil.getParam(exchange, "style");
		Captcha captcha = type == -3 ? captcha(HandlerUtil.getParam(exchange, "style")) : null;
		ICaptcha iCaptcha = null;
		ImageCaptchaInfo imageCaptchaInfo = null;
		Triple<String, String, String> triple = null; // check, sid, image
		if (captcha != null) {
			if (!(captcha instanceof ArithmeticCaptchaAbstract)) {
				captcha.setLen(length);
			}
			if (!(captcha instanceof ChineseCaptchaAbstract)) {
				int font = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "font"), 0);
				font = 0 <= font && font <= 9 ? font : (font == -1 ? RandomUtil.randomInt(0, 10) : 0);
				captcha.setFont(font);
			}
			triple = create(captcha);
		} else if (type == -4) {
			if ("random".equals(style)) {
				style = iStyles[RandomUtil.randomInt(0, iStyles.length)];
			}
			iCaptcha = iCaptcha(style, width, height);
			triple = create(iCaptcha, HandlerUtil.getParam(exchange, "generator"), length);
		} else if (type == -5) {
			imageCaptchaInfo = imageCaptchaGenerator.generateCaptchaImage(style(style));
			Map<String, Object> map = imageCaptchaValidator.generateImageCaptchaValidData(imageCaptchaInfo);
			String check = map.get("percentage").toString();
			triple = Triple.of(check, String.valueOf(IdWorker.getId()), imageCaptchaInfo.getBackgroundImage());
			RedisCache.set(ImageUtil.attr, triple.getMiddle(), check);
		}
		Tuple<String, String> codecheck = triple != null
				? new Tuple<>(triple.getLeft(), triple.getLeft())
				: ImageUtil.create(length, specials, type);
		Tuple<String, BufferedImage> tuple = triple != null ? new Tuple<>(triple.getMiddle(), null)
				: ImageUtil.create(codecheck.first, codecheck.second);
		String image = triple != null ? triple.getRight() : ImageUtil.encode(ImageUtil.bytes(tuple.second), null);
		Map<String, String> params = StringUtil.params("sid", tuple.first, "image", image);
		if (NumberUtil.parseBoolean(HandlerUtil.getParam(exchange, "secure"), true) == false) {
			params.put("code", codecheck.first);
			if (!codecheck.first.equals(codecheck.second) && !StringUtil.isBlank(codecheck.second)) {
				params.put("check", codecheck.second);
			}
		}
		if (imageCaptchaInfo != null) {
			params.put("slider", imageCaptchaInfo.getSliderImage());
		}
		HandlerUtil.setResp(exchange, params);
	}

	private String style(String style) {
        switch (StrUtil.trimToEmpty(style)) {
            case "ROTATE":
            case "CONCAT":
            case "WORD_IMAGE_CLICK":
                return style;
            case "RANDOM":
                int random = RandomUtil.randomInt(0, 3);
                return random == 0 ? "SLIDER" : (random == 1 ? "ROTATE" : "CONCAT");
            default:
                return "SLIDER";
        }
    }

	private Triple<String, String, String> create(Captcha captcha) {
		String check = captcha.text();
		String sid = String.valueOf(IdWorker.getId());
		RedisCache.set(ImageUtil.attr, sid, check);
		log.info("sid:{}, check:{}, code:{}", sid, check, check);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		captcha.out(baos);
		String image = ImageUtil.encode(baos.toByteArray(), null);
		return Triple.of(check, sid, image);
	}

	private Triple<String, String, String> create(ICaptcha iCaptcha, String generator, int length) {
		if (length != 5) {
			Object obj = ReflectUtil.getFieldValue(iCaptcha, generatorField);
			ReflectUtil.setFieldValue(obj, lengthField, length);
		}
		String check = null;
		if ("math".equals(generator) || ("random".equals(generator) && RandomUtil.randomBoolean())) {
			ReflectUtil.setFieldValue(iCaptcha, generatorField, new MathGenerator());
			check = "" + (int) Calculator.conversion(iCaptcha.getCode());
		} else {
			check = iCaptcha.getCode();
		}
		String sid = String.valueOf(IdWorker.getId());
		RedisCache.set(ImageUtil.attr, sid, check);
		log.info("sid:{}, check:{}, code:{}", sid, check, check);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		iCaptcha.write(baos);
		String image = ImageUtil.encode(baos.toByteArray(), null);
		return Triple.of(check, sid, image);
	}

	public void check(HttpServerExchange exchange) throws Exception {
		String sid = HandlerUtil.getParam(exchange, "sid");
		String checkcode = HandlerUtil.getParam(exchange, "checkcode");
		if (StringUtil.hasLength(sid) && StringUtil.hasLength(checkcode)) {
			String check = RedisCache.get(ImageUtil.attr, sid);
			boolean valid = checkcode.equalsIgnoreCase(check);
			if (!valid && check != null && checkcode.matches("[0-9\\.,;]+")) {
				String[] arr1 = check.split("[,;]");
				String[] arr2 = checkcode.split("[,;]");
				if (arr1.length == arr2.length) {
					for (int i = 0; i < arr1.length; i++) {
						valid = imageCaptchaValidator.checkPercentage(Convert.toFloat(arr2[i]),
								Convert.toFloat(arr1[i]));
						if (valid == false) {
							break;
						}
					}
				}
			}
			log.info("sid:{}, expect:{}, checkcode:{}, valid:{}", sid, check, checkcode, valid);
			HandlerUtil.setResp(exchange, StringUtil.params("valid", String.valueOf(valid)));
		}
	}

	public void email(HttpServerExchange exchange) throws Exception {
		String toEmail = HandlerUtil.getParam(exchange, "toEmail");
		if (StringUtil.isEmail(toEmail)) {
			String title = StringUtil.firstNotBlank(HandlerUtil.getParam(exchange, "title"), "验证码");
			int minutes = NumberUtil.parseInt(HandlerUtil.getParam(exchange, "minutes"), 10);
			String checkcode = HandlerUtil.getParam(exchange, "checkcode");
			if (StringUtil.isBlank(checkcode)) {
				checkcode = ImageUtil.random(NumberUtil.parseInt(HandlerUtil.getParam(exchange, "length"), 6), false);
			}
			String showapiUserName = HandlerUtil.getShowapiUserName(exchange);
			String template = "您的验证码为{0}，有效期{1}分钟，超时请重新获取。";
			if (!StringUtil.isBlank(showapiUserName)) {
				// Your verification code is {0}, valid for {1} minutes, please get it again
				// after timeout.
				template = StringUtil.firstNotBlank(RedisConfig.get("email." + showapiUserName), template);
			}
			String content = MessageFormat.format(template, checkcode, minutes);
			boolean send = NumberUtil.parseBoolean(RedisConfig.get("email.switch"), true)
					? MailUtil.send(toEmail, title, content)
					: true;
			if (send) {
				String sid = String.valueOf(IdWorker.getId());
				RedisCache.set(ImageUtil.attr, sid, checkcode);
				log.info("email checkcode: {}, sid: {}", checkcode, sid);
				HandlerUtil.setResp(exchange, StringUtil.params("sid", sid, "code", checkcode));
			}
		}
	}

}
