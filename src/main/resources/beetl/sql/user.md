sample
===
* 注释

	select #{use("cols")} from user  where  #{use("condition")}

cols
===
	id,name,age,createDate

updateSample
===
	
	id=#{id},name=#{name},age=#{age},createDate=#{createDate}

condition
===

	1 = 1  
	-- @if(!isEmpty(id)){
	 and id=#{id}
	-- @}
	-- @if(!isEmpty(name)){
	 and name=#{name}
	-- @}
	-- @if(!isEmpty(age)){
	 and age=#{age}
	-- @}
	-- @if(!isEmpty(createDate)){
	 and createDate=#{createDate}
	-- @}
	