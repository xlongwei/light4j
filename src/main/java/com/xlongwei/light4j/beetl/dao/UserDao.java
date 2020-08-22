package com.xlongwei.light4j.beetl.dao;

import java.util.List;

import org.beetl.sql.core.mapper.BaseMapper;

import com.xlongwei.light4j.beetl.model.User;

public interface UserDao extends BaseMapper<User> {
	/** sample */
	List<User> sample(User user);
}