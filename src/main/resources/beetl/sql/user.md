sample
===
* 注释where #use("condition")#有bug

	select #use("cols")# from user where
	1 = 1  
	@if(!isEmpty(userName)){
	 and `user_name`=#userName#
	@}
	@if(!isEmpty(email)){
	 and `email`=#email#
	@}

cols
===

	id,user_name,email

condition
===

	1 = 1  
	@if(!isEmpty(userName)){
	 and `user_name`=#userName#
	@}
	@if(!isEmpty(email)){
	 and `email`=#email#
	@}

