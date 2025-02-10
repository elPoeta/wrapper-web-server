package com.browxy.wrapper.web.db.service.impl;

import com.browxy.wrapper.web.db.repository.GenericRepository;
import com.browxy.wrapper.web.model.User;

public class UserServiceImpl extends GenericServiceImpl<User, Long> {
	public UserServiceImpl(GenericRepository<User, Long> repository) {
		super(repository);
	}
}
