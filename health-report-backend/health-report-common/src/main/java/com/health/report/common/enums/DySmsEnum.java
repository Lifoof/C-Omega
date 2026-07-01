package com.health.report.common.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * @Description: 短信枚举类
 *
 */
public enum DySmsEnum {

    /**登录短信模板编码*/
	LOGIN_TEMPLATE_CODE("SMS_468410346","体检健康系统","code"),
    /**忘记密码短信模板编码*/
	FORGET_PASSWORD_TEMPLATE_CODE("SMS_175435174","LEADEDATA","code"),
	/**修改密码短信模板编码*/
	CHANGE_PASSWORD_TEMPLATE_CODE("SMS_465391221","LEADEDATA","code"),
	/**注册账号短信模板编码*/
	REGISTER_TEMPLATE_CODE("SMS_175430166","LEADEDATA","code"),
	/**会议通知*/
	MEET_NOTICE_TEMPLATE_CODE("SMS_201480469","LEADEDATA","username,title,minute,time"),
	/**我的计划通知*/
	PLAN_NOTICE_TEMPLATE_CODE("SMS_201470515","LEADEDATA","username,title,time"),
	/**支付成功短信通知*/
	PAY_SUCCESS_NOTICE_CODE("SMS_461735163","LEADEDATA","realname,money,endTime"),
	/**会员到期通知提醒*/
	VIP_EXPIRE_NOTICE_CODE("SMS_461885023","LEADEDATA","realname,endTime");

	/**
	 * 短信模板编码
	 */
	private String templateCode;
	/**
	 * 签名
	 */
	private String signName;
	/**
	 * 短信模板必需的数据名称，多个key以逗号分隔，此处配置作为校验
	 */
	private String keys;

	private DySmsEnum(String templateCode, String signName, String keys) {
		this.templateCode = templateCode;
		this.signName = signName;
		this.keys = keys;
	}

	public String getTemplateCode() {
		return templateCode;
	}

	public void setTemplateCode(String templateCode) {
		this.templateCode = templateCode;
	}

	public String getSignName() {
		return signName;
	}

	public void setSignName(String signName) {
		this.signName = signName;
	}

	public String getKeys() {
		return keys;
	}

	public void setKeys(String keys) {
		this.keys = keys;
	}

	public static DySmsEnum toEnum(String templateCode) {
		if(StringUtils.isEmpty(templateCode)){
			return null;
		}
		for(DySmsEnum item : DySmsEnum.values()) {
			if(item.getTemplateCode().equals(templateCode)) {
				return item;
			}
		}
		return null;
	}
}

