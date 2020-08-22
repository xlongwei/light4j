package com.xlongwei.light4j.beetl.model;

import java.util.Date;

import lombok.Data;

@Data
public class User {
	private Integer id;
	private Integer age;
	private String name;
	private Date createDate;

}